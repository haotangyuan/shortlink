import { useQuery } from "@tanstack/react-query";
import { useEffect, useState } from "react";
import { useSearchParams } from "react-router-dom";
import { adminApi } from "../../api/admin";
import { dateRangeByDays } from "../../lib/date";
import { Button } from "../../components/ui/Button";
import { Card, CardContent } from "../../components/ui/Card";
import { Field } from "../../components/ui/Field";
import { Select } from "../../components/ui/Select";
import { AccessRecordsTable } from "./AccessRecordsTable";
import { DistributionCharts } from "./DistributionCharts";
import { StatsCards } from "./StatsCards";
import { TrendChart } from "./TrendChart";

export function AnalyticsPage() {
  const [searchParams] = useSearchParams();
  const initialRange = dateRangeByDays(7);
  const [gid, setGid] = useState(searchParams.get("gid") ?? "");
  const [fullShortUrl, setFullShortUrl] = useState(searchParams.get("fullShortUrl") ?? "");
  const [range, setRange] = useState(initialRange);

  const groupsQuery = useQuery({ queryKey: ["groups"], queryFn: adminApi.getGroups });
  const groups = groupsQuery.data ?? [];
  useEffect(() => {
    if (!gid && groups[0]?.gid) setGid(groups[0].gid);
  }, [gid, groups]);

  const linksQuery = useQuery({
    queryKey: ["analytics-links", gid],
    enabled: Boolean(gid),
    queryFn: () => adminApi.getLinks(gid, 1, 100),
  });
  const links = linksQuery.data?.records ?? [];

  const statsQuery = useQuery({
    queryKey: ["stats", gid, fullShortUrl, range],
    enabled: Boolean(gid),
    queryFn: () =>
      fullShortUrl
        ? adminApi.getLinkStats(fullShortUrl, gid, range.startDate, range.endDate)
        : adminApi.getGroupStats(gid, range.startDate, range.endDate),
  });
  const recordsQuery = useQuery({
    queryKey: ["stats-records", gid, fullShortUrl, range],
    enabled: Boolean(gid),
    queryFn: () =>
      fullShortUrl
        ? adminApi.getLinkAccessRecords(fullShortUrl, gid, range.startDate, range.endDate, 1, 10)
        : adminApi.getGroupAccessRecords(gid, range.startDate, range.endDate, 1, 10),
  });

  return (
    <div className="grid gap-6">
      <div>
        <h1 className="text-2xl font-semibold text-slate-950">数据统计</h1>
        <p className="mt-1 text-sm text-slate-500">按分组或单条短链接查看访问数据</p>
      </div>
      <Card>
        <CardContent className="grid gap-4 lg:grid-cols-[220px_1fr_auto]">
          <Field label="分组">
            <Select
              value={gid}
              onChange={(event) => {
                setGid(event.target.value);
                setFullShortUrl("");
              }}
            >
              {groups.map((group) => (
                <option key={group.gid} value={group.gid}>
                  {group.name}
                </option>
              ))}
            </Select>
          </Field>
          <Field label="链接">
            <Select value={fullShortUrl} onChange={(event) => setFullShortUrl(event.target.value)}>
              <option value="">分组汇总</option>
              {links.map((link) => (
                <option key={link.id} value={link.fullShortUrl}>
                  {link.fullShortUrl}
                </option>
              ))}
            </Select>
          </Field>
          <div className="flex flex-wrap items-end gap-2">
            {[1, 7, 30, 90].map((days) => (
              <Button key={days} variant="secondary" onClick={() => setRange(dateRangeByDays(days))}>
                {days === 1 ? "今天" : `${days} 天`}
              </Button>
            ))}
          </div>
        </CardContent>
      </Card>
      <StatsCards stats={statsQuery.data} />
      <TrendChart data={statsQuery.data?.daily} />
      <DistributionCharts
        browserStats={statsQuery.data?.browserStats}
        osStats={statsQuery.data?.osStats}
        deviceStats={statsQuery.data?.deviceStats}
        networkStats={statsQuery.data?.networkStats}
        localeCnStats={statsQuery.data?.localeCnStats}
        uvTypeStats={statsQuery.data?.uvTypeStats}
        topIpStats={statsQuery.data?.topIpStats}
      />
      <AccessRecordsTable page={recordsQuery.data} />
    </div>
  );
}
