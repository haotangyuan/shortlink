import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { RotateCcw, Trash2 } from "lucide-react";
import { useState } from "react";
import { adminApi } from "../../api/admin";
import type { LinkPageVO } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";
import { useToast } from "../../components/ui/Toast";

export function RecycleBinPage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [page, setPage] = useState(1);
  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const gidList = groupsQuery.data?.map((group) => group.gid) ?? [];
  const recycleQuery = useQuery({
    queryKey: ["recycle-bin", gidList.join(","), page],
    enabled: gidList.length > 0,
    queryFn: () => adminApi.getRecycleBinLinks(gidList, page, 10),
  });

  const invalidate = () => queryClient.invalidateQueries({ queryKey: ["recycle-bin"] });
  const restoreMutation = useMutation({
    mutationFn: (link: LinkPageVO) => adminApi.restoreLink(link.gid, link.fullShortUrl),
    onSuccess: () => {
      notify("短链接已恢复", "success");
      void invalidate();
    },
  });
  const removeMutation = useMutation({
    mutationFn: (link: LinkPageVO) => adminApi.removeLink(link.gid, link.fullShortUrl),
    onSuccess: () => {
      notify("短链接已移除", "success");
      void invalidate();
    },
  });

  const records = recycleQuery.data?.records ?? [];

  return (
    <div className="grid gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-950">回收站</h1>
        <p className="mt-1 text-sm text-slate-500">恢复或永久移除已回收的短链接</p>
      </div>
      <Card>
        <CardContent>
          {records.length ? (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[760px] text-left text-sm">
                  <thead className="border-b border-slate-200 text-slate-500">
                    <tr>
                      <th className="py-3">短链接</th>
                      <th className="py-3">原始链接</th>
                      <th className="py-3">删除时间</th>
                      <th className="py-3 text-right">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {records.map((link) => (
                      <tr key={link.id} className="border-b border-slate-100">
                        <td className="py-3 font-medium text-slate-950">{link.fullShortUrl}</td>
                        <td className="py-3 text-slate-500">{link.originUrl}</td>
                        <td className="py-3 text-slate-500">{link.delTime ?? "-"}</td>
                        <td className="py-3">
                          <div className="flex justify-end gap-2">
                            <Button variant="secondary" onClick={() => restoreMutation.mutate(link)}>
                              <RotateCcw className="h-4 w-4" />
                              恢复
                            </Button>
                            <Button variant="danger" onClick={() => removeMutation.mutate(link)}>
                              <Trash2 className="h-4 w-4" />
                              移除
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="mt-4 flex justify-end gap-2">
                <Button variant="secondary" disabled={page <= 1} onClick={() => setPage((value) => value - 1)}>
                  上一页
                </Button>
                <Button
                  variant="secondary"
                  disabled={page >= (recycleQuery.data?.pages ?? 1)}
                  onClick={() => setPage((value) => value + 1)}
                >
                  下一页
                </Button>
              </div>
            </>
          ) : (
            <EmptyState title={recycleQuery.isLoading ? "正在加载回收站" : "回收站为空"} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
