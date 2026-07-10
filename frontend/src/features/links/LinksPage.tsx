import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { BarChart3, ExternalLink, Pencil, Plus, Recycle } from "lucide-react";
import { Link, useSearchParams } from "react-router-dom";
import { adminApi } from "../../api/admin";
import type { LinkPageVO } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";
import { CopyButton } from "../../components/ui/CopyButton";
import { EmptyState } from "../../components/ui/EmptyState";
import { Input } from "../../components/ui/Input";
import { Select } from "../../components/ui/Select";
import { useToast } from "../../components/ui/Toast";

export function LinksPage() {
  const queryClient = useQueryClient();
  const { notify } = useToast();
  const [searchParams, setSearchParams] = useSearchParams();
  const selectedGid = searchParams.get("gid") ?? "";
  const page = Number(searchParams.get("page") ?? "1");
  const keyword = searchParams.get("q") ?? "";

  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const groups = groupsQuery.data ?? [];
  const gid = selectedGid || groups[0]?.gid || "";
  const linksQuery = useQuery({
    queryKey: ["links", gid, page, keyword],
    enabled: Boolean(gid),
    queryFn: () => adminApi.getLinks(gid, page, 10, undefined, keyword),
  });
  const linkPage = linksQuery.data;
  const links = linkPage?.records ?? [];

  const recycleMutation = useMutation({
    mutationFn: (link: LinkPageVO) => adminApi.moveToRecycleBin(link.gid, link.fullShortUrl),
    onSuccess: () => {
      notify("已移入回收站", "success");
      void queryClient.invalidateQueries({ queryKey: ["links"] });
    },
  });

  function updateParam(key: string, value: string) {
    const next = new URLSearchParams(searchParams);
    if (value) next.set(key, value);
    else next.delete(key);
    if (key !== "page") next.set("page", "1");
    setSearchParams(next);
  }

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-950">链接管理</h1>
          <p className="mt-1 text-sm text-slate-500">按分组筛选、编辑和回收短链接</p>
        </div>
        <Link to="/dashboard/links/create">
          <Button>
            <Plus className="h-4 w-4" />
            创建链接
          </Button>
        </Link>
      </div>
      <Card>
        <CardContent>
          <div className="grid gap-3 md:grid-cols-[240px_1fr]">
            <Select value={gid} onChange={(event) => updateParam("gid", event.target.value)}>
              {groups.map((group) => (
                <option key={group.gid} value={group.gid}>
                  {group.name}
                </option>
              ))}
            </Select>
            <Input
              value={keyword}
              onChange={(event) => updateParam("q", event.target.value)}
              placeholder="搜索短链接、原始链接或描述"
            />
          </div>
        </CardContent>
      </Card>
      <Card>
        <CardContent>
          {links.length ? (
            <>
              <div className="overflow-x-auto">
                <table className="w-full min-w-[980px] text-left text-sm">
                  <thead className="border-b border-slate-200 text-slate-500">
                    <tr>
                      <th className="py-3">短链接</th>
                      <th className="py-3">原始链接</th>
                      <th className="py-3">访问</th>
                      <th className="py-3">有效期</th>
                      <th className="py-3 text-right">操作</th>
                    </tr>
                  </thead>
                  <tbody>
                    {links.map((link) => (
                      <tr key={link.id} className="border-b border-slate-100">
                        <td className="py-3">
                          <div className="max-w-[240px] truncate font-medium text-slate-950">
                            {link.fullShortUrl}
                          </div>
                          <div className="mt-1 text-xs text-slate-500">{link.describe || "未填写描述"}</div>
                        </td>
                        <td className="py-3">
                          <div className="max-w-[320px] truncate text-slate-600">{link.originUrl}</div>
                        </td>
                        <td className="py-3 text-slate-600">
                          PV {link.totalPv ?? 0} / UV {link.totalUv ?? 0}
                        </td>
                        <td className="py-3 text-slate-500">{link.validDate || "按服务端规则"}</td>
                        <td className="py-3">
                          <div className="flex justify-end gap-2">
                            <CopyButton text={link.fullShortUrl} label="复制" />
                            <a href={link.fullShortUrl} target="_blank" rel="noreferrer">
                              <Button variant="ghost" className="h-10 w-10 px-0">
                                <ExternalLink className="h-4 w-4" />
                              </Button>
                            </a>
                            <Link
                              to={`/dashboard/links/edit?gid=${encodeURIComponent(link.gid)}&fullShortUrl=${encodeURIComponent(link.fullShortUrl)}`}
                            >
                              <Button variant="ghost" className="h-10 w-10 px-0">
                                <Pencil className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Link
                              to={`/dashboard/analytics?gid=${encodeURIComponent(link.gid)}&fullShortUrl=${encodeURIComponent(link.fullShortUrl)}`}
                            >
                              <Button variant="ghost" className="h-10 w-10 px-0">
                                <BarChart3 className="h-4 w-4" />
                              </Button>
                            </Link>
                            <Button
                              variant="ghost"
                              className="h-10 w-10 px-0"
                              onClick={() => recycleMutation.mutate(link)}
                            >
                              <Recycle className="h-4 w-4 text-red-600" />
                            </Button>
                          </div>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
              <div className="mt-4 flex items-center justify-between text-sm text-slate-500">
                <span>共 {linkPage?.total ?? 0} 条</span>
                <div className="flex gap-2">
                  <Button
                    variant="secondary"
                    disabled={page <= 1}
                    onClick={() => updateParam("page", String(page - 1))}
                  >
                    上一页
                  </Button>
                  <Button
                    variant="secondary"
                    disabled={page >= (linkPage?.pages ?? 1)}
                    onClick={() => updateParam("page", String(page + 1))}
                  >
                    下一页
                  </Button>
                </div>
              </div>
            </>
          ) : (
            <EmptyState title={linksQuery.isLoading ? "正在加载链接" : "暂无链接"} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
