import { buildQuery, request } from "./client";
import type {
  AiMessage,
  AiSession,
  GroupSortReq,
  GroupVO,
  LinkCreateReq,
  LinkCreateVO,
  LinkPageVO,
  LinkStatsAccessRecordVO,
  LinkStatsVO,
  LinkUpdateReq,
  PageResult,
  TokenCreateReq,
  TokenVO,
  UserLoginVO,
  UserRegisterReq,
  UserUpdateReq,
  UserVO,
} from "./types";

const admin = "/api/short-link/admin/v1";

export const adminApi = {
  login: (username: string, password: string) =>
    request<UserLoginVO>(
      `${admin}/user/login`,
      { method: "POST", body: JSON.stringify({ username, password }) },
      false,
    ),
  register: (body: UserRegisterReq) =>
    request<void>(`${admin}/user`, { method: "POST", body: JSON.stringify(body) }, false),
  getUser: (username: string) => request<UserVO>(`${admin}/user/${encodeURIComponent(username)}`),
  updateUser: (body: UserUpdateReq) =>
    request<void>(`${admin}/user`, { method: "PUT", body: JSON.stringify(body) }),
  checkLogin: (username: string, token: string) =>
    request<boolean>(
      `${admin}/user/check-login${buildQuery({ username, token })}`,
      { method: "POST" },
    ),
  logout: (username: string, token: string) =>
    request<void>(`${admin}/user/logout${buildQuery({ username, token })}`, { method: "DELETE" }),
  existsUsername: (username: string) =>
    request<boolean>(`${admin}/user/exists${buildQuery({ username })}`, {}, false),

  createPublicLink: (originUrl: string, describe?: string) =>
    request<LinkCreateVO>(
      `${admin}/create`,
      { method: "POST", body: JSON.stringify({ originUrl, describe, gid: "public" }) },
      false,
    ),
  createLink: (body: LinkCreateReq) =>
    request<LinkCreateVO>(`${admin}/create`, { method: "POST", body: JSON.stringify(body) }),
  updateLink: (body: LinkUpdateReq) =>
    request<void>(`${admin}/update`, { method: "POST", body: JSON.stringify(body) }),
  getLinks: (gid: string, current = 1, size = 10, orderTag?: string, keyword?: string) =>
    request<PageResult<LinkPageVO>>(
      `${admin}/page${buildQuery({ gid, current, size, orderTag, keyword })}`,
    ),
  getTitle: (url: string) => request<string>(`${admin}/title${buildQuery({ url })}`),

  getGroups: () => request<GroupVO[]>(`${admin}/group`),
  createGroup: (name: string) =>
    request<void>(`${admin}/group`, { method: "POST", body: JSON.stringify({ name }) }),
  updateGroup: (gid: string, name: string) =>
    request<void>(`${admin}/group`, { method: "PUT", body: JSON.stringify({ gid, name }) }),
  deleteGroup: (gid: string) =>
    request<void>(`${admin}/group${buildQuery({ gid })}`, { method: "DELETE" }),
  sortGroups: (body: GroupSortReq[]) =>
    request<void>(`${admin}/group/sort`, { method: "POST", body: JSON.stringify(body) }),

  moveToRecycleBin: (gid: string, fullShortUrl: string) =>
    request<void>(`${admin}/recycle-bin/save`, {
      method: "POST",
      body: JSON.stringify({ gid, fullShortUrl }),
    }),
  getRecycleBinLinks: (gidList: string[], current = 1, size = 10) =>
    request<PageResult<LinkPageVO>>(
      `${admin}/recycle-bin/page${buildQuery({ gidList, current, size })}`,
    ),
  restoreLink: (gid: string, fullShortUrl: string) =>
    request<void>(`${admin}/recycle-bin/restore`, {
      method: "POST",
      body: JSON.stringify({ gid, fullShortUrl }),
    }),
  removeLink: (gid: string, fullShortUrl: string) =>
    request<void>(`${admin}/recycle-bin/remove`, {
      method: "POST",
      body: JSON.stringify({ gid, fullShortUrl }),
    }),

  getLinkStats: (
    fullShortUrl: string,
    gid: string,
    startDate: string,
    endDate: string,
    enableStatus = 0,
  ) =>
    request<LinkStatsVO>(
      `${admin}/stats${buildQuery({ fullShortUrl, gid, startDate, endDate, enableStatus })}`,
    ),
  getGroupStats: (gid: string, startDate: string, endDate: string) =>
    request<LinkStatsVO>(`${admin}/stats/group${buildQuery({ gid, startDate, endDate })}`),
  getLinkAccessRecords: (
    fullShortUrl: string,
    gid: string,
    startDate: string,
    endDate: string,
    current: number,
    size: number,
    enableStatus = 0,
  ) =>
    request<PageResult<LinkStatsAccessRecordVO>>(
      `${admin}/stats/access-record${buildQuery({
        fullShortUrl,
        gid,
        startDate,
        endDate,
        current,
        size,
        enableStatus,
      })}`,
    ),
  getGroupAccessRecords: (
    gid: string,
    startDate: string,
    endDate: string,
    current: number,
    size: number,
  ) =>
    request<PageResult<LinkStatsAccessRecordVO>>(
      `${admin}/stats/access-record/group${buildQuery({ gid, startDate, endDate, current, size })}`,
    ),

  getTokens: () => request<TokenVO[]>(`${admin}/token`),
  createToken: (body: TokenCreateReq) =>
    request<string>(`${admin}/token`, { method: "POST", body: JSON.stringify(body) }),
  updateTokenStatus: (id: string, enable: boolean) =>
    request<void>(`${admin}/token/${id}/status${buildQuery({ enable })}`, { method: "PATCH" }),
  deleteToken: (id: string) => request<void>(`${admin}/token/${id}`, { method: "DELETE" }),

  getAiSessions: () => request<AiSession[]>(`${admin}/ai/sessions`),
  getAiSessionMessages: (sessionId: string) =>
    request<AiMessage[]>(`${admin}/ai/sessions/${sessionId}/messages`),
  deleteAiSession: (sessionId: string) =>
    request<void>(`${admin}/ai/sessions/${sessionId}`, { method: "DELETE" }),
};
