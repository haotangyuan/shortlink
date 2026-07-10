package dev.haotangyuan.shortlink.ai.tool;

import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.metadata.IPage;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.dto.req.GroupStatsReqDTO;
import dev.haotangyuan.shortlink.dto.req.LinkPageReqDTO;
import dev.haotangyuan.shortlink.service.LinkService;
import dev.haotangyuan.shortlink.service.LinkStatsService;
import dev.haotangyuan.shortlink.vo.LinkPageVO;
import dev.haotangyuan.shortlink.vo.LinkStatsAccessDailyVO;
import dev.haotangyuan.shortlink.vo.LinkStatsVO;
import io.agentscope.core.tool.Tool;
import io.agentscope.core.tool.ToolParam;
import io.agentscope.core.agent.RuntimeContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Supplier;

/**
 * AI 洞察分析工具
 * <p>
 * 提供异常检测和链接健康检查等运营洞察能力，帮助 Agent 发现数据中的问题和机会。
 *
 * @author: haotangyuan
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InsightTools {

    private final LinkStatsService linkStatsService;
    private final LinkService linkService;

    /**
     * 检测流量数据异常
     */
    @Tool(name = "detect_anomalies",
          description = "检测指定分组在指定日期范围内的流量异常事件。"
                      + "包括：PV 环比下降超过 50%、UV 环比飙升超过 100%、连续多日零流量等。"
                      + "当用户问'流量有没有异常'、'点击量为什么下降'等问题时使用此工具。")
    public String detectAnomalies(
            @ToolParam(name = "gid",
                       description = "分组标识") String gid,
            @ToolParam(name = "startDate",
                       description = "检测开始日期，格式 yyyy-MM-dd") String startDate,
            @ToolParam(name = "endDate",
                       description = "检测结束日期，格式 yyyy-MM-dd") String endDate,
            RuntimeContext runtimeContext) {

        return runAsUser(runtimeContext, "detect_anomalies", "检测失败", () -> {
            GroupStatsReqDTO reqDTO = new GroupStatsReqDTO();
            reqDTO.setGid(gid);
            reqDTO.setStartDate(startDate);
            reqDTO.setEndDate(endDate);

            LinkStatsVO stats = linkStatsService.groupShortLinkStats(reqDTO);
            List<LinkStatsAccessDailyVO> daily = stats.getDaily();

            if (daily == null || daily.size() < 2) {
                return "{\"message\": \"数据不足，无法进行异常检测（至少需要 2 天的数据）\"}";
            }

            List<Map<String, Object>> anomalies = new ArrayList<>();

            for (int i = 1; i < daily.size(); i++) {
                LinkStatsAccessDailyVO prev = daily.get(i - 1);
                LinkStatsAccessDailyVO curr = daily.get(i);

                int prevPv = prev.getPv() != null ? prev.getPv() : 0;
                int currPv = curr.getPv() != null ? curr.getPv() : 0;
                int prevUv = prev.getUv() != null ? prev.getUv() : 0;
                int currUv = curr.getUv() != null ? curr.getUv() : 0;

                // PV 环比下降超过 50%
                if (prevPv > 0) {
                    double pvChange = (double) (currPv - prevPv) / prevPv;
                    if (pvChange < -0.5) {
                        Map<String, Object> anomaly = new LinkedHashMap<>();
                        anomaly.put("type", "PV骤降");
                        anomaly.put("date", curr.getDate());
                        anomaly.put("detail", String.format(
                                "PV 从 %d 降至 %d（环比下降 %.1f%%）",
                                prevPv, currPv, Math.abs(pvChange) * 100));
                        anomaly.put("severity", pvChange < -0.7 ? "high" : "medium");
                        anomalies.add(anomaly);
                    }
                }

                // UV 环比飙升超过 100%
                if (prevUv > 0 && currUv > 0) {
                    double uvChange = (double) (currUv - prevUv) / prevUv;
                    if (uvChange > 1.0) {
                        Map<String, Object> anomaly = new LinkedHashMap<>();
                        anomaly.put("type", "UV飙升");
                        anomaly.put("date", curr.getDate());
                        anomaly.put("detail", String.format(
                                "UV 从 %d 升至 %d（环比上升 %.1f%%）",
                                prevUv, currUv, uvChange * 100));
                        anomaly.put("severity", uvChange > 2.0 ? "high" : "medium");
                        anomalies.add(anomaly);
                    }
                }
            }

            // 检测连续零流量（连续 3 天及以上 PV=0）
            int zeroStreak = 0;
            String streakStart = null;
            for (int i = 0; i < daily.size(); i++) {
                LinkStatsAccessDailyVO day = daily.get(i);
                int pv = day.getPv() != null ? day.getPv() : 0;
                if (pv == 0) {
                    if (zeroStreak == 0) {
                        streakStart = day.getDate();
                    }
                    zeroStreak++;
                } else {
                    if (zeroStreak >= 3) {
                        Map<String, Object> anomaly = new LinkedHashMap<>();
                        anomaly.put("type", "连续零流量");
                        anomaly.put("date", streakStart + " ~ " + daily.get(i - 1).getDate());
                        anomaly.put("detail", String.format("连续 %d 天零访问量", zeroStreak));
                        anomaly.put("severity", "high");
                        anomalies.add(anomaly);
                    }
                    zeroStreak = 0;
                }
            }
            if (zeroStreak >= 3) {
                Map<String, Object> anomaly = new LinkedHashMap<>();
                anomaly.put("type", "连续零流量");
                anomaly.put("date", streakStart + " ~ " + daily.get(daily.size() - 1).getDate());
                anomaly.put("detail", String.format("连续 %d 天零访问量", zeroStreak));
                anomaly.put("severity", "high");
                anomalies.add(anomaly);
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gid", gid);
            result.put("dateRange", startDate + " ~ " + endDate);
            result.put("totalDays", daily.size());
            result.put("anomalyCount", anomalies.size());
            result.put("anomalies", anomalies);

            if (anomalies.isEmpty()) {
                result.put("message", "未检测到明显异常，数据表现平稳");
            }

            return JSON.toJSONString(result);
        });
    }

    /**
     * 检查链接健康状态
     */
    @Tool(name = "get_link_health",
          description = "检查指定分组下所有短链接的健康状态，识别以下问题：\n"
                      + "1. 已过期链接（validDate < 当前时间）\n"
                      + "2. 已禁用链接（enableStatus != 0）\n"
                      + "3. 零点击僵尸链接（历史总 PV = 0）\n"
                      + "当用户问'有没有失效链接'、'检查链接健康'、'清理僵尸链接'时使用此工具。")
    public String getLinkHealth(
            @ToolParam(name = "gid",
                       description = "分组标识") String gid,
            RuntimeContext runtimeContext) {

        return runAsUser(runtimeContext, "get_link_health", "检查失败", () -> {
            LinkPageReqDTO pageReq = new LinkPageReqDTO();
            pageReq.setGid(gid);
            pageReq.setCurrent(1);
            pageReq.setSize(100);

            IPage<LinkPageVO> page = linkService.pageLink(pageReq);
            List<LinkPageVO> links = page.getRecords();

            Date now = new Date();
            List<Map<String, Object>> expired = new ArrayList<>();
            List<Map<String, Object>> zeroTraffic = new ArrayList<>();
            long totalLinks = page.getTotal();
            int healthyCount = 0;

            for (LinkPageVO link : links) {
                boolean hasIssue = false;

                // 检查过期
                if (link.getValidDateType() != null && link.getValidDateType() == 1
                        && link.getValidDate() != null && link.getValidDate().before(now)) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("fullShortUrl", link.getFullShortUrl());
                    item.put("originUrl", link.getOriginUrl());
                    item.put("describe", link.getDescribe());
                    item.put("validDate", link.getValidDate().toString());
                    item.put("issue", "已过期");
                    expired.add(item);
                    hasIssue = true;
                }

                // 检查零流量
                int totalPv = link.getTotalPv() != null ? link.getTotalPv() : 0;
                if (totalPv == 0) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("fullShortUrl", link.getFullShortUrl());
                    item.put("originUrl", link.getOriginUrl());
                    item.put("describe", link.getDescribe());
                    item.put("createTime", link.getCreateTime());
                    item.put("issue", "零点击");
                    zeroTraffic.add(item);
                    hasIssue = true;
                }

                if (!hasIssue) {
                    healthyCount++;
                }
            }

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("gid", gid);
            result.put("totalLinks", totalLinks);
            result.put("checkedLinks", links.size());
            result.put("truncated", totalLinks > links.size());
            result.put("healthyCount", healthyCount);
            result.put("expiredLinks", expired);
            result.put("expiredCount", expired.size());
            result.put("zeroTrafficLinks", zeroTraffic);
            result.put("zeroTrafficCount", zeroTraffic.size());

            if (expired.isEmpty() && zeroTraffic.isEmpty()) {
                result.put("message", "所有链接状态健康，无异常");
            }

            return JSON.toJSONString(result);
        });
    }

    private String runAsUser(
            RuntimeContext runtimeContext,
            String operation,
            String failureMessage,
            Supplier<String> action) {
        String username = runtimeContext == null ? null : runtimeContext.getUserId();
        if (username == null || username.isBlank()) {
            return errorResponse("用户未登录");
        }
        String previousUsername = UserContext.getUsername();
        UserContext.setUsername(username);
        try {
            return action.get();
        } catch (Exception ex) {
            log.error("AI tool {} error", operation, ex);
            return errorResponse(failureMessage);
        } finally {
            if (previousUsername == null) {
                UserContext.removeUser();
            } else {
                UserContext.setUsername(previousUsername);
            }
        }
    }

    private String errorResponse(String message) {
        return JSON.toJSONString(Map.of("error", message));
    }
}
