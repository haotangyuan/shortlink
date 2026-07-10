package dev.haotangyuan.shortlink.ai;

import dev.haotangyuan.shortlink.ai.tool.InsightTools;
import dev.haotangyuan.shortlink.ai.tool.StatsTools;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.model.OpenAIChatModel;
import io.agentscope.core.tool.Toolkit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.time.LocalDate;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;

/**
 * AI Agent 配置类
 * <p>
 * 基于 AgentScope Java 2.0 构建运营分析 ReActAgent，
 * 注册统计查询和洞察分析工具，供前端 AI Copilot 侧边栏调用。
 *
 * @author: haotangyuan
 */
@Slf4j
@Configuration
@ConditionalOnProperty(prefix = "short-link.ai", name = "enabled", havingValue = "true")
public class AgentConfig {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
            你是 ShortLink 智能短链运营分析助手。你的职责是帮助用户分析短链接的运营数据，提供有价值的洞察和建议。

            今天是 %s。

            你可以使用以下工具查询数据：
            - list_groups: 列出用户的所有分组，获取分组标识（gid）
            - get_link_stats: 查询单条短链的多维度统计数据（PV/UV/UIP、每日趋势、地区、浏览器、设备等）
            - get_group_stats: 查询某个分组下所有短链的聚合统计
            - compare_links: 横向对比某分组下多条短链的表现排名
            - detect_anomalies: 检测流量异常（PV骤降、UV飙升、连续零流量等）
            - get_link_health: 检查过期/失效/零点击的僵尸链接

            工作原则：
            1. 如果用户没有指定分组，先用 list_groups 获取分组列表，让用户选择或默认使用第一个分组
            2. 如果用户没有指定日期范围，默认使用最近 7 天（基于今天的日期计算）
            3. 先理解用户的问题意图，再选择合适的工具获取数据
            4. 基于数据给出具体、可操作的分析结论，不要泛泛而谈
            5. 如果发现异常数据，主动提醒并给出可能原因和优化建议
            6. 回答使用中文，数据用表格或列表呈现，结论用简洁的文字
            7. 如果用户的问题无法通过现有工具回答（如预测未来趋势），如实说明数据限制
            """;

    @Bean
    public Toolkit analyticsToolkit(StatsTools statsTools, InsightTools insightTools) {
        Toolkit toolkit = new Toolkit();
        toolkit.registerTool(statsTools);
        toolkit.registerTool(insightTools);
        return toolkit;
    }

    @Bean
    @Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
    public ReActAgent analyticsAgent(
            Toolkit analyticsToolkit,
            @Value("${short-link.ai.api-key}") String apiKey,
            @Value("${short-link.ai.model-name}") String modelName,
            @Value("${short-link.ai.base-url}") String baseUrl,
            @Value("${short-link.ai.max-iters:10}") int maxIters) {

        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AI API Key 未配置，AI Copilot 功能将不可用。"
                    + "请在 application.yaml 中配置 short-link.ai.api-key");
            return null;
        }

        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.formatted(LocalDate.now());

        ReActAgent agent = ReActAgent.builder()
                .name("ShortLinkAnalyst")
                .sysPrompt(systemPrompt)
                .model(OpenAIChatModel.builder()
                        .apiKey(apiKey)
                        .modelName(modelName)
                        .baseUrl(baseUrl)
                        .build())
                .toolkit(analyticsToolkit)
                .maxIters(maxIters)
                .build();

        log.debug("AI Copilot Agent 初始化完成，模型: {}, 端点: {}, 最大迭代: {}", modelName, baseUrl, maxIters);
        return agent;
    }
}
