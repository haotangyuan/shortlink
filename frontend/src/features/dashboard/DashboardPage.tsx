import { useQuery } from "@tanstack/react-query";
import { BarChart3, Boxes, Link as LinkIcon, MousePointerClick } from "lucide-react";
import { Link } from "react-router-dom";
import { adminApi } from "../../api/admin";
import type { LinkPageVO } from "../../api/types";
import { Button } from "../../components/ui/Button";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";

function sum(records: LinkPageVO[], key: keyof Pick<LinkPageVO, "totalPv" | "todayPv">) {
  return records.reduce((total, item) => total + (item[key] ?? 0), 0);
}

export function DashboardPage() {
  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const groups = groupsQuery.data ?? [];
  const linksQuery = useQuery({
    queryKey: ["dashboard-links", groups.map((group) => group.gid).join(",")],
    enabled: groups.length > 0,
    queryFn: async () => {
      const pages = await Promise.all(groups.map((group) => adminApi.getLinks(group.gid, 1, 50)));
      return pages.flatMap((page) => page.records);
    },
  });
  const records = linksQuery.data ?? [];

  const stats = [
    { label: "短链接总数", value: groups.reduce((total, group) => total + group.linkCount, 0), icon: LinkIcon },
    { label: "分组数量", value: groups.length, icon: Boxes },
    { label: "今日访问", value: sum(records, "todayPv"), icon: MousePointerClick },
    { label: "累计访问", value: sum(records, "totalPv"), icon: BarChart3 },
  ];

  return (
    <div className="grid gap-6">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-slate-950">仪表板</h1>
          <p className="mt-1 text-sm text-slate-500">短链接、分组和访问数据总览</p>
        </div>
        <div className="flex gap-2">
          <Link to="/dashboard/links/create">
            <Button>创建链接</Button>
          </Link>
          <Link to="/dashboard/groups">
            <Button variant="secondary">管理分组</Button>
          </Link>
        </div>
      </div>

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-4">
        {stats.map((item) => (
          <Card key={item.label}>
            <CardContent className="flex items-center justify-between">
              <div>
                <div className="text-sm text-slate-500">{item.label}</div>
                <div className="mt-2 text-2xl font-semibold text-slate-950">{item.value}</div>
              </div>
              <item.icon className="h-6 w-6 text-blue-600" />
            </CardContent>
          </Card>
        ))}
      </div>

      <Card>
        <CardHeader>
          <CardTitle>分组概览</CardTitle>
        </CardHeader>
        <CardContent>
          {groups.length ? (
            <div className="grid gap-3 md:grid-cols-2 xl:grid-cols-3">
              {groups.map((group) => (
                <Link
                  key={group.gid}
                  to={`/dashboard/links?gid=${encodeURIComponent(group.gid)}`}
                  className="rounded-lg border border-slate-200 p-4 hover:border-blue-300 hover:bg-blue-50"
                >
                  <div className="font-medium text-slate-950">{group.name}</div>
                  <div className="mt-2 text-sm text-slate-500">{group.linkCount} 个链接</div>
                </Link>
              ))}
            </div>
          ) : (
            <EmptyState title="暂无分组" action={<Link to="/dashboard/groups"><Button>创建分组</Button></Link>} />
          )}
        </CardContent>
      </Card>
    </div>
  );
}
