import { clearSessionAfterUnauthorized, getSessionToken } from "./client";
import type { ApiResult } from "./types";

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
  const url = "/api/short-link/admin/v1/ai/chat/stream";

  fetch(url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
    body: JSON.stringify({ message, sessionId }),
    signal: controller.signal,
  })
    .then(async (response) => {
      if (!response.ok) {
        if (response.status === 401) clearSessionAfterUnauthorized();
        const payload = (await response.json().catch(() => null)) as ApiResult<unknown> | null;
        callbacks.onError(payload?.message || `请求失败: HTTP ${response.status}`);
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
      let terminated = false;

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
            if (!terminated) {
              terminated = true;
              callbacks.onError(parsed);
            }
            break;
          case "done":
            if (!terminated) {
              terminated = true;
              callbacks.onDone();
            }
            break;
          case "session_id":
            callbacks.onSessionId?.(parsed);
            break;
        }
      }

      function processLine(rawLine: string) {
        const line = rawLine.endsWith("\r") ? rawLine.slice(0, -1) : rawLine;
        if (line === "") {
          dispatchEvent();
          currentEvent = "text";
          return;
        }
        if (line.startsWith("event:")) {
          currentEvent = line.slice(6).trim();
          return;
        }
        if (line.startsWith("data:")) {
          const rawData = line.slice(5);
          dataBuffer.push(rawData.startsWith(" ") ? rawData.slice(1) : rawData);
        }
      }

      while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split("\n");
        buffer = lines.pop() ?? "";

        lines.forEach(processLine);
      }

      buffer += decoder.decode();
      if (buffer) processLine(buffer);
      dispatchEvent();
      if (!terminated) {
        terminated = true;
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
