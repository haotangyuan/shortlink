import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiError, request, setSessionToken } from "./client";

describe("api request client", () => {
  beforeEach(() => {
    localStorage.clear();
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("unwraps successful result envelopes and attaches the session token", async () => {
    setSessionToken("session-123");
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: "0", data: { ok: true } }), { status: 200 }),
    );

    await expect(request<{ ok: boolean }>("/api/test")).resolves.toEqual({ ok: true });

    const [, init] = vi.mocked(fetch).mock.calls[0];
    expect(new Headers(init?.headers).get("Authorization")).toBe("Bearer session-123");
  });

  it("clears the stored session token when the backend returns 401", async () => {
    setSessionToken("expired-token");
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: "A000200", message: "unauthorized" }), { status: 401 }),
    );

    await expect(request("/api/protected")).rejects.toBeInstanceOf(ApiError);
    expect(localStorage.getItem("shortlink.sessionToken")).toBeNull();
  });
});
