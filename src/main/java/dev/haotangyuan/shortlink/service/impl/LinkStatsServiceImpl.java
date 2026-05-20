package dev.haotangyuan.shortlink.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import dev.haotangyuan.shortlink.common.biz.user.GroupOwnershipVerifier;
import dev.haotangyuan.shortlink.dao.entity.*;
import dev.haotangyuan.shortlink.dao.mapper.*;
import dev.haotangyuan.shortlink.dto.req.GroupStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsAccessRecordReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import dev.haotangyuan.shortlink.vo.*;
import dev.haotangyuan.shortlink.service.LinkStatsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 访问统计接口实现层
 *
 * @author: haotangyuan
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LinkStatsServiceImpl implements LinkStatsService {

    private final GroupMapper groupMapper;
    private final GroupOwnershipVerifier groupOwnershipService;
    private final LinkAccessStatsMapper linkAccessStatsMapper;
    private final LinkLocaleStatsMapper linkLocaleStatsMapper;
    private final LinkAccessLogsMapper linkAccessLogsMapper;
    private final LinkBrowserStatsMapper linkBrowserStatsMapper;
    private final LinkOsStatsMapper linkOsStatsMapper;
    private final LinkDeviceStatsMapper linkDeviceStatsMapper;
    private final LinkNetworkStatsMapper linkNetworkStatsMapper;

    @Override
    public LinkStatsVO oneShortLinkStats(LinkStatsReqDTO linkStatsReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(linkStatsReqDTO.getGid());
        List<LinkAccessStatsDO> listStatsByShortLink = linkAccessStatsMapper.listStatsByShortLink(linkStatsReqDTO);
        if (CollUtil.isEmpty(listStatsByShortLink)) {
            return null;
        }
        // 基础访问数据
        LinkAccessStatsDO pvUvUidStatsByShortLink = linkAccessLogsMapper.findPvUvUidStatsByShortLink(linkStatsReqDTO);
        // 基础访问详情
        List<LinkStatsAccessDailyVO> daily = new ArrayList<>();
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(linkStatsReqDTO.getStartDate()), DateUtil.parse(linkStatsReqDTO.getEndDate()), DateField.DAY_OF_MONTH).stream()
                .map(DateUtil::formatDate)
                .toList();
        rangeDates.forEach(each -> listStatsByShortLink.stream()
                .filter(item -> Objects.equals(each, DateUtil.formatDate(item.getDate())))
                .findFirst()
                .ifPresentOrElse(item -> {
                    LinkStatsAccessDailyVO accessDailyRespDTO = LinkStatsAccessDailyVO.builder()
                            .date(each)
                            .pv(item.getPv())
                            .uv(item.getUv())
                            .uip(item.getUip())
                            .build();
                    daily.add(accessDailyRespDTO);
                }, () -> {
                    LinkStatsAccessDailyVO accessDailyRespDTO = LinkStatsAccessDailyVO.builder()
                            .date(each)
                            .pv(0)
                            .uv(0)
                            .uip(0)
                            .build();
                    daily.add(accessDailyRespDTO);
                }));
        // 地区访问详情
        List<LinkStatsLocaleCNVO> localeCnStats = new ArrayList<>();
        List<LinkLocaleStatsDO> listedLocaleByShortLink = linkLocaleStatsMapper.listLocaleByShortLink(linkStatsReqDTO);
        int localeCnSum = listedLocaleByShortLink.stream()
                .mapToInt(LinkLocaleStatsDO::getCnt)
                .sum();
        listedLocaleByShortLink.forEach(each -> {
            double actualRatio = calculateRatio(each.getCnt(), localeCnSum);
            LinkStatsLocaleCNVO localeCNRespDTO = LinkStatsLocaleCNVO.builder()
                    .cnt(each.getCnt())
                    .locale(each.getProvince())
                    .ratio(actualRatio)
                    .build();
            localeCnStats.add(localeCNRespDTO);
        });
        // 小时访问详情
        List<Integer> hourStats = new ArrayList<>();
        List<LinkAccessStatsDO> listHourStatsByShortLink = linkAccessStatsMapper.listHourStatsByShortLink(linkStatsReqDTO);
        for (int i = 0; i < 24; i++) {
            int h = i;
            int hourCnt = listHourStatsByShortLink.stream()
                    .filter(each -> Objects.equals(each.getHour(), h))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hourCnt);
        }
        // 高频访问IP详情
        List<LinkStatsTopIpVO> topIpStats = new ArrayList<>();
        List<HashMap<String, Object>> listTopIpByShortLink = linkAccessLogsMapper.listTopIpByShortLink(linkStatsReqDTO);
        listTopIpByShortLink.forEach(each -> {
            LinkStatsTopIpVO statsTopIpRespDTO = LinkStatsTopIpVO.builder()
                    .ip(each.get("ip").toString())
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .build();
            topIpStats.add(statsTopIpRespDTO);
        });
        // 一周访问详情
        List<Integer> weekdayStats = new ArrayList<>();
        List<LinkAccessStatsDO> listWeekdayStatsByShortLink = linkAccessStatsMapper.listWeekdayStatsByShortLink(linkStatsReqDTO);
        for (int i = 1; i < 8; i++) {
            int wd = i;
            int weekdayCnt = listWeekdayStatsByShortLink.stream()
                    .filter(each -> Objects.equals(each.getWeekday(), wd))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            weekdayStats.add(weekdayCnt);
        }
        // 浏览器访问详情
        List<LinkStatsBrowserVO> browserStats = new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByShortLink = linkBrowserStatsMapper.listBrowserStatsByShortLink(linkStatsReqDTO);
        int browserSum = listBrowserStatsByShortLink.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listBrowserStatsByShortLink.forEach(each -> {
            int cnt = Integer.parseInt(each.get("count").toString());
            double actualRatio = calculateRatio(cnt, browserSum);
            LinkStatsBrowserVO browserRespDTO = LinkStatsBrowserVO.builder()
                    .cnt(cnt)
                    .browser(each.get("browser").toString())
                    .ratio(actualRatio)
                    .build();
            browserStats.add(browserRespDTO);
        });
        // 操作系统访问详情
        List<LinkStatsOsVO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByShortLink = linkOsStatsMapper.listOsStatsByShortLink(linkStatsReqDTO);
        int osSum = listOsStatsByShortLink.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listOsStatsByShortLink.forEach(each -> {
            int cnt = Integer.parseInt(each.get("count").toString());
            double actualRatio = calculateRatio(cnt, osSum);
            LinkStatsOsVO osRespDTO = LinkStatsOsVO.builder()
                    .cnt(cnt)
                    .os(each.get("os").toString())
                    .ratio(actualRatio)
                    .build();
            osStats.add(osRespDTO);
        });
        // 访客访问类型详情
        List<LinkStatsUvVO> uvTypeStats = new ArrayList<>();
        HashMap<String, Object> findUvTypeByShortLink = linkAccessLogsMapper.findUvTypeCntByShortLink(linkStatsReqDTO);
        int oldUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("oldUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int newUserCnt = Integer.parseInt(
                Optional.ofNullable(findUvTypeByShortLink)
                        .map(each -> each.get("newUserCnt"))
                        .map(Object::toString)
                        .orElse("0")
        );
        int uvSum = oldUserCnt + newUserCnt;
        double actualOldRatio = calculateRatio(oldUserCnt, uvSum);
        double actualNewRatio = calculateRatio(newUserCnt, uvSum);
        LinkStatsUvVO newUvRespDTO = LinkStatsUvVO.builder()
                .uvType("newUser")
                .cnt(newUserCnt)
                .ratio(actualNewRatio)
                .build();
        uvTypeStats.add(newUvRespDTO);
        LinkStatsUvVO oldUvRespDTO = LinkStatsUvVO.builder()
                .uvType("oldUser")
                .cnt(oldUserCnt)
                .ratio(actualOldRatio)
                .build();
        uvTypeStats.add(oldUvRespDTO);
        // 访问设备类型详情
        List<LinkStatsDeviceVO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByShortLink = linkDeviceStatsMapper.listDeviceStatsByShortLink(linkStatsReqDTO);
        int deviceSum = listDeviceStatsByShortLink.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        listDeviceStatsByShortLink.forEach(each -> {
            double actualRatio = calculateRatio(each.getCnt(), deviceSum);
            LinkStatsDeviceVO deviceRespDTO = LinkStatsDeviceVO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceStats.add(deviceRespDTO);
        });
        // 访问网络类型详情
        List<LinkStatsNetworkVO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByShortLink = linkNetworkStatsMapper.listNetworkStatsByShortLink(linkStatsReqDTO);
        int networkSum = listNetworkStatsByShortLink.stream()
                .mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        listNetworkStatsByShortLink.forEach(each -> {
            double actualRatio = calculateRatio(each.getCnt(), networkSum);
            LinkStatsNetworkVO networkRespDTO = LinkStatsNetworkVO.builder()
                    .cnt(each.getCnt())
                    .network(each.getNetwork())
                    .ratio(actualRatio)
                    .build();
            networkStats.add(networkRespDTO);
        });
        return LinkStatsVO.builder()
                .pv(pvUvUidStatsByShortLink.getPv())
                .uv(pvUvUidStatsByShortLink.getUv())
                .uip(pvUvUidStatsByShortLink.getUip())
                .daily(daily)
                .localeCnStats(localeCnStats)
                .hourStats(hourStats)
                .topIpStats(topIpStats)
                .weekdayStats(weekdayStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .uvTypeStats(uvTypeStats)
                .deviceStats(deviceStats)
                .networkStats(networkStats)
                .build();
    }

    @Override
    public IPage<LinkStatsAccessRecordVO> shortLinkStatsAccessRecord(LinkStatsAccessRecordReqDTO linkStatsAccessRecordReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(linkStatsAccessRecordReqDTO.getGid());
        LambdaQueryWrapper<LinkAccessLogsDO> queryWrapper = Wrappers.lambdaQuery(LinkAccessLogsDO.class)
                .eq(LinkAccessLogsDO::getFullShortUrl, linkStatsAccessRecordReqDTO.getFullShortUrl())
                .between(LinkAccessLogsDO::getCreateTime, linkStatsAccessRecordReqDTO.getStartDate(), linkStatsAccessRecordReqDTO.getEndDate())
                .eq(LinkAccessLogsDO::getDelFlag, 0)
                .orderByDesc(LinkAccessLogsDO::getCreateTime);
        Page<LinkAccessLogsDO> page = new Page<>(linkStatsAccessRecordReqDTO.getCurrent(), linkStatsAccessRecordReqDTO.getSize());
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectPage(page, queryWrapper);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>(linkStatsAccessRecordReqDTO.getCurrent(), linkStatsAccessRecordReqDTO.getSize());
        }
        IPage<LinkStatsAccessRecordVO> actualResult = linkAccessLogsDOIPage.convert(each -> {
            LinkStatsAccessRecordVO linkStatsAccessRecordRespDTO = BeanUtil.toBean(each, LinkStatsAccessRecordVO.class);
            linkStatsAccessRecordRespDTO.setUvType(Boolean.TRUE.equals(each.getFirstFlag()) ? "新访客" : "老访客");
            return linkStatsAccessRecordRespDTO;
        });
        return actualResult;
    }

    @Override
    public LinkStatsVO groupShortLinkStats(GroupStatsReqDTO groupStatsReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(groupStatsReqDTO.getGid());
        List<LinkAccessStatsDO> listStatsByGroup = Optional.ofNullable(linkAccessStatsMapper.listStatsByGroup(groupStatsReqDTO))
                .orElse(Collections.emptyList());

        int totalPv = listStatsByGroup.stream()
                .mapToInt(item -> Optional.ofNullable(item.getPv()).orElse(0))
                .sum();
        int totalUv = listStatsByGroup.stream()
                .mapToInt(item -> Optional.ofNullable(item.getUv()).orElse(0))
                .sum();
        int totalUip = listStatsByGroup.stream()
                .mapToInt(item -> Optional.ofNullable(item.getUip()).orElse(0))
                .sum();

        // 基础访问详情（即使 listStatsByGroup 为空也继续构造）
        List<LinkStatsAccessDailyVO> daily = new ArrayList<>();
        List<String> rangeDates = DateUtil.rangeToList(DateUtil.parse(groupStatsReqDTO.getStartDate()), DateUtil.parse(groupStatsReqDTO.getEndDate()), DateField.DAY_OF_MONTH).stream()
                .map(DateUtil::formatDate)
                .toList();
        rangeDates.forEach(each -> {
            if (CollUtil.isNotEmpty(listStatsByGroup)) {
                listStatsByGroup.stream()
                        .filter(item -> Objects.equals(each, DateUtil.formatDate(item.getDate())))
                        .findFirst()
                        .ifPresentOrElse(item -> {
                            LinkStatsAccessDailyVO accessDailyRespDTO = LinkStatsAccessDailyVO.builder()
                                    .date(each)
                                    .pv(item.getPv())
                                    .uv(item.getUv())
                                    .uip(item.getUip())
                                    .build();
                            daily.add(accessDailyRespDTO);
                        }, () -> {
                            LinkStatsAccessDailyVO accessDailyRespDTO = LinkStatsAccessDailyVO.builder()
                                    .date(each)
                                    .pv(0)
                                    .uv(0)
                                    .uip(0)
                                    .build();
                            daily.add(accessDailyRespDTO);
                        });
            } else {
                // listStatsByGroup 为空时，所有日期返回0
                LinkStatsAccessDailyVO accessDailyRespDTO = LinkStatsAccessDailyVO.builder()
                        .date(each)
                        .pv(0)
                        .uv(0)
                        .uip(0)
                        .build();
                daily.add(accessDailyRespDTO);
            }
        });
        // 地区访问详情（仅国内）
        List<LinkStatsLocaleCNVO> localeCnStats = new ArrayList<>();
        List<LinkLocaleStatsDO> listedLocaleByGroup = linkLocaleStatsMapper.listLocaleByGroup(groupStatsReqDTO);
        int localeCnSum = listedLocaleByGroup.stream()
                .mapToInt(LinkLocaleStatsDO::getCnt)
                .sum();
        listedLocaleByGroup.forEach(each -> {
            double actualRatio = calculateRatio(each.getCnt(), localeCnSum);
            LinkStatsLocaleCNVO localeCNRespDTO = LinkStatsLocaleCNVO.builder()
                    .cnt(each.getCnt())
                    .locale(each.getProvince())
                    .ratio(actualRatio)
                    .build();
            localeCnStats.add(localeCNRespDTO);
        });
        // 小时访问详情
        List<Integer> hourStats = new ArrayList<>();
        List<LinkAccessStatsDO> listHourStatsByGroup = CollUtil.isNotEmpty(listStatsByGroup)
                ? linkAccessStatsMapper.listHourStatsByGroup(groupStatsReqDTO)
                : new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            int h = i;
            int hourCnt = listHourStatsByGroup.stream()
                    .filter(each -> Objects.equals(each.getHour(), h))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            hourStats.add(hourCnt);
        }
        // 高频访问IP详情
        List<LinkStatsTopIpVO> topIpStats = new ArrayList<>();
        List<HashMap<String, Object>> listTopIpByGroup = linkAccessLogsMapper.listTopIpByGroup(groupStatsReqDTO);
        listTopIpByGroup.forEach(each -> {
            LinkStatsTopIpVO statsTopIpRespDTO = LinkStatsTopIpVO.builder()
                    .ip(each.get("ip").toString())
                    .cnt(Integer.parseInt(each.get("count").toString()))
                    .build();
            topIpStats.add(statsTopIpRespDTO);
        });
        // 一周访问详情
        List<Integer> weekdayStats = new ArrayList<>();
        List<LinkAccessStatsDO> listWeekdayStatsByGroup = CollUtil.isNotEmpty(listStatsByGroup)
                ? linkAccessStatsMapper.listWeekdayStatsByGroup(groupStatsReqDTO)
                : new ArrayList<>();
        for (int i = 1; i < 8; i++) {
            int wd = i;
            int weekdayCnt = listWeekdayStatsByGroup.stream()
                    .filter(each -> Objects.equals(each.getWeekday(), wd))
                    .findFirst()
                    .map(LinkAccessStatsDO::getPv)
                    .orElse(0);
            weekdayStats.add(weekdayCnt);
        }
        // 浏览器访问详情
        List<LinkStatsBrowserVO> browserStats = new ArrayList<>();
        List<HashMap<String, Object>> listBrowserStatsByGroup = linkBrowserStatsMapper.listBrowserStatsByGroup(groupStatsReqDTO);
        int browserSum = listBrowserStatsByGroup.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listBrowserStatsByGroup.forEach(each -> {
            int cnt = Integer.parseInt(each.get("count").toString());
            double actualRatio = calculateRatio(cnt, browserSum);
            LinkStatsBrowserVO browserRespDTO = LinkStatsBrowserVO.builder()
                    .cnt(cnt)
                    .browser(each.get("browser").toString())
                    .ratio(actualRatio)
                    .build();
            browserStats.add(browserRespDTO);
        });
        // 操作系统访问详情
        List<LinkStatsOsVO> osStats = new ArrayList<>();
        List<HashMap<String, Object>> listOsStatsByGroup = linkOsStatsMapper.listOsStatsByGroup(groupStatsReqDTO);
        int osSum = listOsStatsByGroup.stream()
                .mapToInt(each -> Integer.parseInt(each.get("count").toString()))
                .sum();
        listOsStatsByGroup.forEach(each -> {
            int cnt = Integer.parseInt(each.get("count").toString());
            double actualRatio = calculateRatio(cnt, osSum);
            LinkStatsOsVO osRespDTO = LinkStatsOsVO.builder()
                    .cnt(cnt)
                    .os(each.get("os").toString())
                    .ratio(actualRatio)
                    .build();
            osStats.add(osRespDTO);
        });
        // 访问设备类型详情
        List<LinkStatsDeviceVO> deviceStats = new ArrayList<>();
        List<LinkDeviceStatsDO> listDeviceStatsByGroup = linkDeviceStatsMapper.listDeviceStatsByGroup(groupStatsReqDTO);
        int deviceSum = listDeviceStatsByGroup.stream()
                .mapToInt(LinkDeviceStatsDO::getCnt)
                .sum();
        listDeviceStatsByGroup.forEach(each -> {
            double actualRatio = calculateRatio(each.getCnt(), deviceSum);
            LinkStatsDeviceVO deviceRespDTO = LinkStatsDeviceVO.builder()
                    .cnt(each.getCnt())
                    .device(each.getDevice())
                    .ratio(actualRatio)
                    .build();
            deviceStats.add(deviceRespDTO);
        });
        // 访问网络类型详情
        List<LinkStatsNetworkVO> networkStats = new ArrayList<>();
        List<LinkNetworkStatsDO> listNetworkStatsByGroup = linkNetworkStatsMapper.listNetworkStatsByGroup(groupStatsReqDTO);
        int networkSum = listNetworkStatsByGroup.stream()
                .mapToInt(LinkNetworkStatsDO::getCnt)
                .sum();
        listNetworkStatsByGroup.forEach(each -> {
            double actualRatio = calculateRatio(each.getCnt(), networkSum);
            LinkStatsNetworkVO networkRespDTO = LinkStatsNetworkVO.builder()
                    .cnt(each.getCnt())
                    .network(each.getNetwork())
                    .ratio(actualRatio)
                    .build();
            networkStats.add(networkRespDTO);
        });

        return LinkStatsVO.builder()
                .pv(totalPv)
                .uv(totalUv)
                .uip(totalUip)
                .daily(daily)
                .localeCnStats(localeCnStats)
                .hourStats(hourStats)
                .topIpStats(topIpStats)
                .weekdayStats(weekdayStats)
                .browserStats(browserStats)
                .osStats(osStats)
                .deviceStats(deviceStats)
                .networkStats(networkStats)
                .build();
    }

    @Override
    public IPage<LinkStatsAccessRecordVO> groupShortLinkStatsAccessRecord(GroupStatsAccessRecordReqDTO groupStatsAccessRecordReqDTO) {
        groupOwnershipService.assertOwnedByCurrentUser(groupStatsAccessRecordReqDTO.getGid());
        Page<LinkAccessLogsDO> page = new Page<>(groupStatsAccessRecordReqDTO.getCurrent(), groupStatsAccessRecordReqDTO.getSize());
        IPage<LinkAccessLogsDO> linkAccessLogsDOIPage = linkAccessLogsMapper.selectGroupPage(page, groupStatsAccessRecordReqDTO);
        if (CollUtil.isEmpty(linkAccessLogsDOIPage.getRecords())) {
            return new Page<>(groupStatsAccessRecordReqDTO.getCurrent(), groupStatsAccessRecordReqDTO.getSize());
        }
        IPage<LinkStatsAccessRecordVO> actualResult = linkAccessLogsDOIPage
                .convert(each -> {
                    LinkStatsAccessRecordVO linkStatsAccessRecordRespDTO = BeanUtil.toBean(each, LinkStatsAccessRecordVO.class);
                    linkStatsAccessRecordRespDTO.setUvType(Boolean.TRUE.equals(each.getFirstFlag()) ? "新访客" : "老访客");
                    return linkStatsAccessRecordRespDTO;
                });
        return actualResult;
    }

    private static double calculateRatio(int cnt, int sum) {
        if (sum == 0) {
            return 0.0;
        }
        return Math.round((double) cnt / sum * 100.0) / 100.0;
    }
}
