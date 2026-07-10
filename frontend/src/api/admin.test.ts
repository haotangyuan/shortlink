import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { adminApi } from "./admin";

describe("adminApi", () => {
  beforeEach(() => {
    vi.stubGlobal("fetch", vi.fn());
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("passes the link search keyword to the paginated backend query", async () => {
    vi.mocked(fetch).mockResolvedValue(
      new Response(
        JSON.stringify({
          code: "0",
          data: { records: [], total: 0, size: 10, current: 1, pages: 0 },
        }),
        { status: 200 },
      ),
    );

    await adminApi.getLinks("group-1", 1, 10, undefined, "docs link");

    const [url] = vi.mocked(fetch).mock.calls[0];
    expect(url).toBe(
      "/api/short-link/admin/v1/page?gid=group-1&current=1&size=10&keyword=docs+link",
    );
  });
});
