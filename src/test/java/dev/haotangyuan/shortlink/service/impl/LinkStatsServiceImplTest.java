package dev.haotangyuan.shortlink.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.common.convention.exception.ClientException;
import dev.haotangyuan.shortlink.dao.entity.LinkAccessLogsDO;
import dev.haotangyuan.shortlink.dao.entity.LinkAccessStatsDO;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessLogsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkAccessStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkBrowserStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkDeviceStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkLocaleStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkNetworkStatsMapper;
import dev.haotangyuan.shortlink.dao.mapper.LinkOsStatsMapper;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.vo.LinkStatsAccessRecordVO;
import dev.haotangyuan.shortlink.vo.LinkStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LinkStatsServiceImplTest {

    @Mock
    private GroupOwnershipVerifier ownershipVerifier;
    @Mock
    private LinkAccessStatsMapper accessStatsMapper;
    @Mock
    private LinkLocaleStatsMapper localeStatsMapper;
    @Mock
    private LinkAccessLogsMapper accessLogsMapper;
    @Mock
    private LinkBrowserStatsMapper browserStatsMapper;
    @Mock
    private LinkOsStatsMapper osStatsMapper;
    @Mock
    private LinkDeviceStatsMapper deviceStatsMapper;
    @Mock
    private LinkNetworkStatsMapper networkStatsMapper;

    private LinkStatsServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new LinkStatsServiceImpl(
                ownershipVerifier,
                accessStatsMapper,
                localeStatsMapper,
                accessLogsMapper,
                browserStatsMapper,
                osStatsMapper,
                deviceStatsMapper,
                networkStatsMapper
        );
    }

    @Test
    void groupTotalsUseDistinctVisitorsAcrossTheWholeRange() {
        LinkAccessStatsDO firstDay = LinkAccessStatsDO.builder()
                .date(new Date(1767225600000L))
                .pv(1)
                .uv(1)
                .uip(1)
                .build();
        LinkAccessStatsDO secondDay = LinkAccessStatsDO.builder()
                .date(new Date(1767312000000L))
                .pv(1)
                .uv(1)
                .uip(1)
                .build();
        when(accessStatsMapper.listStatsByGroup(any())).thenReturn(List.of(firstDay, secondDay));
        when(accessLogsMapper.findPvUvUidStatsByGroup(any())).thenReturn(
                LinkAccessStatsDO.builder().pv(2).uv(1).uip(1).build()
        );
        when(accessStatsMapper.listHourStatsByGroup(any())).thenReturn(Collections.emptyList());
        when(accessStatsMapper.listWeekdayStatsByGroup(any())).thenReturn(Collections.emptyList());
        when(localeStatsMapper.listLocaleByGroup(any())).thenReturn(Collections.emptyList());
        when(accessLogsMapper.listTopIpByGroup(any())).thenReturn(Collections.emptyList());
        when(browserStatsMapper.listBrowserStatsByGroup(any())).thenReturn(Collections.emptyList());
        when(osStatsMapper.listOsStatsByGroup(any())).thenReturn(Collections.emptyList());
        when(deviceStatsMapper.listDeviceStatsByGroup(any())).thenReturn(Collections.emptyList());
        when(networkStatsMapper.listNetworkStatsByGroup(any())).thenReturn(Collections.emptyList());
        GroupStatsReqDTO request = new GroupStatsReqDTO();
        request.setGid("group-1");
        request.setStartDate("2026-01-01");
        request.setEndDate("2026-01-02");

        LinkStatsVO result = service.groupShortLinkStats(request);

        assertEquals(2, result.getPv());
        assertEquals(1, result.getUv());
        assertEquals(1, result.getUip());
    }

    @Test
    void emptyAccessRecordPagePreservesTheTotalAndUsesOwnershipAwareQuery() {
        Page<LinkAccessLogsDO> sourcePage = new Page<>(2, 10);
        sourcePage.setTotal(25);
        when(accessLogsMapper.selectLinkPage(any(), any())).thenReturn(sourcePage);
        LinkStatsAccessRecordReqDTO request = new LinkStatsAccessRecordReqDTO();
        request.setGid("group-1");
        request.setFullShortUrl("short.example/abc123");
        request.setStartDate("2026-01-01");
        request.setEndDate("2026-01-02");
        request.setCurrent(2);

        com.baomidou.mybatisplus.core.metadata.IPage<LinkStatsAccessRecordVO> result =
                service.shortLinkStatsAccessRecord(request);

        assertEquals(25, result.getTotal());
        assertEquals(0, request.getEnableStatus());
    }

    @Test
    void statsRangeIsBounded() {
        GroupStatsReqDTO request = new GroupStatsReqDTO();
        request.setGid("group-1");
        request.setStartDate("2024-01-01");
        request.setEndDate("2026-01-02");

        assertThrows(ClientException.class, () -> service.groupShortLinkStats(request));
    }
}
