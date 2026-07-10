import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { AUTH_CLEARED_EVENT, ApiError, request, setSessionToken } from "./client";

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
    localStorage.setItem("shortlink.username", "alice");
    const authCleared = vi.fn();
    window.addEventListener(AUTH_CLEARED_EVENT, authCleared);
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: "A000200", message: "unauthorized" }), { status: 401 }),
    );

    await expect(request("/api/protected")).rejects.toBeInstanceOf(ApiError);
    expect(localStorage.getItem("shortlink.sessionToken")).toBeNull();
    expect(localStorage.getItem("shortlink.username")).toBeNull();
    expect(authCleared).toHaveBeenCalledOnce();
    window.removeEventListener(AUTH_CLEARED_EVENT, authCleared);
  });

  it("keeps the admin session when a separate API token is rejected", async () => {
    setSessionToken("admin-session");
    vi.mocked(fetch).mockResolvedValue(
      new Response(JSON.stringify({ code: "A000200", message: "unauthorized" }), { status: 401 }),
    );

    await expect(request("/api/core", {}, false)).rejects.toBeInstanceOf(ApiError);

    expect(localStorage.getItem("shortlink.sessionToken")).toBe("admin-session");
  });
});
