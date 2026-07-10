import { useState, useRef, useEffect } from "react";
import { Sparkles, X, Send, Loader2, Bot, User, Plus, History, Trash2, MessageSquare } from "lucide-react";
import ReactMarkdown from "react-markdown";
import remarkGfm from "remark-gfm";
import { cn } from "../../lib/cn";
import { formatTime, QUICK_QUESTIONS, TOOL_LABELS, useAiChat } from "./useAiChat";

export function AiCopilot() {
  const [open, setOpen] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const {
    sessionId,
    sessions,
    messages,
    input,
    setInput,
    loading,
    currentTool,
    historyLoading,
    loadSessions,
    loadSessionMessages: loadMessages,
    startNewChat,
    deleteSession,
    sendMessage: send,
    stop,
  } = useAiChat();
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: "smooth" });
  }, [messages]);

  useEffect(() => {
    if (open) {
      const timer = setTimeout(() => inputRef.current?.focus(), 100);
      void loadSessions();
      return () => clearTimeout(timer);
    }
  }, [loadSessions, open]);

  async function loadSessionMessages(sid: string) {
    setShowHistory(false);
    await loadMessages(sid);
  }

  function handleNewChat() {
    startNewChat();
    setShowHistory(false);
    setTimeout(() => inputRef.current?.focus(), 100);
  }

  function handleDeleteSession(sid: string, event: React.MouseEvent) {
    event.stopPropagation();
    void deleteSession(sid);
  }

  function sendMessage(text: string) {
    setShowHistory(false);
    send(text);
  }

  function handleKeyDown(e: React.KeyboardEvent) {
    if (e.key === "Enter" && !e.shiftKey) {
      e.preventDefault();
      sendMessage(input);
    }
  }

  if (!open) {
    return (
      <button
        type="button"
        onClick={() => setOpen(true)}
        className="fixed bottom-6 right-6 z-50 flex h-14 w-14 items-center justify-center rounded-full bg-blue-600 text-white shadow-lg transition hover:bg-blue-700 hover:shadow-xl"
        title="AI 运营助手"
      >
        <Sparkles className="h-6 w-6" />
      </button>
    );
  }

  return (
    <div className="fixed inset-y-0 right-0 z-50 flex w-full max-w-md flex-col border-l border-slate-200 bg-white shadow-2xl">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
        <div className="flex items-center gap-2">
          <Sparkles className="h-5 w-5 text-blue-600" />
          <span className="text-sm font-semibold text-slate-900">AI 运营助手</span>
        </div>
        <div className="flex items-center gap-1">
          <button
            type="button"
            onClick={handleNewChat}
            className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-blue-600"
            title="新对话"
          >
            <Plus className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => {
              setShowHistory((v) => !v);
              if (!showHistory) loadSessions();
            }}
            className={cn(
              "rounded-md p-1.5 transition",
              showHistory
                ? "bg-blue-50 text-blue-600"
                : "text-slate-400 hover:bg-slate-100 hover:text-slate-600",
            )}
            title="历史对话"
          >
            <History className="h-4 w-4" />
          </button>
          <button
            type="button"
            onClick={() => setOpen(false)}
            className="rounded-md p-1.5 text-slate-400 hover:bg-slate-100 hover:text-slate-600"
          >
            <X className="h-4 w-4" />
          </button>
        </div>
      </div>

      {/* Content Area */}
      <div className="flex-1 overflow-y-auto px-4 py-4">
        {/* History View */}
        {showHistory && (
          <div className="space-y-2">
            <p className="mb-3 text-xs font-medium text-slate-400">历史对话</p>
            {sessions.length === 0 ? (
              <div className="flex flex-col items-center justify-center py-12 text-slate-400">
                <MessageSquare className="mb-2 h-8 w-8" />
                <p className="text-sm">暂无历史对话</p>
              </div>
            ) : (
              sessions.map((s) => (
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
              ))
            )}
          </div>
        )}

        {/* Loading state when switching sessions */}
        {historyLoading && (
          <div className="flex h-full items-center justify-center">
            <Loader2 className="h-6 w-6 animate-spin text-blue-500" />
          </div>
        )}

        {/* Welcome State (no messages, not showing history) */}
        {!showHistory && !historyLoading && messages.length === 0 && (
          <div className="flex h-full flex-col items-center justify-center gap-4">
            <div className="rounded-full bg-blue-50 p-4">
              <Bot className="h-8 w-8 text-blue-500" />
            </div>
            <p className="text-center text-sm text-slate-500">
              我是你的短链运营分析助手，可以帮你分析流量数据、检测异常、检查链接健康。
            </p>
            <div className="flex flex-wrap justify-center gap-2">
              {QUICK_QUESTIONS.map((q) => (
                <button
                  key={q}
                  type="button"
                  onClick={() => sendMessage(q)}
                  className="rounded-full border border-slate-200 px-3 py-1.5 text-xs text-slate-600 transition hover:border-blue-300 hover:bg-blue-50 hover:text-blue-700"
                >
                  {q}
                </button>
              ))}
            </div>
            {/* Recent sessions on welcome screen */}
            {sessions.length > 0 && (
              <div className="mt-6 w-full">
                <p className="mb-2 text-xs font-medium text-slate-400">最近对话</p>
                <div className="space-y-1">
                  {sessions.slice(0, 5).map((s) => (
                    <button
                      key={s.sessionId}
                      type="button"
                      onClick={() => loadSessionMessages(s.sessionId)}
                      className="flex w-full items-center gap-2 rounded-lg px-3 py-2 text-left transition hover:bg-slate-50"
                    >
                      <MessageSquare className="h-3.5 w-3.5 flex-shrink-0 text-slate-400" />
                      <span className="flex-1 truncate text-sm text-slate-600">{s.title}</span>
                      <span className="flex-shrink-0 text-xs text-slate-400">
                        {formatTime(s.updateTime || s.createTime)}
                      </span>
                    </button>
                  ))}
                </div>
              </div>
            )}
          </div>
        )}

        {/* Chat Messages */}
        {!showHistory && !historyLoading &&
          messages.map((msg, i) => (
            <div
              key={i}
              className={cn("mb-4 flex gap-3", msg.role === "user" ? "justify-end" : "justify-start")}
            >
              {msg.role === "assistant" && (
                <div className="mt-0.5 flex-shrink-0 rounded-full bg-blue-50 p-1.5">
                  <Bot className="h-3.5 w-3.5 text-blue-500" />
                </div>
              )}
              <div
                className={cn(
                  "max-w-[85%] rounded-xl px-3.5 py-2.5 text-sm leading-relaxed",
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
                {/* Message content */}
                <div className={cn("break-words text-sm", msg.role === "user" ? "" : "ai-markdown")}>
                  {msg.role === "assistant" && msg.content ? (
                    i === messages.length - 1 && loading ? (
                      /* 流式阶段：纯文本渲染，避免 react-markdown 增量解析异常 */
                      <span className="whitespace-pre-wrap">{msg.content}</span>
                    ) : (
                      /* 流式结束后 / 历史消息：ReactMarkdown 完整渲染 */
                      <ReactMarkdown
                        remarkPlugins={[remarkGfm]}
                        components={{
                          pre: ({ children, node: _n, ...rest }) => (
                            <pre className="my-2 overflow-x-auto rounded bg-slate-800 p-2 text-xs text-slate-100" {...rest}>{children}</pre>
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
                    )
                  ) : (
                    <span className="whitespace-pre-wrap">{msg.content}</span>
                  )}
                  {msg.role === "assistant" && loading && i === messages.length - 1 && !msg.content && (
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
                  )}
                </div>
              </div>
              {msg.role === "user" && (
                <div className="mt-0.5 flex-shrink-0 rounded-full bg-slate-200 p-1.5">
                  <User className="h-3.5 w-3.5 text-slate-600" />
                </div>
              )}
            </div>
          ))}
        {!showHistory && !historyLoading && <div ref={messagesEndRef} />}
      </div>

      {/* Input */}
      <div className="border-t border-slate-200 px-4 py-3">
        <div className="flex items-center gap-2">
          <input
            ref={inputRef}
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyDown={handleKeyDown}
            placeholder={loading ? "AI 正在分析中..." : "输入你的问题..."}
            disabled={loading || showHistory}
            className="flex-1 rounded-lg border border-slate-200 px-3 py-2 text-sm outline-none transition placeholder:text-slate-400 focus:border-blue-400 focus:ring-1 focus:ring-blue-400 disabled:bg-slate-50"
          />
          {loading ? (
            <button
              type="button"
              onClick={stop}
              className="flex h-9 w-9 items-center justify-center rounded-lg bg-slate-200 text-slate-500 transition hover:bg-slate-300"
              title="停止生成"
            >
              <X className="h-4 w-4" />
            </button>
          ) : (
            <button
              type="button"
              onClick={() => sendMessage(input)}
              disabled={!input.trim() || showHistory}
              className="flex h-9 w-9 items-center justify-center rounded-lg bg-blue-600 text-white transition hover:bg-blue-700 disabled:bg-slate-200 disabled:text-slate-400"
              title="发送"
            >
              <Send className="h-4 w-4" />
            </button>
          )}
        </div>
      </div>
    </div>
  );
}
