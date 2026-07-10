package dev.haotangyuan.shortlink.service.impl;

import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.dao.entity.LinkAccessStatsDO;
import dev.haotangyuan.shortlink.dao.mapper.GroupMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessLogsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkBrowserStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkDeviceStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkLocaleStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkNetworkStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkOsStatsMapper;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LinkStatsServiceImplTest {

    @Test
    void includesTheWholeEndDateInAggregateQueries() {
        LinkAccessStatsMapper accessStatsMapper = mock(LinkAccessStatsMapper.class);
        LinkLocaleStatsMapper localeStatsMapper = mock(LinkLocaleStatsMapper.class);
        LinkAccessLogsMapper accessLogsMapper = mock(LinkAccessLogsMapper.class);
        LinkBrowserStatsMapper browserStatsMapper = mock(LinkBrowserStatsMapper.class);
        LinkOsStatsMapper osStatsMapper = mock(LinkOsStatsMapper.class);
        LinkDeviceStatsMapper deviceStatsMapper = mock(LinkDeviceStatsMapper.class);
        LinkNetworkStatsMapper networkStatsMapper = mock(LinkNetworkStatsMapper.class);
        LinkStatsServiceImpl service = new LinkStatsServiceImpl(
                mock(GroupMapper.class),
                mock(GroupOwnershipVerifier.class),
                accessStatsMapper,
                localeStatsMapper,
                accessLogsMapper,
                browserStatsMapper,
                osStatsMapper,
                deviceStatsMapper,
                networkStatsMapper
        );
        LinkStatsReqDTO request = new LinkStatsReqDTO();
        request.setFullShortUrl("127.0.0.1:8068/abc123");
        request.setGid("group-1");
        request.setStartDate("2026-07-10");
        request.setEndDate("2026-07-10");
        when(accessStatsMapper.listStatsByShortLink(request)).thenReturn(Collections.emptyList());
        when(accessStatsMapper.listHourStatsByShortLink(request)).thenReturn(Collections.emptyList());
        when(accessStatsMapper.listWeekdayStatsByShortLink(request)).thenReturn(Collections.emptyList());
        when(accessLogsMapper.findPvUvUidStatsByShortLink(request)).thenReturn(new LinkAccessStatsDO());
        when(accessLogsMapper.listTopIpByShortLink(request)).thenReturn(Collections.emptyList());
        when(localeStatsMapper.listLocaleByShortLink(request)).thenReturn(Collections.emptyList());
        when(browserStatsMapper.listBrowserStatsByShortLink(request)).thenReturn(Collections.emptyList());
        when(osStatsMapper.listOsStatsByShortLink(request)).thenReturn(Collections.emptyList());
        when(deviceStatsMapper.listDeviceStatsByShortLink(request)).thenReturn(Collections.emptyList());
        when(networkStatsMapper.listNetworkStatsByShortLink(request)).thenReturn(Collections.emptyList());

        service.oneShortLinkStats(request);

        assertEquals("2026-07-10 23:59:59", request.getEndDate());
    }
}
