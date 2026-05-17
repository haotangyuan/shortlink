package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.text.StrBuilder;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.config.GotoDomainWhiteListConfiguration;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.common.convention.exception.ServiceException;
import dev.haotangyuan.shortlink.common.enums.ValidDateTypeEnum;
import dev.haotangyuan.shortlink.dao.entity.LinkDO;
import dev.haotangyuan.shortlink.dao.entity.LinkGotoDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkGotoMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkMapper;
import dev.haotangyuan.shortlink.dto.biz.LinkStatsRecordDTO;
import dev.haotangyuan.shortlink.dto.req.LinkBatchCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkCreateReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkUpdateReqDTO;
import dev.haotangyuan.shortlink.dto.resp.*;
import dev.haotangyuan.shortlink.mq.consumer.LinkStatsSaver;
import dev.haotangyuan.shortlink.mq.producer.LinkStatsSaveProducer;
import dev.haotangyuan.shortlink.service.LinkService;
import dev.haotangyuan.shortlink.toolkit.LinkUtil;
import dev.haotangyuan.shortlink.toolkit.ShortCodeUtil;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import com.github.benmanes.caffeine.cache.Cache;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RLock;
import org.redisson.api.RReadWriteLock;
import org.redisson.api.RedissonClient;

import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static dev.haotangyuan.shortlink.common.constant.LinkConstant.UV_COOKIE_MAX_AGE_SECONDS;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.*;
import static dev.haotangyuan.shortlink.common.constant.UserConstant.PUBLIC_GID;
import static dev.haotangyuan.shortlink.common.constant.UserConstant.PUBLIC_USERNAME;

/**
 * 短链接接口实现层
 * @author: haotangyuan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkServiceImpl extends ServiceImpl<LinkMapper, LinkDO> implements LinkService {

    private final RBloomFilter<String> shortUriCreateCachePenetrationBloomFilter;
    private final LinkGotoMapper linkGotoMapper;
    private final StringRedisTemplate stringRedisTemplate;
    private final RedissonClient redissonClient;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final GotoDomainWhiteListConfiguration gotoDomainWhiteListConfiguration;
    private final LinkStatsSaveProducer linkStatsSaveProducer;
    private final LinkStatsSaver linkStatsSaver;
    private final GroupOwnershipVerifier groupOwnershipService;
    private final LinkUtil linkUtil;
    // 本地每键互斥锁缓存（避免跳转路径使用分布式锁）
    private final Cache<String, ReentrantLock> redirectLockCache;
    // 短链接跳转目标 URL 本地缓存（减少 Redis 网络往返）
    private final Cache<String, String> redirectCache;

    private DefaultRedisScript<List> hllBatchScript;
    private static final String HLL_PFCOUNT_BATCH_LUA = "lua/hll_pfcount_batch.lua";

    @Value("${short-link.domain.default}")
    private String createLinkDefaultDomain;

    @PostConstruct
    public void init() {
        hllBatchScript = new DefaultRedisScript<>();
        hllBatchScript.setScriptSource(new ResourceScriptSource(new ClassPathResource(HLL_PFCOUNT_BATCH_LUA)));
        hllBatchScript.setResultType(List.class);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LinkCreateRespDTO createLink(LinkCreateReqDTO linkCreateReqDTO) {
        // 未登录（public）创建：强制使用公共分组
        String currentUsername = UserContext.getUsername();
        if (java.util.Objects.equals(currentUsername, PUBLIC_USERNAME)) {
            linkCreateReqDTO.setGid(PUBLIC_GID);
        } else {
            // 鉴权：校验分组归属
            groupOwnershipService.assertOwnedByCurrentUser(linkCreateReqDTO.getGid());
        }
        verificationWhitelist(linkCreateReqDTO.getOriginUrl());
        
        // 设置默认值
        if (linkCreateReqDTO.getCreatedType() == null) {
            linkCreateReqDTO.setCreatedType(0);
        }
        if (linkCreateReqDTO.getValidDateType() == null) {
            linkCreateReqDTO.setValidDateType(ValidDateTypeEnum.CUSTOM.getType());
        }
        
        // 处理有效期逻辑，限制最大3天
        Date now = new Date();
        Date maxValidDate = DateUtil.offsetDay(now, 3);
        
        if (linkCreateReqDTO.getValidDate() == null) {
            // 如果没有传入validDate，默认设置为1天后
            linkCreateReqDTO.setValidDate(DateUtil.offsetDay(now, 1));
        } else if (linkCreateReqDTO.getValidDate().after(maxValidDate)) {
            // 如果传入的validDate超过3天，修正为3天
            linkCreateReqDTO.setValidDate(maxValidDate);
        }
        
        // 确保不是永久有效
        if (linkCreateReqDTO.getValidDateType() == ValidDateTypeEnum.PERMANENT.getType()) {
            linkCreateReqDTO.setValidDateType(ValidDateTypeEnum.CUSTOM.getType());
        }
        
        String shortCode = ShortCodeUtil.next();
        String fullShortUrl = StrBuilder.create(createLinkDefaultDomain)
                .append("/")
                .append(shortCode)
                .toString();
        LinkDO shortLinkDO = LinkDO.builder()
                .domain(createLinkDefaultDomain)
                .originUrl(linkCreateReqDTO.getOriginUrl())
                .gid(linkCreateReqDTO.getGid())
                .createdType(linkCreateReqDTO.getCreatedType())
                .validDateType(linkCreateReqDTO.getValidDateType())
                .validDate(linkCreateReqDTO.getValidDate())
                .describe(linkCreateReqDTO.getDescribe())
                .shortUri(shortCode)
                .enableStatus(0)
                .totalPv(0)
                .totalUv(0)
                .totalUip(0)
                .delTime(0L)
                .fullShortUrl(fullShortUrl)
                .favicon(linkUtil.getFavicon(linkCreateReqDTO.getOriginUrl()))
                .build();
        LinkGotoDO linkGotoDO = LinkGotoDO.builder()
                .fullShortUrl(fullShortUrl)
                .gid(linkCreateReqDTO.getGid())
                .build();
        try {
            baseMapper.insert(shortLinkDO);
            linkGotoMapper.insert(linkGotoDO);
        } catch (DuplicateKeyException ex) {
            // 首先判断是否存在布隆过滤器，如果不存在直接新增
            if (!shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl)) {
                shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
            }
            throw new ServiceException(String.format("短链接：%s 生成重复", fullShortUrl));
        }
        // 缓存预热
        stringRedisTemplate.opsForValue().set(
                String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                linkCreateReqDTO.getOriginUrl(),
                LinkUtil.getLinkCacheValidTime(linkCreateReqDTO.getValidDate()), TimeUnit.MILLISECONDS
        );
        try {
            stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        } catch (Throwable t) {
            log.warn("Clear negative cache on create error, fullShortUrl={}", fullShortUrl, t);
        }
        shortUriCreateCachePenetrationBloomFilter.add(fullShortUrl);
        return LinkCreateRespDTO.builder()
                .fullShortUrl("http://" + shortLinkDO.getFullShortUrl())
                .originUrl(linkCreateReqDTO.getOriginUrl())
                .gid(linkCreateReqDTO.getGid())
                .build();
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public void updateLink(LinkUpdateReqDTO linkUpdateReqDTO) {
        // 鉴权：旧、新分组均需属于当前用户
        groupOwnershipService.assertOwnedByCurrentUser(linkUpdateReqDTO.getOriginGid());
        groupOwnershipService.assertOwnedByCurrentUser(linkUpdateReqDTO.getGid());
        verificationWhitelist(linkUpdateReqDTO.getOriginUrl());
        LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                .eq(LinkDO::getGid, linkUpdateReqDTO.getOriginGid())
                .eq(LinkDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                .eq(LinkDO::getDelFlag, 0)
                .eq(LinkDO::getEnableStatus, 0);
        LinkDO hasLinkDO = baseMapper.selectOne(queryWrapper);
        if (hasLinkDO == null) {
            throw new ClientException("短链接记录不存在");
        }
        if (Objects.equals(hasLinkDO.getGid(), linkUpdateReqDTO.getGid())) {
            LambdaUpdateWrapper<LinkDO> updateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                    .eq(LinkDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                    .eq(LinkDO::getGid, linkUpdateReqDTO.getGid())
                    .eq(LinkDO::getDelFlag, 0)
                    .eq(LinkDO::getEnableStatus, 0)
                    .set(Objects.equals(linkUpdateReqDTO.getValidDateType(), ValidDateTypeEnum.PERMANENT.getType()), LinkDO::getValidDate, null);
            LinkDO linkDO = LinkDO.builder()
                    .domain(hasLinkDO.getDomain())
                    .shortUri(hasLinkDO.getShortUri())
                    .favicon(Objects.equals(linkUpdateReqDTO.getOriginUrl(), hasLinkDO.getOriginUrl()) ? hasLinkDO.getFavicon() : linkUtil.getFavicon(linkUpdateReqDTO.getOriginUrl()))
                    .createdType(hasLinkDO.getCreatedType())
                    .gid(linkUpdateReqDTO.getGid())
                    .originUrl(linkUpdateReqDTO.getOriginUrl())
                    .describe(linkUpdateReqDTO.getDescribe())
                    .validDateType(linkUpdateReqDTO.getValidDateType())
                    .validDate(linkUpdateReqDTO.getValidDate())
                    .build();
            baseMapper.update(linkDO, updateWrapper);
        } else {
            RReadWriteLock readWriteLock = redissonClient.getReadWriteLock(String.format(LOCK_GID_UPDATE_KEY, linkUpdateReqDTO.getFullShortUrl()));
            RLock rLock = readWriteLock.writeLock();
            rLock.lock();
            try {
                LambdaUpdateWrapper<LinkDO> linkUpdateWrapper = Wrappers.lambdaUpdate(LinkDO.class)
                        .eq(LinkDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkDO::getGid, hasLinkDO.getGid())
                        .eq(LinkDO::getDelFlag, 0)
                        .eq(LinkDO::getDelTime, 0L)
                        .eq(LinkDO::getEnableStatus, 0);
                LinkDO delLinkDO = LinkDO.builder()
                        .delTime(System.currentTimeMillis())
                        .build();
                delLinkDO.setDelFlag(1);
                baseMapper.update(delLinkDO, linkUpdateWrapper);
                LinkDO linkDO = LinkDO.builder()
                        .domain(createLinkDefaultDomain)
                        .originUrl(linkUpdateReqDTO.getOriginUrl())
                        .gid(linkUpdateReqDTO.getGid())
                        .createdType(hasLinkDO.getCreatedType())
                        .validDateType(linkUpdateReqDTO.getValidDateType())
                        .validDate(linkUpdateReqDTO.getValidDate())
                        .describe(linkUpdateReqDTO.getDescribe())
                        .shortUri(hasLinkDO.getShortUri())
                        .enableStatus(hasLinkDO.getEnableStatus())
                        .totalPv(hasLinkDO.getTotalPv())
                        .totalUv(hasLinkDO.getTotalUv())
                        .totalUip(hasLinkDO.getTotalUip())
                        .fullShortUrl(hasLinkDO.getFullShortUrl())
                        .favicon(Objects.equals(linkUpdateReqDTO.getOriginUrl(), hasLinkDO.getOriginUrl()) ? hasLinkDO.getFavicon() : linkUtil.getFavicon(linkUpdateReqDTO.getOriginUrl()))
                        .delTime(0L)
                        .build();
                baseMapper.insert(linkDO);
                LambdaQueryWrapper<LinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(LinkGotoDO.class)
                        .eq(LinkGotoDO::getFullShortUrl, linkUpdateReqDTO.getFullShortUrl())
                        .eq(LinkGotoDO::getGid, hasLinkDO.getGid());
                LinkGotoDO linkGotoDO = linkGotoMapper.selectOne(linkGotoQueryWrapper);
                linkGotoMapper.delete(linkGotoQueryWrapper);
                linkGotoDO.setGid(linkUpdateReqDTO.getGid());
                linkGotoMapper.insert(linkGotoDO);

                // 失效 gid 缓存
                linkStatsSaver.invalidateGidCache(linkUpdateReqDTO.getFullShortUrl());
            } finally {
                rLock.unlock();
            }
        }
        if (!Objects.equals(hasLinkDO.getValidDateType(), linkUpdateReqDTO.getValidDateType())
                || !Objects.equals(hasLinkDO.getValidDate(), linkUpdateReqDTO.getValidDate())
                || !Objects.equals(hasLinkDO.getOriginUrl(), linkUpdateReqDTO.getOriginUrl())) {
            // 删除 Redis 缓存
            stringRedisTemplate.delete(String.format(GOTO_SHORT_LINK_KEY, linkUpdateReqDTO.getFullShortUrl()));
            // 删除本地 Caffeine 缓存
            redirectCache.invalidate(linkUpdateReqDTO.getFullShortUrl());
            Date currentDate = new Date();
            if (hasLinkDO.getValidDate() != null && hasLinkDO.getValidDate().before(currentDate)) {
                if (Objects.equals(linkUpdateReqDTO.getValidDateType(), ValidDateTypeEnum.PERMANENT.getType()) || linkUpdateReqDTO.getValidDate().after(currentDate)) {
                    stringRedisTemplate.delete(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, linkUpdateReqDTO.getFullShortUrl()));
                }
            }
        }
    }

    @Override
    public IPage<LinkPageRespDTO> pageLink(LinkPageReqDTO linkPageReqDTO) {
        // 鉴权：校验分组归属
        groupOwnershipService.assertOwnedByCurrentUser(linkPageReqDTO.getGid());
        IPage<LinkDO> resultPage = baseMapper.pageLink(linkPageReqDTO);
        
        // 获取所有短链接列表以批量计算今日 UV/UIP
        List<String> fullShortUrls = resultPage.getRecords().stream()
                .map(LinkDO::getFullShortUrl)
                .toList();
        
        // 使用批量 Lua 脚本获取今日 UV/UIP
        Map<String, int[]> todayStatsMap = batchGetTodayStats(fullShortUrls);
        
        return resultPage.convert(each -> {
            LinkPageRespDTO bean = BeanUtil.toBean(each, LinkPageRespDTO.class);
            bean.setDomain("http://" + bean.getDomain());
            
            // 设置今日统计
            int[] todayStats = todayStatsMap.get(each.getFullShortUrl());
            if (todayStats != null) {
                bean.setTodayPv(todayStats[0]);
                bean.setTodayUv(todayStats[1]);
                bean.setTodayUip(todayStats[2]);
            } else {
                bean.setTodayPv(0);
                bean.setTodayUv(0);
                bean.setTodayUip(0);
            }
            
            return bean;
        });
    }

    /**
     * 批量获取今日统计数据
     */
    private Map<String, int[]> batchGetTodayStats(List<String> fullShortUrls) {
        Map<String, int[]> resultMap = new HashMap<>();
        
        if (fullShortUrls == null || fullShortUrls.isEmpty()) {
            return resultMap;
        }
        
        // 计算 v = epochDay(Asia/Shanghai) % 2
        int v = (int)(LocalDate.now(ZoneId.of("Asia/Shanghai")).toEpochDay() % 2);
        
        String uvPrefix = String.format(STATS_UV_PREFIX, v);
        String uipPrefix = String.format(STATS_UIP_PREFIX, v);
        String fsuList = String.join(",", fullShortUrls);
        
        try {
            // 批量获取 UV 数据
            List<Object> uvResults = stringRedisTemplate.execute(hllBatchScript, 
                Arrays.asList(uvPrefix), fsuList);
            
            // 批量获取 UIP 数据
            List<Object> uipResults = stringRedisTemplate.execute(hllBatchScript, 
                Arrays.asList(uipPrefix), fsuList);
            
            // 合并结果
            for (int i = 0; i < fullShortUrls.size(); i++) {
                String fullShortUrl = fullShortUrls.get(i);
                
                int todayUv = 0;
                int todayUip = 0;
                
                if (uvResults != null && i < uvResults.size()) {
                    Object uvObj = uvResults.get(i);
                    todayUv = uvObj instanceof Number ? ((Number) uvObj).intValue() : 0;
                }
                
                if (uipResults != null && i < uipResults.size()) {
                    Object uipObj = uipResults.get(i);
                    todayUip = uipObj instanceof Number ? ((Number) uipObj).intValue() : 0;
                }
                
                // 获取今日 PV
                int todayPv = getTodayPvFromStats(fullShortUrl);
                
                resultMap.put(fullShortUrl, new int[]{todayPv, todayUv, todayUip});
            }
        } catch (Exception e) {
            log.warn("Failed to batch get today stats, returning empty map", e);
        }
        
        return resultMap;
    }

    /**
     * 从 stats 表获取今日 PV
     */
    private int getTodayPvFromStats(String fullShortUrl) {
        try {
            // 使用 Asia/Shanghai 时区获取今日日期
            ZoneId shanghaiZone = ZoneId.of("Asia/Shanghai");
            LocalDate today = LocalDate.now(shanghaiZone);
            Date todayDate = Date.from(today.atStartOfDay(shanghaiZone).toInstant());
            
            // 从 stats 表查询今日 PV 总数
            return linkAccessStatsMapper.sumTodayPvByShortUrl(fullShortUrl, todayDate);
        } catch (Exception e) {
            log.warn("Failed to get today PV from stats for {}, returning 0", fullShortUrl, e);
            return 0;
        }
    }

    @Override
    public List<GroupLinkCountQueryRespDTO> listGroupLinkCount(List<String> gidList) {
        // 鉴权：校验分组列表归属
        groupOwnershipService.assertAllOwnedByCurrentUser(gidList);
        if (CollUtil.isEmpty(gidList)) {
            return Collections.emptyList();
        }
        return baseMapper.listGroupLinkCount(gidList);
    }

    @SneakyThrows
    @Override
    public void restoreUrl(String shortUri, ServletRequest request, ServletResponse response) {
        String serverName = request.getServerName();
        String serverPort = Optional.of(request.getServerPort())
                .filter(each -> !Objects.equals(each, 80))
                .map(String::valueOf)
                .map(each -> ":" + each)
                .orElse("");
        String fullShortUrl = StrBuilder.create(serverName)
                .append(serverPort)
                .append("/")
                .append(shortUri)
                .toString();
        // 1. 优先查询本地 Caffeine 缓存
        String originalLink = redirectCache.getIfPresent(fullShortUrl);
        if (StrUtil.isNotBlank(originalLink)) {
            linkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        // 2. 查询 Redis 缓存
        originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(originalLink)) {
            // 回写本地缓存
            redirectCache.put(fullShortUrl, originalLink);
            linkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            ((HttpServletResponse) response).sendRedirect(originalLink);
            return;
        }
        boolean contains = ShortCodeUtil.mightExist(shortUri);
        if (!contains) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        contains = shortUriCreateCachePenetrationBloomFilter.contains(fullShortUrl);
        if (!contains) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        String gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
        if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
            ((HttpServletResponse) response).sendRedirect("/page/notfound");
            return;
        }
        // 本地每键互斥：使用 Caffeine 管理 ReentrantLock，避免跳转路径使用分布式锁导致尾延迟放大
        ReentrantLock lock = redirectLockCache.get(fullShortUrl, k -> new ReentrantLock());
        lock.lock();
        try {
            // 双重检查：先查本地缓存
            originalLink = redirectCache.getIfPresent(fullShortUrl);
            if (StrUtil.isNotBlank(originalLink)) {
                linkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }
            // 双重检查：再查 Redis
            originalLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(originalLink)) {
                // 回写本地缓存
                redirectCache.put(fullShortUrl, originalLink);
                linkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
                ((HttpServletResponse) response).sendRedirect(originalLink);
                return;
            }
            gotoIsNullShortLink = stringRedisTemplate.opsForValue().get(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl));
            if (StrUtil.isNotBlank(gotoIsNullShortLink)) {
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<LinkGotoDO> linkGotoQueryWrapper = Wrappers.lambdaQuery(LinkGotoDO.class)
                    .eq(LinkGotoDO::getFullShortUrl, fullShortUrl);
            LinkGotoDO linkGotoDO = linkGotoMapper.selectOne(linkGotoQueryWrapper);
            if (linkGotoDO == null) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            LambdaQueryWrapper<LinkDO> queryWrapper = Wrappers.lambdaQuery(LinkDO.class)
                    .eq(LinkDO::getGid, linkGotoDO.getGid())
                    .eq(LinkDO::getFullShortUrl, fullShortUrl)
                    .eq(LinkDO::getDelFlag, 0)
                    .eq(LinkDO::getEnableStatus, 0);
            LinkDO linkDO = baseMapper.selectOne(queryWrapper);
            if (linkDO == null || (linkDO.getValidDate() != null && linkDO.getValidDate().before(new Date()))) {
                stringRedisTemplate.opsForValue().set(String.format(GOTO_IS_NULL_SHORT_LINK_KEY, fullShortUrl), "-", 30, TimeUnit.MINUTES);
                ((HttpServletResponse) response).sendRedirect("/page/notfound");
                return;
            }
            stringRedisTemplate.opsForValue().set(
                    String.format(GOTO_SHORT_LINK_KEY, fullShortUrl),
                    linkDO.getOriginUrl(),
                    LinkUtil.getLinkCacheValidTime(linkDO.getValidDate()), TimeUnit.MILLISECONDS
            );
            // 同时写入本地缓存
            redirectCache.put(fullShortUrl, linkDO.getOriginUrl());
            linkStats(buildLinkStatsRecordAndSetUser(fullShortUrl, request, response));
            ((HttpServletResponse) response).sendRedirect(linkDO.getOriginUrl());
        } finally {
            lock.unlock();
        }
    }

    @Override
    public LinkBatchCreateRespDTO batchCreateLink(LinkBatchCreateReqDTO linkBatchCreateReqDTO) {
        List<String> originUrls = linkBatchCreateReqDTO.getOriginUrls();
        List<String> describes = linkBatchCreateReqDTO.getDescribes();
        List<LinkBaseInfoRespDTO> result = new ArrayList<>();
        for (int i = 0; i < originUrls.size(); i++) {
            LinkCreateReqDTO shortLinkCreateReqDTO = BeanUtil.toBean(linkBatchCreateReqDTO, LinkCreateReqDTO.class);
            shortLinkCreateReqDTO.setOriginUrl(originUrls.get(i));
            shortLinkCreateReqDTO.setDescribe(describes.get(i));
            try {
                LinkCreateRespDTO shortLink = createLink(shortLinkCreateReqDTO);
                LinkBaseInfoRespDTO linkBaseInfoRespDTO = LinkBaseInfoRespDTO.builder()
                        .fullShortUrl(shortLink.getFullShortUrl())
                        .originUrl(shortLink.getOriginUrl())
                        .describe(describes.get(i))
                        .build();
                result.add(linkBaseInfoRespDTO);
            } catch (Throwable ex) {
                log.error("批量创建短链接失败，原始参数：{}", originUrls.get(i));
            }
        }
        return LinkBatchCreateRespDTO.builder()
                .total(result.size())
                .baseLinkInfos(result)
                .build();
    }

    private LinkStatsRecordDTO buildLinkStatsRecordAndSetUser(String fullShortUrl, ServletRequest request, ServletResponse response) {
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        AtomicReference<String> uv = new AtomicReference<>();
        // UV Cookie 最大保留 3 个月（从常量类提取）
        Runnable addResponseCookieTask = () -> {
            uv.set(UUID.fastUUID().toString());
            Cookie uvCookie = new Cookie("uv", uv.get());
            uvCookie.setMaxAge(UV_COOKIE_MAX_AGE_SECONDS);
            uvCookie.setPath(StrUtil.sub(fullShortUrl, fullShortUrl.indexOf("/"), fullShortUrl.length()));
            ((HttpServletResponse) response).addCookie(uvCookie);
        };
        if (ArrayUtil.isNotEmpty(cookies)) {
            Arrays.stream(cookies)
                    .filter(each -> Objects.equals(each.getName(), "uv"))
                    .findFirst()
                    .map(Cookie::getValue)
                    .ifPresentOrElse(uv::set, addResponseCookieTask);
        } else {
            addResponseCookieTask.run();
        }
        String uip = LinkUtil.getActualIp(((HttpServletRequest) request));
        String os = LinkUtil.getOs(((HttpServletRequest) request));
        String browser = LinkUtil.getBrowser(((HttpServletRequest) request));
        String device = LinkUtil.getDevice(((HttpServletRequest) request));
        return LinkStatsRecordDTO.builder()
                .fullShortUrl(fullShortUrl)
                .uv(uv.get())
                .uip(uip)
                .os(os)
                .browser(browser)
                .device(device)
                .currentDate(new Date())
                .build();
    }

    @Override
    public void linkStats(LinkStatsRecordDTO linkStatsRecordDTO) {
        Map<String, String> producerMap = new HashMap<>();
        producerMap.put("statsRecord", JSON.toJSONString(linkStatsRecordDTO));
        linkStatsSaveProducer.send(producerMap);
    }

    private void verificationWhitelist(String originUrl) {
        Boolean enable = gotoDomainWhiteListConfiguration.getEnable();
        if (enable == null || !enable) {
            return;
        }
        String domain = LinkUtil.extractDomain(originUrl);
        if (StrUtil.isBlank(domain)) {
            throw new ClientException("跳转链接填写错误");
        }
        List<String> details = gotoDomainWhiteListConfiguration.getDetails();
        if (!details.contains(domain)) {
            throw new ClientException("演示环境为避免恶意攻击，请生成以下网站跳转链接：" + gotoDomainWhiteListConfiguration.getNames());
        }
    }
}
