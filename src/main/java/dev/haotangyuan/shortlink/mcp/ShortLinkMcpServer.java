package dev.haotangyuan.shortlink.mcp;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.haotangyuan.shortlink.common.biz.user.UserContext;
import dev.haotangyuan.shortlink.common.constant.UserConstant;
import dev.haotangyuan.shortlink.dto.req.LinkCreateReqDTO;
import dev.haotangyuan.shortlink.dto.resp.LinkCreateRespDTO;
import dev.haotangyuan.shortlink.service.LinkService;
import io.modelcontextprotocol.server.McpServer;
import io.modelcontextprotocol.server.McpSyncServer;
import io.modelcontextprotocol.server.transport.WebMvcSseServerTransportProvider;
import io.modelcontextprotocol.spec.McpSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MCP 配置（基于官方 Java SDK 标准实现）
 * - 使用 WebMvcSseServerTransportProvider 暴露标准 SSE/消息端点
 * - 通过 McpServer 同步构建并注册工具
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class ShortLinkMcpServer {

    private static final String SSE_ENDPOINT = "/api/mcp";
    private static final String MESSAGE_ENDPOINT = "/api/mcp/message";

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public WebMvcSseServerTransportProvider transportProvider(ObjectMapper objectMapper) {
        // 使用 builder（替代已弃用构造函数），并开启 keepalive
        log.info("Creating MCP transport provider via builder. sse={}, message={}", SSE_ENDPOINT, MESSAGE_ENDPOINT);
        return WebMvcSseServerTransportProvider.builder()
                .objectMapper(objectMapper)
                .sseEndpoint(SSE_ENDPOINT)
                .messageEndpoint(MESSAGE_ENDPOINT)
                .keepAliveInterval(Duration.ofSeconds(20))
                .build();
    }

    // 将传输层 RouterFunction 注册到 Spring 容器
    @Bean
    public RouterFunction<ServerResponse> mcpRouter(WebMvcSseServerTransportProvider provider) {
        return provider.getRouterFunction();
    }

    // 构建并注册 MCP 服务器与工具
    @Bean
    public McpSyncServer mcpServer(WebMvcSseServerTransportProvider provider, LinkService linkService) {
        // 定义工具的输入 JSON Schema
        Map<String, Object> properties = new HashMap<>();
        properties.put("originUrl", Map.of(
                "type", "string",
                "description", "The original long URL to shorten"
        ));
        properties.put("describe", Map.of(
                "type", "string",
                "description", "Optional description for the link"
        ));
        McpSchema.JsonSchema inputSchema = new McpSchema.JsonSchema(
                "object",
                properties,
                List.of("originUrl"),
                false,
                null,
                null
        );

        McpSchema.Tool createShortLinkTool = McpSchema.Tool.builder()
                .name("createShortLink")
                .title("Create Short Link")
                .description("Create a short link from a long URL with 3-day validity period")
                .inputSchema(inputSchema)
                .build();

        // 仅启用 tools 能力
        McpSchema.ServerCapabilities caps = McpSchema.ServerCapabilities.builder()
                .tools(true)
                .build();

        // 构建同步服务器，注册工具调用处理器
        McpSyncServer server = McpServer.sync(provider)
                .serverInfo("ShortLink MCP Server", "1.0.0")
                .capabilities(caps)
                .requestTimeout(Duration.ofSeconds(30))
                .toolCall(createShortLinkTool, (exchange, callReq) -> {
                    Map<String, Object> args = callReq.arguments();
                    String originUrl = args == null ? null : (String) args.get("originUrl");
                    String describe = args == null ? null : (String) args.get("describe");

                    if (originUrl == null || originUrl.isBlank()) {
                        return new McpSchema.CallToolResult("originUrl is required", true);
                    }

                    try {
                        // 切换至 public 用户上下文
                        UserContext.setUsername(UserConstant.PUBLIC_USERNAME);

                        LinkCreateReqDTO linkRequest = new LinkCreateReqDTO();
                        linkRequest.setOriginUrl(originUrl);
                        linkRequest.setDescribe(describe);
                        linkRequest.setCreatedType(0);
                        // 默认 3 天有效
                        java.util.Date validDate = cn.hutool.core.date.DateUtil.offsetDay(new java.util.Date(), 3);
                        linkRequest.setValidDate(validDate);

                        LinkCreateRespDTO linkResponse = linkService.createLink(linkRequest);

                        String text = String.format(
                                "Short link created successfully!\n" +
                                        "Original URL: %s\n" +
                                        "Short URL: %s\n" +
                                        "Description: %s\n" +
                                        "Valid until: %s",
                                originUrl,
                                linkResponse.getFullShortUrl(),
                                describe != null ? describe : "No description",
                                cn.hutool.core.date.DateUtil.formatDateTime(validDate)
                        );

                        return new McpSchema.CallToolResult(List.of(new McpSchema.TextContent(text)), false);
                    } catch (Exception e) {
                        log.error("MCP: Failed to create short link", e);
                        return new McpSchema.CallToolResult("Failed to create short link: " + e.getMessage(), true);
                    } finally {
                        UserContext.removeUser();
                    }
                })
                .build();

        log.info("MCP server initialized with tool: createShortLink");
        return server;
    }
}
