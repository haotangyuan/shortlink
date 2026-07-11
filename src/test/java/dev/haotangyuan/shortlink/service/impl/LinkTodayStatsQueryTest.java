package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.dao.entity.LinkAccessStatsDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LinkTodayStatsQueryTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(
            Instant.parse("2026-07-11T04:00:00Z"), ZoneId.of("Asia/Shanghai"));

    @Test
    void returnsEmptyResultWithoutCallingDependencies() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        LinkAccessStatsMapper mapper = mock(LinkAccessStatsMapper.class);
        LinkTodayStatsQuery query = new LinkTodayStatsQuery(redisTemplate, mapper, FIXED_CLOCK);

        assertEquals(Collections.emptyMap(), query.findByShortUrls(Collections.emptyList()));

        verify(redisTemplate, never()).execute(any(), anyList(), any());
        verify(mapper, never()).listTodayPvByShortUrls(anyList(), any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void combinesDatabasePvAndRedisUvUipByListPosition() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        LinkAccessStatsMapper mapper = mock(LinkAccessStatsMapper.class);
        when(mapper.listTodayPvByShortUrls(anyList(), any())).thenReturn(List.of(
                LinkAccessStatsDO.builder().fullShortUrl("s/one").pv(7).build()));
        when(redisTemplate.execute(any(), anyList(), eq("s/one,s/two")))
                .thenReturn(List.of(11L, 12L), List.of(21L, 22L));
        LinkTodayStatsQuery query = new LinkTodayStatsQuery(redisTemplate, mapper, FIXED_CLOCK);

        Map<String, LinkTodayStatsQuery.TodayStats> result = query.findByShortUrls(List.of("s/one", "s/two"));

        assertEquals(new LinkTodayStatsQuery.TodayStats(7, 11, 21), result.get("s/one"));
        assertEquals(new LinkTodayStatsQuery.TodayStats(0, 12, 22), result.get("s/two"));
    }

    @Test
    void fallsBackToZeroWhenDataStoresAreUnavailable() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        LinkAccessStatsMapper mapper = mock(LinkAccessStatsMapper.class);
        when(mapper.listTodayPvByShortUrls(anyList(), any())).thenThrow(new IllegalStateException("db unavailable"));
        when(redisTemplate.execute(any(), anyList(), any())).thenThrow(new IllegalStateException("redis unavailable"));
        LinkTodayStatsQuery query = new LinkTodayStatsQuery(redisTemplate, mapper, FIXED_CLOCK);

        Map<String, LinkTodayStatsQuery.TodayStats> result = query.findByShortUrls(List.of("s/one"));

        assertEquals(new LinkTodayStatsQuery.TodayStats(0, 0, 0), result.get("s/one"));
    }
}
