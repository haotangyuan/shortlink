import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { setSessionToken } from "./client";
import { streamAiChat } from "./ai";

describe("streamAiChat", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn(() => new Promise<Response>(() => undefined)));
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("sends messages in a POST body instead of the URL", () => {
    setSessionToken("session-123");

    streamAiChat("sensitive message", "session-1", {
      onText: vi.fn(),
      onToolCall: vi.fn(),
      onError: vi.fn(),
      onDone: vi.fn(),
    });

    const [url, init] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe("/api/short-link/admin/v1/ai/chat/stream");
    expect(init?.method).toBe("POST");
    expect(init?.body).toBe(JSON.stringify({ message: "sensitive message", sessionId: "session-1" }));
    const headers = new Headers(init?.headers);
    expect(headers.get("Authorization")).toBe("Bearer session-123");
    expect(headers.get("Content-Type")).toBe("application/json");
  });

  it("parses CRLF events and a final event without a trailing blank line", async () => {
    const encoder = new TextEncoder();
    const stream = new ReadableStream({
      start(controller) {
        controller.enqueue(encoder.encode('event: text\r\ndata: "hello"\r\n\r\n'));
        controller.enqueue(encoder.encode('event: done\r\ndata: "[DONE]"'));
        controller.close();
      },
    });
    vi.mocked(fetch).mockResolvedValue(new Response(stream, { status: 200 }));
    const onText = vi.fn();
    const onDone = vi.fn();

    streamAiChat("message", "session-1", {
      onText,
      onToolCall: vi.fn(),
      onError: vi.fn(),
      onDone,
    });
    await vi.waitFor(() => expect(onDone).toHaveBeenCalledOnce());

    expect(onText).toHaveBeenCalledWith("hello");
  });

  it("clears authentication when the stream endpoint returns 401", async () => {
    setSessionToken("expired-token");
    localStorage.setItem("shortlink.username", "alice");
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: "A000200", message: "用户身份验证失败" }), {
        status: 401,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const onError = vi.fn();

    streamAiChat("message", "session-1", {
      onText: vi.fn(),
      onToolCall: vi.fn(),
      onError,
      onDone: vi.fn(),
    });
    await vi.waitFor(() => expect(onError).toHaveBeenCalledWith("用户身份验证失败"));

    expect(localStorage.getItem("shortlink.sessionToken")).toBeNull();
    expect(localStorage.getItem("shortlink.username")).toBeNull();
  });
});
