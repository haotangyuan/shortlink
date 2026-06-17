package dev.haotangyuan.shortlink.controller.admin;

import dev.haotangyuan.shortlink.common.convention.result.Result;
import dev.haotangyuan.shortlink.common.convention.result.Results;
import dev.haotangyuan.shortlink.service.AiSessionService;
import dev.haotangyuan.shortlink.vo.AiMessageVO;
import dev.haotangyuan.shortlink.vo.AiSessionVO;
import io.agentscope.core.ReActAgent;
import io.agentscope.core.message.Msg;
import io.agentscope.core.message.MsgRole;
import io.agentscope.core.message.UserMessage;
import io.agentscope.core.event.AgentEvent;
import io.agentscope.core.event.AgentEventType;
import io.agentscope.core.event.TextBlockDeltaEvent;
import io.agentscope.core.event.ToolCallStartEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * AI Copilot 对话接口
 * <p>
 * 提供 SSE 流式输出端点，将 AgentScope ReActAgent 的 streamEvents 事件流
 * 桥接到 Spring MVC 的 SseEmitter，实现前端实时展示 AI 分析和工具调用状态。
 * <p>
 * 支持多会话管理和多轮对话上下文：
 * - 每个会话通过 sessionId 标识（前端生成 UUID）
 * - 对话历史持久化到数据库，切换会话时可恢复
 * - 每次请求加载最近 20 条历史消息作为 LLM 上下文
 *
 * @author: haotangyuan
 */
@Slf4j
@RestController
@ConditionalOnProperty(prefix = "short-link.ai", name = "enabled", havingValue = "true")
@RequiredArgsConstructor
public class AiChatController {

    private final ReActAgent analyticsAgent;
    private final AiSessionService aiSessionService;
    private final ObjectMapper objectMapper;

    /** 每次发送给 LLM 的最大历史消息数 */
    private static final int MAX_HISTORY_MESSAGES = 20;

    /**
     * SSE 流式对话端点
     * <p>
     * 事件类型：
     * - event: session_id → 会话 ID 确认
     * - event: text       → AI 正在输出的文字片段
     * - event: tool_call  → AI 正在调用某个工具（工具名称）
     * - event: done       → 对话完成
     * - event: error      → 发生错误
     */
    @GetMapping(value = "/api/short-link/admin/v1/ai/chat/stream",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamChat(
            @RequestParam String message,
            @RequestParam(defaultValue = "default") String sessionId) {

        SseEmitter emitter = new SseEmitter(120_000L); // 2 分钟超时

        log.info("AI Chat 请求: sessionId={}, message={}", sessionId, message);

        // 在异步回调前捕获用户名（TransmittableThreadLocal 可能在回调线程中不可用）
        String username;
        try {
            username = dev.haotangyuan.shortlink.common.biz.user.UserContext.getUsername();
            if (username == null) {
                emitter.send(SseEmitter.event().name("error")
                        .data(objectMapper.writeValueAsString("用户未登录")));
                emitter.complete();
                return emitter;
            }
        } catch (Exception e) {
            try {
                emitter.send(SseEmitter.event().name("error")
                        .data(objectMapper.writeValueAsString("用户未登录")));
                emitter.complete();
            } catch (IOException ignored) {}
            return emitter;
        }

        // 1. 幂等创建会话
        aiSessionService.getOrCreateSession(sessionId);

        // 2. 检查是否为首条消息（用于后续更新标题）
        long msgCountBefore = aiSessionService.getMessageCount(sessionId);

        // 3. 保存用户消息
        aiSessionService.saveMessage(sessionId, "user", message);

        // 4. 加载历史消息，构建 LLM 上下文
        List<AiMessageVO> history = aiSessionService.getSessionMessages(sessionId);
        List<Msg> conversation = buildConversation(history);

        // 5. 发送 session_id 确认事件
        final String finalSessionId = sessionId;
        try {
            emitter.send(SseEmitter.event().name("session_id")
                    .data(objectMapper.writeValueAsString(finalSessionId)));
        } catch (IOException e) {
            emitter.completeWithError(e);
            return emitter;
        }

        // 6. 流式调用 Agent（带完整对话历史）
        StringBuilder assistantText = new StringBuilder();
        final String capturedUsername = username;

        analyticsAgent.streamEvents(conversation)
                .subscribe(
                        event -> {
                            try {
                                if (event.getType() == AgentEventType.TEXT_BLOCK_DELTA) {
                                    String delta = ((TextBlockDeltaEvent) event).getDelta();
                                    assistantText.append(delta);
                                    // JSON 编码 delta，将 \n 转义为 \\n，防止 SSE 协议将换行误解为事件边界
                                    emitter.send(SseEmitter.event()
                                            .name("text")
                                            .data(objectMapper.writeValueAsString(delta)));
                                } else if (event.getType() == AgentEventType.TOOL_CALL_START) {
                                    String toolName = ((ToolCallStartEvent) event).getToolCallName();
                                    emitter.send(SseEmitter.event()
                                            .name("tool_call")
                                            .data(objectMapper.writeValueAsString(toolName)));
                                }
                            } catch (IOException e) {
                                emitter.completeWithError(e);
                            }
                        },
                        error -> {
                            log.error("AI Chat 流式输出异常", error);
                            try {
                                emitter.send(SseEmitter.event()
                                        .name("error")
                                        .data(objectMapper.writeValueAsString("AI 处理出错: " + error.getMessage())));
                            } catch (IOException ignored) {
                            }
                            emitter.completeWithError(error);
                        },
                        () -> {
                            // 保存 assistant 回复
                            String fullText = assistantText.toString();
                            if (!fullText.isBlank()) {
                                try {
                                    aiSessionService.saveMessage(finalSessionId, "assistant", fullText);
                                } catch (Exception e) {
                                    log.error("保存 AI 回复失败", e);
                                }
                            }

                            // 如果是首条消息，更新会话标题
                            if (msgCountBefore == 0) {
                                String title = message.length() > 30
                                        ? message.substring(0, 30) + "..."
                                        : message;
                                try {
                                    aiSessionService.updateSessionTitle(finalSessionId, title);
                                } catch (Exception e) {
                                    log.error("更新会话标题失败", e);
                                }
                            }

                            try {
                                emitter.send(SseEmitter.event()
                                        .name("done")
                                        .data(objectMapper.writeValueAsString("[DONE]")));
                            } catch (IOException ignored) {
                            }
                            emitter.complete();
                            log.info("AI Chat 完成: sessionId={}", finalSessionId);
                        }
                );

        return emitter;
    }

    /**
     * 获取当前用户的会话列表
     */
    @GetMapping("/api/short-link/admin/v1/ai/sessions")
    public Result<List<AiSessionVO>> listSessions() {
        return Results.success(aiSessionService.listSessions());
    }

    /**
     * 获取指定会话的消息列表
     */
    @GetMapping("/api/short-link/admin/v1/ai/sessions/{sessionId}/messages")
    public Result<List<AiMessageVO>> getSessionMessages(@PathVariable String sessionId) {
        return Results.success(aiSessionService.getSessionMessages(sessionId));
    }

    /**
     * 删除会话及其消息
     */
    @DeleteMapping("/api/short-link/admin/v1/ai/sessions/{sessionId}")
    public Result<Void> deleteSession(@PathVariable String sessionId) {
        aiSessionService.deleteSession(sessionId);
        return Results.success();
    }

    /**
     * 从数据库消息记录构建 LLM 对话上下文
     * <p>
     * 取最近 MAX_HISTORY_MESSAGES 条消息，转换为 AgentScope Msg 列表
     */
    private List<Msg> buildConversation(List<AiMessageVO> history) {
        List<Msg> conversation = new ArrayList<>();

        // 只取最近 N 条消息
        int start = Math.max(0, history.size() - MAX_HISTORY_MESSAGES);
        for (int i = start; i < history.size(); i++) {
            AiMessageVO msg = history.get(i);
            if ("user".equals(msg.getRole())) {
                conversation.add(new UserMessage(msg.getContent()));
            } else if ("assistant".equals(msg.getRole())) {
                conversation.add(Msg.builder()
                        .role(MsgRole.ASSISTANT)
                        .textContent(msg.getContent())
                        .build());
            }
        }
        return conversation;
    }
}
