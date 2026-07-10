import type { ApiResult } from "./types";

export class ApiError extends Error {
  constructor(
    message: string,
    public readonly code?: string,
    public readonly status?: number,
  ) {
    super(message);
  }
}

function getStorage(): Storage | null {
  if (typeof localStorage === "undefined" || typeof localStorage.getItem !== "function") {
    return null;
  }
  return localStorage;
}

let sessionToken = getStorage()?.getItem("shortlink.sessionToken") ?? null;
export const AUTH_CLEARED_EVENT = "shortlink:auth-cleared";

export function setSessionToken(token: string | null) {
  sessionToken = token;
  const storage = getStorage();
  if (!storage) return;
  if (token) storage.setItem("shortlink.sessionToken", token);
  else storage.removeItem("shortlink.sessionToken");
}

export function getSessionToken() {
  return sessionToken;
}

export function clearSessionAfterUnauthorized() {
  setSessionToken(null);
  getStorage()?.removeItem("shortlink.username");
  if (typeof window !== "undefined") {
    window.dispatchEvent(new Event(AUTH_CLEARED_EVENT));
  }
}

export async function request<T>(path: string, init: RequestInit = {}, auth = true): Promise<T> {
  const headers = new Headers(init.headers);
  if (!headers.has("Content-Type") && init.body) headers.set("Content-Type", "application/json");
  if (auth && sessionToken) headers.set("Authorization", `Bearer ${sessionToken}`);

  const response = await fetch(path, { ...init, headers });
  const payload = (await response.json().catch(() => null)) as ApiResult<T> | null;

  if (response.status === 401) {
    if (auth) clearSessionAfterUnauthorized();
    throw new ApiError("用户身份验证失败", payload?.code, response.status);
  }
  if (!payload) throw new ApiError("网络请求失败", undefined, response.status);
  if (payload.code !== "0") {
    throw new ApiError(payload.message || "请求失败", payload.code, response.status);
  }
  return payload.data as T;
}

export function buildQuery(params: Record<string, string | number | boolean | string[] | undefined>) {
  const query = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value === undefined || value === "") return;
    if (Array.isArray(value)) {
      value.forEach((item) => query.append(key, item));
      return;
    }
    query.set(key, String(value));
  });
  const serialized = query.toString();
  return serialized ? `?${serialized}` : "";
}
