import { createContext, useCallback, useContext, useEffect, useMemo, useState } from "react";
import { adminApi } from "../api/admin";
import { ApiError, getSessionToken, setSessionToken } from "../api/client";
import type { UserVO } from "../api/types";

type AuthContextValue = {
  user: UserVO | null;
  username: string | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => Promise<void>;
  refreshUser: () => Promise<void>;
  checkAuth: () => Promise<boolean>;
};

const AuthContext = createContext<AuthContextValue | null>(null);

function storedUsername() {
  return localStorage.getItem("shortlink.username");
}

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [username, setUsername] = useState<string | null>(() => storedUsername());
  const [token, setToken] = useState<string | null>(() => getSessionToken());
  const [user, setUser] = useState<UserVO | null>(null);

  const clearAuth = useCallback(() => {
    setUsername(null);
    setToken(null);
    setUser(null);
    setSessionToken(null);
    localStorage.removeItem("shortlink.username");
  }, []);

  const refreshUser = useCallback(async () => {
    if (!username) return;
    const nextUser = await adminApi.getUser(username);
    setUser(nextUser);
  }, [username]);

  const checkAuth = useCallback(async () => {
    if (!username || !token) return false;
    const valid = await adminApi.checkLogin(username, token).catch(() => false);
    if (!valid) clearAuth();
    return valid;
  }, [clearAuth, token, username]);

  const login = useCallback(async (nextUsername: string, password: string) => {
    const result = await adminApi.login(nextUsername, password);
    setSessionToken(result.token);
    localStorage.setItem("shortlink.username", nextUsername);
    setUsername(nextUsername);
    setToken(result.token);
    setUser(await adminApi.getUser(nextUsername));
  }, []);

  const logout = useCallback(async () => {
    if (username && token) {
      await adminApi.logout(username, token).catch(() => undefined);
    }
    clearAuth();
  }, [clearAuth, token, username]);

  useEffect(() => {
    if (username && token && !user) {
      void refreshUser().catch((error) => {
        if (error instanceof ApiError && error.status === 401) clearAuth();
      });
    }
  }, [clearAuth, refreshUser, token, user, username]);

  const value = useMemo(
    () => ({
      user,
      username,
      token,
      isAuthenticated: Boolean(username && token),
      login,
      logout,
      refreshUser,
      checkAuth,
    }),
    [checkAuth, login, logout, refreshUser, token, user, username],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth() {
  const context = useContext(AuthContext);
  if (!context) throw new Error("useAuth must be used within AuthProvider");
  return context;
}
