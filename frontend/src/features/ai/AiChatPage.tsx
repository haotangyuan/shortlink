import { useState, useRef, useEffect, useCallback } from "react";
import { Send, Loader2, Bot, User, Plus, Trash2, MessageSquare, Sparkles, X } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { streamAiChat } from "../../api/ai";
import { adminApi } from "../../api/admin";
import type { AiSession } from "../../api/types";
import { cn } from "../../lib/cn";

interface ChatMessage {
  role: "user" | "assistant" | "system";
  content: string;
  toolCalls?: string[];
}

const QUICK_QUESTIONS = [
  "本周流量趋势分析",
  "哪些链接表现最好？",
  "有没有异常流量？",
  "检查失效链接",
];

const TOOL_LABELS: Record<string, string> = {
  get_link_stats: "查询短链统计数据",
  get_group_stats: "查询分组统计数据",
  compare_links: "对比短链表现",
  list_groups: "获取分组列表",
  detect_anomalies: "检测流量异常",
  get_link_health: "检查链接健康状态",
};

function formatTime(dateStr: string): string {
  const d = new Date(dateStr);
  const now = new Date();
  const diff = now.getTime() - d.getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);
  if (minutes < 1) return "刚刚";
  if (minutes < 60) return `${minutes} 分钟前`;
  if (hours < 24) return `${hours} 小时前`;
  if (days < 7) return `${days} 天前`;
  return `${d.getMonth() + 1}/${d.getDate()}`;
}

export function AiChatPage() {
  const [sessionId, setSessionId] = useState(() => crypto.randomUUID());
  const [sessions, setSessions] = useState<AiSession[]>([]);
  const [messages, setMessages] = useState<ChatMessage[]>([]);
  const [input, setInput] = useState("");
  const [loading, setLoading] = useState(false);
  const [currentTool, setCurrentTool] = useState<string | null>(null);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [sessionsLoading, setSessionsLoading] = useState(true);
  const abortRef = useRef<AbortController | null>(null);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  async function loadSessions() {
    setSessionsLoading(true);
    try {
      const data = await adminApi.getAiSessions();
      setSessions(data ?? []);
      // 如果首次返回空列表，1 秒后自动重试一次
      if (!data || data.length === 0) {
        setTimeout(async () => {
          try {
            const retry = await adminApi.getAiSessions();
            setSessions(retry ?? []);
          } catch (e) {
            console.error("重试加载历史会话失败:", e);
          }
        }, 1000);
      }
    } catch (e) {
      console.error("加载历史会话失败:", e);
      setSessions([]);
    } finally {
      setSessionsLoading(false);
    }
  }

  useEffect(() => {
    // 等待 150ms 确保 ProtectedRoute 的 auth 校验完成后再发起请求
    const timer = setTimeout(() => {
      loadSessions();
    }, 150);
    inputRef.current?.focus();
    return () => clearTimeout(timer);
  }, []);

  async function loadSessionMessages(sid: string) {
    setHistoryLoading(true);
    try {
      const data = await adminApi.getAiSessionMessages(sid);
      const loaded: ChatMessage[] = (data ?? []).map((m) => ({
        role: m.role as "user" | "assistant",
        content: m.content,
      }));
      setMessages(loaded);
      setSessionId(sid as `${string}-${string}-${string}-${string}-${string}`);
    } catch {
      setMessages([]);
    } finally {
      setHistoryLoading(false);
    }
  }

  function handleNewChat() {
    setSessionId(crypto.randomUUID());
    setMessages([]);
    setInput("");
    setTimeout(() => inputRef.current?.focus(), 100);
  }

  async function handleDeleteSession(sid: string, e: React.MouseEvent) {
    e.stopPropagation();
    try {
      await adminApi.deleteAiSession(sid);
      setSessions((prev) => prev.filter((s) => s.sessionId !== sid));
      if (sid === sessionId) {
        handleNewChat();
      }
    } catch {
      // 静默处理
    }
  }

  const sendMessage = useCallback(
    (text: string) => {
      if (!text.trim() || loading) return;

      const userMsg: ChatMessage = { role: "user", content: text.trim() };
      const assistantMsg: ChatMessage = { role: "assistant", content: "", toolCalls: [] };

      setMessages((prev) => [...prev, userMsg, assistantMsg]);
      setInput("");
      setLoading(true);
      setCurrentTool(null);

      const controller = streamAiChat(text.trim(), sessionId, {
        onText: (delta) => {
          setMessages((prev) => {
            const updated = [...prev];
            const last = updated[updated.length - 1];
            if (last.role === "assistant") {
              updated[updated.length - 1] = { ...last, content: last.content + delta };
            }
            return updated;
          });
        },
        onToolCall: (toolName) => {
          setCurrentTool(toolName);
          setMessages((prev) => {
            const updated = [...prev];
            const last = updated[updated.length - 1];
            if (last.role === "assistant") {
              updated[updated.length - 1] = {
                ...last,
                toolCalls: [...(last.toolCalls ?? []), toolName],
              };
            }
            return updated;
          });
        },
        onError: (error) => {
          setCurrentTool(null);
          setLoading(false);
          setMessages((prev) => {
            const updated = [...prev];
            const last = updated[updated.length - 1];
            if (last.role === "assistant" && !last.content) {
              updated[updated.length - 1] = { ...last, content: `⚠️ ${error}` };
            }
            return updated;
          });
        },
        onDone: () => {
          setCurrentTool(null);
          setLoading(false);
          // 短暂延迟确保后端完全提交会话数据后再刷新列表
          setTimeout(() => loadSessions(), 300);
        },
      });

      abortRef.current = controller;
    },
    [loading, sessionId],
  );

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  }

  function handleStop() {
    abortRef.current?.abort();
    setLoading(false);
    setCurrentTool(null);
  }

  return (
    <div className="flex h-[calc(100vh-8rem)] gap-4">
      {/* 左侧：会话历史 */}
      <div className="flex w-72 flex-shrink-0 flex-col rounded-xl border border-slate-200 bg-white">
        <div className="flex items-center justify-between border-b border-slate-100 px-4 py-3">
          <span className="text-sm font-semibold text-slate-700">历史对话</span>
          <button
            type="button"
            onClick={handleNewChat}
            className="flex items-center gap-1 rounded-lg bg-blue-600 px-3 py-1.5 text-xs font-medium text-white transition hover:bg-blue-700"
          >
            <Plus className="h-3.5 w-3.5" />
            新对话
          </button>
        </div>
        <div className="flex-1 overflow-y-auto px-2 py-2">
          {sessionsLoading ? (
            <div className="flex flex-col items-center justify-center py-12 text-slate-400">
              <Loader2 className="mb-2 h-5 w-5 animate-spin" />
              <p className="text-xs">加载中...</p>
            </div>
          ) : sessions.length === 0 ? (
            <div className="flex flex-col items-center justify-center py-12 text-slate-400">
              <MessageSquare className="mb-2 h-8 w-8" />
              <p className="text-sm">暂无历史对话</p>
            </div>
          ) : (
            <div className="space-y-1">
              {sessions.map((s) => (
                <button
                  key={s.sessionId}
                  type="button"
                  onClick={() => loadSessionMessages(s.sessionId)}
                  className={cn(
                    "group flex w-full items-center gap-3 rounded-lg px-3 py-2.5 text-left transition",
                    s.sessionId === sessionId
                      ? "bg-blue-50 ring-1 ring-blue-200"
                      : "hover:bg-slate-50",
                  )}
                >
                  <MessageSquare
                    className={cn(
                      "h-4 w-4 flex-shrink-0",
                      s.sessionId === sessionId ? "text-blue-500" : "text-slate-400",
                    )}
                  />
                  <div className="min-w-0 flex-1">
                    <p
                      className={cn(
                        "truncate text-sm",
                        s.sessionId === sessionId
                          ? "font-medium text-blue-700"
                          : "text-slate-700",
                      )}
                    >
                      {s.title}
                    </p>
                    <p className="text-xs text-slate-400">{formatTime(s.updateTime || s.createTime)}</p>
                  </div>
                  <span
                    onClick={(e) => handleDeleteSession(s.sessionId, e)}
                    className="flex-shrink-0 rounded p-1 text-slate-300 opacity-0 transition hover:bg-red-50 hover:text-red-500 group-hover:opacity-100"
                    title="删除对话"
                  >
                    <Trash2 className="h-3.5 w-3.5" />
                  </span>
                </button>
              ))}
            </div>
          )}
        </div>
      </div>

      {/* 右侧：聊天区域 */}
      <div className="flex min-w-0 flex-1 flex-col rounded-xl border border-slate-200 bg-white">
        {/* 聊天头部 */}
        <div className="flex items-center gap-2 border-b border-slate-100 px-5 py-3">
          <Sparkles className="h-5 w-5 text-blue-600" />
          <span className="text-sm font-semibold text-slate-900">AI 运营助手</span>
          <span className="text-xs text-slate-400">— 短链运营分析助手</span>
        </div>

        {/* 消息区域 */}
        <div className="flex-1 overflow-y-auto px-5 py-4">
          {historyLoading ? (
            <div className="flex h-full items-center justify-center">
              <Loader2 className="h-6 w-6 animate-spin text-blue-500" />
            </div>
          ) : messages.length === 0 ? (
            /* 欢迎状态 */
            <div className="flex h-full flex-col items-center justify-center gap-5">
              <div className="rounded-full bg-blue-50 p-5">
                <Bot className="h-10 w-10 text-blue-500" />
              </div>
              <div className="text-center">
                <p className="text-base font-medium text-slate-700">你好，我是你的短链运营分析助手</p>
                <p className="mt-1 text-sm text-slate-500">
                  可以帮你分析流量数据、检测异常、检查链接健康
                </p>
              </div>
              <div className="flex flex-wrap justify-center gap-2">
                {QUICK_QUESTIONS.map((q) => (
                  <button
                    key={q}
                    type="button"
                    onClick={() => sendMessage(q)}
                    className="rounded-full border border-slate-200 px-4 py-2 text-sm text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                  >
                    {q}
                  </button>
                ))}
              </div>
            </div>
          ) : (
            /* 消息列表 */
            messages.map((msg, i) => (
              <div
                key={i}
                className={cn("mb-5 flex gap-3", msg.role === "user" ? "justify-end" : "justify-start")}
              >
                {msg.role === "assistant" && (
                  <div className="mt-0.5 flex-shrink-0 rounded-full bg-blue-50 p-2">
                    <Bot className="h-4 w-4 text-blue-500" />
                  </div>
                )}
                <div
                  className={cn(
                    "max-w-[80%] rounded-xl px-4 py-3 text-sm leading-relaxed",
                    msg.role === "user"
                      ? "bg-blue-600 text-white"
                      : "bg-slate-50 text-slate-700",
                  )}
                >
                  {/* Tool call badges */}
                  {msg.toolCalls && msg.toolCalls.length > 0 && (
                    <div className="mb-2 flex flex-wrap gap-1">
                      {msg.toolCalls.map((tool, j) => (
                        <span
                          key={j}
                          className="inline-flex items-center gap-1 rounded-md bg-amber-50 px-2 py-0.5 text-[11px] text-amber-700"
                        >
                          <Loader2 className="h-2.5 w-2.5" />
                          {TOOL_LABELS[tool] ?? tool}
                        </span>
                      ))}
                    </div>
                  )}

                  {/* 消息内容 */}
                  <div className={cn("break-words text-sm", msg.role === "user" ? "" : "ai-markdown")}>
                    {msg.role === "assistant" && msg.content ? (
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={{
                          pre: ({ children, node: _n, ...rest }) => (
                            <pre className="my-2 overflow-x-auto rounded bg-slate-800 p-3 text-xs text-slate-100" {...rest}>{children}</pre>
                          ),
                          table: ({ children, node: _n, ...rest }) => (
                            <div className="my-2 overflow-x-auto">
                              <table className="w-full border-collapse border border-slate-200 text-xs" {...rest}>{children}</table>
                            </div>
                          ),
                          thead: ({ children, node: _n, ...rest }) => (
                            <thead className="bg-slate-100" {...rest}>{children}</thead>
                          ),
                          th: ({ children, node: _n, ...rest }) => (
                            <th className="border border-slate-200 px-2 py-1 text-left font-semibold text-slate-700" {...rest}>{children}</th>
                          ),
                          td: ({ children, node: _n, ...rest }) => (
                            <td className="border border-slate-200 px-2 py-1 text-slate-600" {...rest}>{children}</td>
                          ),
                          p: ({ children, node: _n, ...rest }) => (
                            <p className="mb-1.5 last:mb-0" {...rest}>{children}</p>
                          ),
                          strong: ({ children, node: _n, ...rest }) => (
                            <strong className="font-semibold text-slate-900" {...rest}>{children}</strong>
                          ),
                          ul: ({ children, node: _n, ...rest }) => (
                            <ul className="mb-1.5 ml-4 list-disc space-y-0.5" {...rest}>{children}</ul>
                          ),
                          ol: ({ children, node: _n, ...rest }) => (
                            <ol className="mb-1.5 ml-4 list-decimal space-y-0.5" {...rest}>{children}</ol>
                          ),
                          code: ({ children, className, node: _n, ...rest }) => (
                            <code className={cn("rounded bg-slate-200 px-1 py-0.5 text-xs text-slate-800", className)} {...rest}>{children}</code>
                          ),
                        }}
                      >
                        {msg.content}
                      </ReactMarkdown>
                    ) : msg.role === "assistant" && loading && i === messages.length - 1 ? (
                      <span className="inline-flex items-center gap-1 text-slate-400">
                        {currentTool ? (
                          <>
                            <Loader2 className="h-3 w-3 animate-spin" />
                            正在{TOOL_LABELS[currentTool] ?? "查询数据"}...
                          </>
                        ) : (
                          <>
                            <Loader2 className="h-3 w-3 animate-spin" />
                            正在思考...
                          </>
                        )}
                      </span>
                    ) : (
                      <span className="whitespace-pre-wrap">{msg.content}</span>
                    )}
                  </div>
                </div>
                {msg.role === "user" && (
                  <div className="mt-0.5 flex-shrink-0 rounded-full bg-slate-200 p-2">
                    <User className="h-4 w-4 text-slate-600" />
                  </div>
                )}
              </div>
            ))
          )}
          <div ref={messagesEndRef} />
        </div>

        {/* 输入区域 */}
        <div className="border-t border-slate-100 px-5 py-4">
          <div className="flex items-center gap-3">
            <input
              ref={inputRef}
              type="text"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder={loading ? "AI 正在分析中..." : "输入你的问题..."}
              disabled={loading || historyLoading}
              className="flex-1 rounded-lg border border-slate-200 px-4 py-2.5 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-1 focus:ring-blue-400 disabled:bg-slate-50"
            />
            {loading ? (
              <button
                type="button"
                onClick={handleStop}
                className="flex h-10 w-10 items-center justify-center rounded-lg bg-slate-200 text-slate-500 transition hover:bg-slate-300"
                title="停止生成"
              >
                <X className="h-4 w-4" />
              </button>
            ) : (
              <button
                type="button"
                onClick={() => sendMessage(input)}
                disabled={!input.trim() || historyLoading}
                className="flex h-10 w-10 items-center justify-center rounded-lg bg-blue-600 text-white transition hover:bg-blue-700 disabled:bg-slate-200 disabled:text-slate-400"
                title="发送"
              >
                <Send className="h-4 w-4" />
              </button>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}
