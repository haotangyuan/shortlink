package dev.haotangyuan.shortlink.mq.consumer;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import dev.haotangyuan.shortlink.dao.entity.*;
import dev.haotangyuan.shortlink.dao.mapper.*;
import dev.haotangyuan.shortlink.dto.biz.LinkStatsRecordDTO;
import dev.haotangyuan.shortlink.toolkit.ipgeo.GeoInfo;
import dev.haotangyuan.shortlink.toolkit.ipgeo.IpGeoClient;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.LOCK_GID_UPDATE_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_HLL_DELTA_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_UIP_ACTIVE_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_UIP_HLL_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_UV_ACTIVE_KEY;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_UV_HLL_KEY;

/**
 * 短链接统计持久化（同步串行，DB 事务内）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LinkStatsSaver {

    private final LinkMapper linkMapper;
    private final LinkGotoMapper linkGotoMapper;
    private final RedissonClient redissonClient;
    private final IpGeoClient ipGeoClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;
    private final LinkFirstVisitMapper linkFirstVisitMapper;
    private final StringRedisTemplate stringRedisTemplate;

    private DefaultRedisScript<Long> hllCountAddDeltaScript;

    // 本地 gid 缓存（1万条，10分钟过期），gid 变更是低频操作，缓存不一致通过乐观重试机制修正
    private final Cache<String, String> gidCache = Caffeine.newBuilder()
            .maximumSize(10_000)
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build();

    private static final String HLL_COUNT_ADD_DELTA_LUA = "lua/hll_count_add_delta.lua";
    private static final int HLL_TTL_SECONDS = 86_400;
    private static final int HLL_DELTA_TTL_SECONDS = 7 * 86_400;

    @PostConstruct
    public void init() {
        hllCountAddDeltaScript = new DefaultRedisScript<>();
        hllCountAddDeltaScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(HLL_COUNT_ADD_DELTA_LUA)));
        hllCountAddDeltaScript.setResultType(Long.class);
        log.info("LinkStatsSaver initialized");
    }

    // 失效本地 gid 缓存（当 gid 发生变更时由 LinkServiceImpl 调用）
    public void invalidateGidCache(String fullShortUrl) {
        gidCache.invalidate(fullShortUrl);
        log.debug("Invalidated gid cache for: {}", fullShortUrl);
    }

    // 同步保存统计数据（事务），messageId 唯一索引冲突抛异常回滚（DB 层幂等）
    @Transactional(rollbackFor = Exception.class)
    public void save(LinkStatsRecordDTO statsRecord, String messageId) {
        String fullShortUrl = statsRecord.getFullShortUrl();

        // 计算时间相关字段
        Date eventTime = statsRecord.getCurrentDate();
        if (eventTime == null) {
            eventTime = new Date();
        }
        ZoneId zoneId = ZoneId.of("Asia/Shanghai");
        ZonedDateTime zonedDateTime = ZonedDateTime.ofInstant(eventTime.toInstant(), zoneId);
        int hour = zonedDateTime.getHour();
        int weekValue = zonedDateTime.getDayOfWeek().getValue();
        LocalDate localDate = zonedDateTime.toLocalDate();
        Date statsDate = Date.from(localDate.atStartOfDay(zoneId).toInstant());

        // 计算 v = epochDay(Asia/Shanghai) % 2（基于事件时间）
        int v = (int) (localDate.toEpochDay() % 2);

        // 使用新的 {v} 风格键
        String uvKey = String.format(STATS_UV_HLL_KEY, v, fullShortUrl);
        String uipKey = String.format(STATS_UIP_HLL_KEY, v, fullShortUrl);
        String uvActiveKey = String.format(STATS_UV_ACTIVE_KEY, v);
        String uipActiveKey = String.format(STATS_UIP_ACTIVE_KEY, v);
        String uvDeltaKey = String.format(STATS_HLL_DELTA_KEY, messageId, "uv");
        String uipDeltaKey = String.format(STATS_HLL_DELTA_KEY, messageId, "uip");

        // 计算 UV delta
        Long uvDelta = stringRedisTemplate.execute(hllCountAddDeltaScript,
                Arrays.asList(uvKey, uvActiveKey, uvDeltaKey),
                statsRecord.getUv(),
                fullShortUrl,
                String.valueOf(HLL_TTL_SECONDS),
                String.valueOf(HLL_DELTA_TTL_SECONDS));

        // 计算 UIP delta
        Long uipDelta = stringRedisTemplate.execute(hllCountAddDeltaScript,
                Arrays.asList(uipKey, uipActiveKey, uipDeltaKey),
                statsRecord.getUip(),
                fullShortUrl,
                String.valueOf(HLL_TTL_SECONDS),
                String.valueOf(HLL_DELTA_TTL_SECONDS));

        // 查询 IP 地理位置
        GeoInfo geoInfo = ipGeoClient.query(statsRecord.getUip());
        if (geoInfo == null) {
            geoInfo = GeoInfo.unknown();
        }

        // 1. 首访判定（判重表）
        boolean isFirstVisit = false;
        String uv = statsRecord.getUv();
        if (StrUtil.isNotBlank(uv)) {
            int affected = linkFirstVisitMapper.insertIgnore(LinkFirstVisitDO.builder()
                    .fullShortUrl(fullShortUrl)
                    .user(uv)
                    .build());
            isFirstVisit = (affected > 0);
        }

        // 2. 访问日志（messageId 唯一键兜底重复消费）
        String locale = null;
        if (geoInfo != null) {
            locale = Stream.of(geoInfo.getCountry(), geoInfo.getProvince(), geoInfo.getCity())
                    .filter(StrUtil::isNotBlank)
                    .collect(Collectors.joining("-"));
            if (StrUtil.isBlank(locale)) {
                locale = null;
            }
        }
        linkAccessLogsMapper.insert(LinkAccessLogsDO.builder()
                .fullShortUrl(fullShortUrl)
                .user(statsRecord.getUv())
                .ip(statsRecord.getUip())
                .browser(statsRecord.getBrowser())
                .os(statsRecord.getOs())
                .network(geoInfo != null ? geoInfo.getIsp() : null)
                .device(statsRecord.getDevice())
                .locale(locale)
                .firstFlag(isFirstVisit)
                .messageId(messageId)
                .build());

        int uvDeltaInt = uvDelta != null ? uvDelta.intValue() : 0;
        int uipDeltaInt = uipDelta != null ? uipDelta.intValue() : 0;

        // 3. 地区统计
        linkLocaleStatsMapper.shortLinkLocaleStats(LinkLocaleStatsDO.builder()
                .fullShortUrl(fullShortUrl)
                .date(statsDate)
                .cnt(1)
                .province(geoInfo.getProvince())
                .city(geoInfo.getCity())
                .adcode(geoInfo.getAdcode())
                .country(geoInfo.getCountry())
                .build());

        // 4. 操作系统统计
        linkOsStatsMapper.shortLinkOsStats(LinkOsStatsDO.builder()
                .os(statsRecord.getOs())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(statsDate)
                .build());

        // 5. 浏览器统计
        linkBrowserStatsMapper.shortLinkBrowserStats(LinkBrowserStatsDO.builder()
                .browser(statsRecord.getBrowser())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(statsDate)
                .build());

        // 6. 设备统计
        linkDeviceStatsMapper.shortLinkDeviceStats(LinkDeviceStatsDO.builder()
                .device(statsRecord.getDevice())
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(statsDate)
                .build());

        // 7. 网络统计
        linkNetworkStatsMapper.shortLinkNetworkStats(LinkNetworkStatsDO.builder()
                .network(geoInfo != null ? geoInfo.getIsp() : null)
                .cnt(1)
                .fullShortUrl(fullShortUrl)
                .date(statsDate)
                .build());

        // 8. 访问统计（PV/UV/UIP）
        linkAccessStatsMapper.shortLinkAccessStats(LinkAccessStatsDO.builder()
                .pv(1)
                .uv(uvDeltaInt)
                .uip(uipDeltaInt)
                .hour(hour)
                .weekday(weekValue)
                .fullShortUrl(fullShortUrl)
                .date(statsDate)
                .build());

        // 9. 更新 link 表统计
        updateLinkAgg(fullShortUrl, uvDeltaInt, uipDeltaInt);
        cleanupDeltaKeysAfterCommit(Arrays.asList(uvDeltaKey, uipDeltaKey));
    }

    private void cleanupDeltaKeysAfterCommit(java.util.List<String> deltaKeys) {
        Runnable cleanup = () -> {
            try {
                stringRedisTemplate.delete(deltaKeys);
            } catch (Exception ex) {
                log.warn("Failed to clean HLL delta keys after commit: {}", ex.getMessage());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    cleanup.run();
                }
            });
        } else {
            cleanup.run();
        }
    }

    // 乐观更新 + 回源重试
    private void updateLinkAgg(String fullShortUrl, int uvDelta, int uipDelta) {
        String cachedGid = gidCache.getIfPresent(fullShortUrl);
        if (cachedGid != null) {
            int affected = linkMapper.incrementStats(cachedGid, fullShortUrl, 1, uvDelta, uipDelta);
            if (affected > 0) {
                return;
            }
            gidCache.invalidate(fullShortUrl);
        }
        incrementStatsWithReadLock(fullShortUrl, uvDelta, uipDelta);
    }

    // 读锁保护：查询 gid + incrementStats
    private void incrementStatsWithReadLock(String fullShortUrl, int uvDelta, int uipDelta) {
        RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, fullShortUrl));
        RLock rLock = readWriteLock.readLock();
        rLock.lock();
        try {
            LinkGotoDO linkGotoDO = linkGotoMapper.selectOne(
                    Wrappers.lambdaQuery(LinkGotoDO.class).eq(LinkGotoDO::getFullShortUrl, fullShortUrl));
            if (linkGotoDO == null) {
                log.warn("Link not found: {}", fullShortUrl);
                return;
            }
            String gid = linkGotoDO.getGid();
            gidCache.put(fullShortUrl, gid);
            int affected = linkMapper.incrementStats(gid, fullShortUrl, 1, uvDelta, uipDelta);
            if (affected == 0) {
                log.warn("incrementStats affected 0, link deleted or gid changed: {}", fullShortUrl);
            }
        } finally {
            rLock.unlock();
        }
    }
}
