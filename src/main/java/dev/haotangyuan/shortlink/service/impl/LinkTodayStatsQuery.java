package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.dao.entity.LinkAccessStatsDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_UIP_PREFIX;
import static dev.haotangyuan.shortlink.common.constant.RedisKeyConstant.STATS_UV_PREFIX;

/**
 * 批量查询短链接的当日 PV、UV 和 UIP。
 */
@Slf4j
@Component
class LinkTodayStatsQuery {

    private static final ZoneId SHANGHAI_ZONE = ZoneId.of("Asia/Shanghai");
    private static final String HLL_PFCOUNT_BATCH_LUA = "lua/hll_pfcount_batch.lua";

    private final StringRedisTemplate stringRedisTemplate;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final Clock clock;
    private final DefaultRedisScript<List> hllBatchScript;

    @Autowired
    LinkTodayStatsQuery(StringRedisTemplate stringRedisTemplate, LinkAccessStatsMapper linkAccessStatsMapper) {
        this(stringRedisTemplate, linkAccessStatsMapper, Clock.system(SHANGHAI_ZONE));
    }

    LinkTodayStatsQuery(StringRedisTemplate stringRedisTemplate,
                        LinkAccessStatsMapper linkAccessStatsMapper,
                        Clock clock) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.linkAccessStatsMapper = linkAccessStatsMapper;
        this.clock = clock;
        this.hllBatchScript = new DefaultRedisScript<>();
        this.hllBatchScript.setScriptSource(
                new ResourceScriptSource(new ClassPathResource(HLL_PFCOUNT_BATCH_LUA)));
        this.hllBatchScript.setResultType(List.class);
    }

    Map<String, TodayStats> findByShortUrls(List<String> fullShortUrls) {
        if (fullShortUrls == null || fullShortUrls.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, Integer> todayPvMap = findTodayPv(fullShortUrls);
        int version = (int) (LocalDate.now(clock).toEpochDay() % 2);
        String joinedUrls = String.join(",", fullShortUrls);
        List<Object> uvResults = executeHllCount(String.format(STATS_UV_PREFIX, version), joinedUrls);
        List<Object> uipResults = executeHllCount(String.format(STATS_UIP_PREFIX, version), joinedUrls);

        Map<String, TodayStats> result = new HashMap<>(fullShortUrls.size());
        for (int index = 0; index < fullShortUrls.size(); index++) {
            String fullShortUrl = fullShortUrls.get(index);
            result.put(fullShortUrl, new TodayStats(
                    todayPvMap.getOrDefault(fullShortUrl, 0),
                    valueAt(uvResults, index),
                    valueAt(uipResults, index)));
        }
        return result;
    }

    private Map<String, Integer> findTodayPv(List<String> fullShortUrls) {
        try {
            Date today = Date.from(LocalDate.now(clock).atStartOfDay(SHANGHAI_ZONE).toInstant());
            return linkAccessStatsMapper.listTodayPvByShortUrls(fullShortUrls, today).stream()
                    .collect(Collectors.toMap(
                            LinkAccessStatsDO::getFullShortUrl,
                            each -> Optional.ofNullable(each.getPv()).orElse(0),
                            Integer::sum));
        } catch (Exception ex) {
            log.warn("Failed to batch get today's PV, returning zero values", ex);
            return Collections.emptyMap();
        }
    }

    private List<Object> executeHllCount(String keyPrefix, String joinedUrls) {
        try {
            List<Object> result = stringRedisTemplate.execute(hllBatchScript, List.of(keyPrefix), joinedUrls);
            return result == null ? Collections.emptyList() : result;
        } catch (Exception ex) {
            log.warn("Failed to batch get today's UV/UIP, returning zero values", ex);
            return Collections.emptyList();
        }
    }

    private int valueAt(List<Object> values, int index) {
        if (index >= values.size()) {
            return 0;
        }
        Object value = values.get(index);
        return value instanceof Number number ? number.intValue() : 0;
    }

    record TodayStats(int pv, int uv, int uip) {
    }
}
