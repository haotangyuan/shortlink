import { getSessionToken } from "./client";

export interface AiStreamCallbacks {
  onText: (delta: string) => void;
  onToolCall: (toolName: string) => void;
  onError: (error: string) => void;
  onDone: () => void;
  onSessionId?: (sessionId: string) => void;
}

/**
 * 通过 SSE 流式调用 AI Chat 接口
 * 返回 AbortController，可用于取消请求
 */
export function streamAiChat(
  message: string,
  sessionId: string,
  callbacks: AiStreamCallbacks,
): AbortController {
  const controller = new AbortController();
  const token = getSessionToken();
  const url = `/api/short-link/admin/v1/ai/chat/stream?message=${encodeURIComponent(message)}&sessionId=${encodeURIComponent(sessionId)}`;

  fetch(url, {
    headers: token ? { Authorization: `Bearer ${token}` } : {},
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        callbacks.onError(`请求失败: HTTP ${response.status}`);
        return;
      }

      const reader = response.body?.getReader();
      if (!reader) {
        callbacks.onError("无法读取响应流");
        return;
      }

      const decoder = new TextDecoder();
      let buffer = "";
      let currentEvent = "text"; // SSE default event name when no event: field
      let dataBuffer: string[] = []; // Buffer for multi-line data fields (SSE spec)
      let doneCalled = false; // 防止 onDone 被重复调用

      // Dispatch the accumulated data buffer as a complete event
      function dispatchEvent() {
        if (dataBuffer.length === 0) return;
        const joinedData = dataBuffer.join("\n");
        dataBuffer = [];
        // JSON 解码数据（后端用 objectMapper.writeValueAsString 编码，保留换行等特殊字符）
        let parsed: string;
        try {
          parsed = JSON.parse(joinedData);
        } catch {
          parsed = joinedData;
        }
        switch (currentEvent) {
          case "text":
            callbacks.onText(parsed);
            break;
          case "tool_call":
            callbacks.onToolCall(parsed);
            break;
          case "error":
            callbacks.onError(parsed);
            break;
          case "done":
            if (!doneCalled) {
              doneCalled = true;
              callbacks.onDone();
            }
            break;
          case "session_id":
            callbacks.onSessionId?.(parsed);
            break;
        }
      }

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() ?? "";

        for (const line of lines) {
          if (line === "") {
            // Empty line = end of SSE event: dispatch buffered data, then reset
            dispatchEvent();
            currentEvent = "text";
            continue;
          }
          if (line.startsWith("event:")) {
            currentEvent = line.slice(6).trim();
            continue;
          }
          if (line.startsWith("data:")) {
            // SSE spec: strip only the first space after "data:" if present
            const rawData = line.slice(5);
            const data = rawData.startsWith(" ") ? rawData.slice(1) : rawData;
            dataBuffer.push(data);
          }
        }
      }

      // Dispatch any remaining buffered data at end of stream
      dispatchEvent();
      if (!doneCalled) {
        doneCalled = true;
        callbacks.onDone();
      }
    })
    .catch((err) => {
      if (err.name !== "AbortError") {
        callbacks.onError(err.message || "网络请求异常");
      }
    });

  return controller;
}
