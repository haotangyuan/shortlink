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
});
