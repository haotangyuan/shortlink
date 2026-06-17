package dev.haotangyuan.shortlink.ai.tool;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkPageReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkStatsReqDTO;
import dev.haotangyuan.shortlink.service.GroupService;
import dev.haotangyuan.shortlink.service.LinkService;
import dev.haotangyuan.shortlink.service.LinkStatsService;
import dev.haotangyuan.shortlink.vo.GroupVO;
import dev.haotangyuan.shortlink.vo.LinkPageVO;
import dev.haotangyuan.shortlink.vo.LinkStatsVO;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * AI 统计查询工具
 * <p>
 * 供 ReActAgent 通过 @Tool 注解调用，查询短链接统计数据。
 * 所有方法返回 JSON 字符串，便于 Agent 解析并生成自然语言分析报告。
 *
 * @author: haotangyuan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StatsTools {

    private final LinkStatsService linkStatsService;
    private final LinkService linkService;
    private final GroupService groupService;

    /**
     * 查询单条短链的多维统计数据
     */
    @Tool(name = "get_link_stats",
          description = "查询单条短链接在指定日期范围内的多维度统计数据，包括 PV/UV/UIP、每日趋势、"
                      + "小时分布、地区分布、浏览器分布、操作系统分布、设备类型、网络类型、"
                      + "新老访客比例、高频访问 IP 等。当用户询问某条短链的数据表现时使用此工具。")
    public String getLinkStats(
            @ToolParam(name = "fullShortUrl",
                       description = "短链接地址，例如 '127.0.0.1:8068/abc123'（不含 http://）") String fullShortUrl,
            @ToolParam(name = "gid",
                       description = "该短链所属的分组标识") String gid,
            @ToolParam(name = "startDate",
                       description = "统计开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate",
                       description = "统计结束日期，格式 yyyy-MM-dd") String endDate) {

        try {
            LinkStatsReqDTO reqDTO = new LinkStatsReqDTO();
            reqDTO.setFullShortUrl(fullShortUrl);
            reqDTO.setGid(gid);
            reqDTO.setStartDate(startDate);
            reqDTO.setEndDate(endDate);
            reqDTO.setEnableStatus(0);

            LinkStatsVO stats = linkStatsService.oneShortLinkStats(reqDTO);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("fullShortUrl", fullShortUrl);
            result.put("dateRange", startDate + " ~ " + endDate);
            result.put("pv", stats.getPv());
            result.put("uv", stats.getUv());
            result.put("uip", stats.getUip());
            result.put("daily", stats.getDaily());
            result.put("hourStats", stats.getHourStats());
            result.put("weekdayStats", stats.getWeekdayStats());
            result.put("localeCnStats", stats.getLocaleCnStats());
            result.put("browserStats", stats.getBrowserStats());
            result.put("osStats", stats.getOsStats());
            result.put("deviceStats", stats.getDeviceStats());
            result.put("networkStats", stats.getNetworkStats());
            result.put("uvTypeStats", stats.getUvTypeStats());
            result.put("topIpStats", stats.getTopIpStats());

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("AI tool get_link_stats error", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 查询某分组下所有短链的聚合统计
     */
    @Tool(name = "get_group_stats",
          description = "查询某个分组下所有短链接在指定日期范围内的聚合统计数据。"
                      + "当用户询问某个分组/类别的整体数据表现时使用此工具。"
                      + "如果用户没有指定分组，可以先用 list_groups 工具获取分组列表。")
    public String getGroupStats(
            @ToolParam(name = "gid",
                       description = "分组标识") String gid,
            @ToolParam(name = "startDate",
                       description = "统计开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate",
                       description = "统计结束日期，格式 yyyy-MM-dd") String endDate) {

        try {
            GroupStatsReqDTO reqDTO = new GroupStatsReqDTO();
            reqDTO.setGid(gid);
            reqDTO.setStartDate(startDate);
            reqDTO.setEndDate(endDate);

            LinkStatsVO stats = linkStatsService.groupShortLinkStats(reqDTO);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gid", gid);
            result.put("dateRange", startDate + " ~ " + endDate);
            result.put("pv", stats.getPv());
            result.put("uv", stats.getUv());
            result.put("uip", stats.getUip());
            result.put("daily", stats.getDaily());
            result.put("hourStats", stats.getHourStats());
            result.put("weekdayStats", stats.getWeekdayStats());
            result.put("localeCnStats", stats.getLocaleCnStats());
            result.put("browserStats", stats.getBrowserStats());
            result.put("osStats", stats.getOsStats());
            result.put("deviceStats", stats.getDeviceStats());
            result.put("networkStats", stats.getNetworkStats());
            result.put("uvTypeStats", stats.getUvTypeStats());

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("AI tool get_group_stats error", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 横向对比同一分组下多条短链的表现
     */
    @Tool(name = "compare_links",
          description = "横向对比某个分组下多条短链接在指定日期范围内的表现，"
                      + "返回每条短链的 PV、UV、UIP 等核心指标排名。"
                      + "当用户询问'哪条链接表现最好/最差'、'链接对比'时使用此工具。")
    public String compareLinks(
            @ToolParam(name = "gid",
                       description = "分组标识") String gid,
            @ToolParam(name = "startDate",
                       description = "统计开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate",
                       description = "统计结束日期，格式 yyyy-MM-dd") String endDate) {

        try {
            LinkPageReqDTO pageReq = new LinkPageReqDTO();
            pageReq.setGid(gid);
            pageReq.setCurrent(1);
            pageReq.setSize(20);
            pageReq.setOrderTag("totalPv");

            IPage<LinkPageVO> page = linkService.pageLink(pageReq);
            List<LinkPageVO> links = page.getRecords();

            List<Map<String, Object>> comparison = new ArrayList<>();
            for (LinkPageVO link : links) {
                LinkStatsReqDTO statsReq = new LinkStatsReqDTO();
                statsReq.setFullShortUrl(link.getFullShortUrl());
                statsReq.setGid(gid);
                statsReq.setStartDate(startDate);
                statsReq.setEndDate(endDate);
                statsReq.setEnableStatus(0);

                LinkStatsVO stats = linkStatsService.oneShortLinkStats(statsReq);

                Map<String, Object> row = new LinkedHashMap<>();
                row.put("fullShortUrl", link.getFullShortUrl());
                row.put("originUrl", link.getOriginUrl());
                row.put("describe", link.getDescribe());
                row.put("pv", stats.getPv());
                row.put("uv", stats.getUv());
                row.put("uip", stats.getUip());
                comparison.add(row);
            }

            // 按 PV 降序排序
            comparison.sort((a, b) -> Integer.compare(
                    (int) b.getOrDefault("pv", 0),
                    (int) a.getOrDefault("pv", 0)));

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gid", gid);
            result.put("dateRange", startDate + " ~ " + endDate);
            result.put("totalLinks", links.size());
            result.put("links", comparison);

            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("AI tool compare_links error", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 列出用户的所有分组
     */
    @Tool(name = "list_groups",
          description = "列出当前用户的所有短链接分组，返回分组标识（gid）和分组名称。"
                      + "当用户没有指定具体分组、或需要查看有哪些分组时使用此工具。")
    public String listGroups() {
        try {
            List<GroupVO> groups = groupService.listGroup();
            List<Map<String, Object>> result = groups.stream()
                    .map(g -> {
                        Map<String, Object> m = new LinkedHashMap<>();
                        m.put("gid", g.getGid());
                        m.put("name", g.getName());
                        return m;
                    })
                    .collect(Collectors.toList());
            return JSON.toJSONString(result);
        } catch (Exception e) {
            log.error("AI tool list_groups error", e);
            return "{\"error\": \"查询失败: " + e.getMessage() + "\"}";
        }
    }
}
