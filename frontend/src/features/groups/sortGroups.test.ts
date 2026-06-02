import { describe, expect, it } from "vitest";
import type { GroupVO } from "../../api/types";
import { buildGroupSortPayload } from "./sortGroups";

function group(gid: string): GroupVO {
  return { gid, name: gid, sortOrder: 0, linkCount: 0 };
}

describe("buildGroupSortPayload", () => {
  it("assigns higher sortOrder to groups that should appear earlier", () => {
    expect(buildGroupSortPayload([group("alpha"), group("gamma"), group("beta")])).toEqual([
      { gid: "alpha", sortOrder: 3 },
      { gid: "gamma", sortOrder: 2 },
      { gid: "beta", sortOrder: 1 },
    ]);
  });
});
