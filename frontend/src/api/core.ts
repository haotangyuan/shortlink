import { buildQuery, request } from "./client";
import type { LinkCreateReq, LinkCreateVO, LinkPageVO, PageResult } from "./types";

const core = "/api/short-link/v1";

export const coreApi = {
  createLink: (body: LinkCreateReq, apiToken: string) =>
    request<LinkCreateVO>(
      `${core}/create`,
      {
        method: "POST",
        headers: { Authorization: `Bearer ${apiToken}` },
        body: JSON.stringify(body),
      },
      false,
    ),
  getLinks: (gid: string, apiToken: string, current = 1, size = 10) =>
    request<PageResult<LinkPageVO>>(
      `${core}/page${buildQuery({ gid, current, size })}`,
      { headers: { Authorization: `Bearer ${apiToken}` } },
      false,
    ),
};
