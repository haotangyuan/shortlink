import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { ArrowDown, ArrowUp, Pencil, Plus, Trash2 } from "lucide-react";
import { FormEvent, useState } from "react";
import { adminApi } from "../../api/admin";
import type { GroupVO } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { Input } from "../../components/ui/Input";
import { useToast } from "../../components/ui/Toast";
import { buildGroupSortPayload } from "./sortGroups";

export function GroupsPage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [name, setName] = useState("");
  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const groups = groupsQuery.data ?? [];

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["groups"] });
  const createMutation = useMutation({
    mutationFn: adminApi.createGroup,
    onSuccess: () => {
      setName("");
      notify("分组已创建", "success");
      void invalidate();
    },
  });
  const updateMutation = useMutation({
    mutationFn: ({ gid, nextName }: { gid: string; nextName: string }) => adminApi.updateGroup(gid, nextName),
    onSuccess: () => {
      notify("分组已更新", "success");
      void invalidate();
    },
  });
  const deleteMutation = useMutation({
    mutationFn: adminApi.deleteGroup,
    onSuccess: () => {
      notify("分组已删除", "success");
      void invalidate();
    },
  });
  const sortMutation = useMutation({
    mutationFn: adminApi.sortGroups,
    onSuccess: () => void invalidate(),
  });

  function submit(event: FormEvent) {
    event.preventDefault();
    if (!name.trim()) return;
    createMutation.mutate(name.trim());
  }

  function rename(group: GroupVO) {
    const nextName = window.prompt("分组名称", group.name);
    if (nextName?.trim()) updateMutation.mutate({ gid: group.gid, nextName: nextName.trim() });
  }

  function remove(group: GroupVO) {
    if (window.confirm(`删除分组「${group.name}」？`)) deleteMutation.mutate(group.gid);
  }

  function move(index: number, offset: number) {
    const next = [...groups];
    const target = index + offset;
    if (target < 0 || target >= next.length) return;
    [next[index], next[target]] = [next[target], next[index]];
    sortMutation.mutate(buildGroupSortPayload(next));
  }

  return (
    <div className="grid gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-950">分组管理</h1>
        <p className="mt-1 text-sm text-slate-500">创建、排序和维护短链接分组</p>
      </div>
      <Card>
        <CardContent>
          <form className="flex flex-col gap-3 sm:flex-row" onSubmit={submit}>
            <Input value={name} onChange={(event) => setName(event.target.value)} placeholder="新分组名称" />
            <Button type="submit" disabled={createMutation.isPending}>
              <Plus className="h-4 w-4" />
              创建
            </Button>
          </form>
        </CardContent>
      </Card>
      <Card>
        <CardHeader>
          <CardTitle>分组列表</CardTitle>
        </CardHeader>
        <CardContent>
          {groups.length ? (
            <div className="overflow-x-auto">
              <table className="w-full min-w-[640px] text-left text-sm">
                <thead className="border-b border-slate-200 text-slate-500">
                  <tr>
                    <th className="py-3">名称</th>
                    <th className="py-3">GID</th>
                    <th className="py-3">链接数</th>
                    <th className="py-3 text-right">操作</th>
                  </tr>
                </thead>
                <tbody>
                  {groups.map((group, index) => (
                    <tr key={group.gid} className="border-b border-slate-100">
                      <td className="py-3 font-medium text-slate-950">{group.name}</td>
                      <td className="py-3 text-slate-500">{group.gid}</td>
                      <td className="py-3 text-slate-500">{group.linkCount}</td>
                      <td className="py-3">
                        <div className="flex justify-end gap-2">
                          <Button variant="ghost" className="h-8 w-8 px-0" onClick={() => move(index, -1)}>
                            <ArrowUp className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" className="h-8 w-8 px-0" onClick={() => move(index, 1)}>
                            <ArrowDown className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" className="h-8 w-8 px-0" onClick={() => rename(group)}>
                            <Pencil className="h-4 w-4" />
                          </Button>
                          <Button variant="ghost" className="h-8 w-8 px-0" onClick={() => remove(group)}>
                            <Trash2 className="h-4 w-4 text-red-600" />
                          </Button>
                        </div>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          ) : (
            <EmptyState title={groupsQuery.isLoading ? "正在加载分组" : "暂无分组"} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
