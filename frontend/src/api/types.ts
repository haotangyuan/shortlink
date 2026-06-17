export type ApiResult<T> = {
  code: string;
  message?: string;
  data?: T;
  requestId?: string;
};

export type UserLoginVO = { token: string };

export type UserVO = {
  id: number;
  username: string;
  realName?: string;
  phone?: string;
  mail?: string;
};

export type UserRegisterReq = {
  username: string;
  password: string;
  realName?: string;
  phone?: string;
  mail?: string;
};

export type UserUpdateReq = Omit<UserRegisterReq, "password"> & { password?: string };

export type GroupVO = {
  gid: string;
  name: string;
  sortOrder: number;
  linkCount: number;
};

export type GroupSortReq = {
  gid: string;
  sortOrder: number;
};

export type LinkCreateReq = {
  originUrl: string;
  gid: string;
  createdType?: number;
  validDateType?: number;
  validDate?: string;
  describe?: string;
};

export type LinkUpdateReq = {
  originUrl: string;
  fullShortUrl: string;
  originGid: string;
  gid: string;
  validDateType: number;
  validDate?: string;
  describe?: string;
};

export type LinkCreateVO = {
  gid: string;
  originUrl: string;
  fullShortUrl: string;
};

export type LinkPageVO = {
  id: number;
  domain: string;
  shortUri: string;
  fullShortUrl: string;
  originUrl: string;
  gid: string;
  validDateType: number;
  validDate?: string;
  createTime: string;
  describe?: string;
  favicon?: string;
  totalPv: number;
  totalUv: number;
  totalUip: number;
  todayPv: number;
  todayUv: number;
  todayUip: number;
  delTime?: number;
};

export type PageResult<T> = {
  records: T[];
  total: number;
  size: number;
  current: number;
  pages: number;
};

export type TokenVO = {
  id: number;
  tokenMasked: string;
  name: string;
  enableStatus: number;
  validDate?: string;
  describe?: string;
  updateTime?: string;
};

export type TokenCreateReq = {
  name: string;
  describe?: string;
  validDate?: string | null;
};

export type LinkStatsDailyVO = {
  date: string;
  pv: number;
  uv: number;
  uip: number;
};

export type RatioStat = {
  cnt: number;
  ratio: number;
  browser?: string;
  os?: string;
  locale?: string;
  uvType?: string;
  device?: string;
  network?: string;
};

export type TopIpStat = {
  cnt: number;
  ip: string;
};

export type LinkStatsVO = {
  pv: number;
  uv: number;
  uip: number;
  daily?: LinkStatsDailyVO[];
  localeCnStats?: RatioStat[];
  hourStats?: number[];
  topIpStats?: TopIpStat[];
  weekdayStats?: number[];
  browserStats?: RatioStat[];
  osStats?: RatioStat[];
  uvTypeStats?: RatioStat[];
  deviceStats?: RatioStat[];
  networkStats?: RatioStat[];
};

export type LinkStatsAccessRecordVO = {
  uvType?: string;
  browser?: string;
  os?: string;
  ip?: string;
  network?: string;
  device?: string;
  locale?: string;
  user?: string;
  createTime?: string;
};

export type AiSession = {
  sessionId: string;
  title: string;
  createTime: string;
  updateTime: string;
};

export type AiMessage = {
  role: "user" | "assistant";
  content: string;
  createTime: string;
};
