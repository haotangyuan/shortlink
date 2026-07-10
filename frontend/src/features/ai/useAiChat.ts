import { useCallback, useEffect, useRef, useState } from "react";
import { streamAiChat } from "../../api/ai";
import { adminApi } from "../../api/admin";
import type { AiSession } from "../../api/types";

export interface ChatMessage {
  role: "user" | "assistant" | "system";
  content: string;
  toolCalls?: string[];
}

export const QUICK_QUESTIONS = [
  "本周流量趋势分析",
  "哪些链接表现最好？",
  "有没有异常流量？",
  "检查失效链接",
];

export const TOOL_LABELS: Record<string, string> = {
  get_link_stats: "查询短链统计数据",
  get_group_stats: "查询分组统计数据",
  compare_links: "对比短链表现",
  list_groups: "获取分组列表",
  detect_anomalies: "检测流量异常",
  get_link_health: "检查链接健康状态",
};

export function formatTime(dateString: string): string {
  const date = new Date(dateString);
  const difference = Date.now() - date.getTime();
  const minutes = Math.floor(difference / 60_000);
  const hours = Math.floor(difference / 3_600_000);
  const days = Math.floor(difference / 86_400_000);
  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes} 分钟前`;
  if (hours < 24) return `${hours} 小时前`;
  if (days < 7) return `${days} 天前`;
  return `${date.getMonth() + 1}/${date.getDate()}`;
}

export function useAiChat(refreshDelayMs = 0) {
  const [sessionId, setSessionId] = useState<string>(() => crypto.randomUUID());
  const [sessions, setSessions] = useState<AiSession[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [currentTool, setCurrentTool] = useState<string | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [sessionsLoading, setSessionsLoading] = useState(false);
  const abortRef = useRef<AbortController | null>(null);
  const refreshTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const stop = useCallback(() => {
    abortRef.current?.abort();
    abortRef.current = null;
    setLoading(false);
    setCurrentTool(null);
  }, []);

  useEffect(() => () => {
    abortRef.current?.abort();
    if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
  }, []);

  const loadSessions = useCallback(async () => {
    setSessionsLoading(true);
    try {
      setSessions((await adminApi.getAiSessions()) ?? []);
    } catch {
      setSessions([]);
    } finally {
      setSessionsLoading(false);
    }
  }, []);

  const loadSessionMessages = useCallback(async (nextSessionId: string) => {
    stop();
    setHistoryLoading(true);
    try {
      const data = await adminApi.getAiSessionMessages(nextSessionId);
      setMessages((data ?? []).map((message) => ({
        role: message.role,
        content: message.content,
      })));
      setSessionId(nextSessionId);
    } catch {
      setMessages([]);
    } finally {
      setHistoryLoading(false);
    }
  }, [stop]);

  const startNewChat = useCallback(() => {
    stop();
    setSessionId(crypto.randomUUID());
    setMessages([]);
    setInput("");
  }, [stop]);

  const deleteSession = useCallback(async (deletedSessionId: string) => {
    try {
      await adminApi.deleteAiSession(deletedSessionId);
      setSessions((current) => current.filter((session) => session.sessionId !== deletedSessionId));
      if (deletedSessionId === sessionId) startNewChat();
    } catch {
      return;
    }
  }, [sessionId, startNewChat]);

  const sendMessage = useCallback((value: string) => {
    const message = value.trim();
    if (!message || loading) return;

    setMessages((current) => [
      ...current,
      { role: "user", content: message },
      { role: "assistant", content: "", toolCalls: [] },
    ]);
    setInput("");
    setLoading(true);
    setCurrentTool(null);

    abortRef.current = streamAiChat(message, sessionId, {
      onText: (delta) => {
        setMessages((current) => {
          const updated = [...current];
          const last = updated.at(-1);
          if (last?.role === "assistant") {
            updated[updated.length - 1] = { ...last, content: last.content + delta };
          }
          return updated;
        });
      },
      onToolCall: (toolName) => {
        setCurrentTool(toolName);
        setMessages((current) => {
          const updated = [...current];
          const last = updated.at(-1);
          if (last?.role === "assistant") {
            updated[updated.length - 1] = {
              ...last,
              toolCalls: [...(last.toolCalls ?? []), toolName],
            };
          }
          return updated;
        });
      },
      onError: (error) => {
        abortRef.current = null;
        setCurrentTool(null);
        setLoading(false);
        setMessages((current) => {
          const updated = [...current];
          const last = updated.at(-1);
          if (last?.role === "assistant" && !last.content) {
            updated[updated.length - 1] = { ...last, content: `⚠️ ${error}` };
          }
          return updated;
        });
      },
      onDone: () => {
        abortRef.current = null;
        setCurrentTool(null);
        setLoading(false);
        if (refreshTimerRef.current) clearTimeout(refreshTimerRef.current);
        refreshTimerRef.current = setTimeout(() => void loadSessions(), refreshDelayMs);
      },
      onSessionId: setSessionId,
    });
  }, [loadSessions, loading, refreshDelayMs, sessionId]);

  return {
    sessionId,
    sessions,
    messages,
    input,
    setInput,
    loading,
    currentTool,
    historyLoading,
    sessionsLoading,
    loadSessions,
    loadSessionMessages,
    startNewChat,
    deleteSession,
    sendMessage,
    stop,
  };
}
