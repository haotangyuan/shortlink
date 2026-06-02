import type { GroupSortReq, GroupVO } from "../../api/types";

export function buildGroupSortPayload(groups: GroupVO[]): GroupSortReq[] {
  return groups.map((group, index) => ({
    gid: group.gid,
    sortOrder: groups.length - index,
  }));
}
