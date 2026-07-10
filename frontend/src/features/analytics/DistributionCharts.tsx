import { Cell, Pie, PieChart, ResponsiveContainer, Tooltip } from "recharts";
import type { RatioStat, TopIpStat } from "../../api/types";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";

const colors = ["#2563eb", "#059669", "#d97706", "#7c3aed", "#dc2626", "#0891b2"];

function nameOf(item: RatioStat) {
  return item.browser ?? item.os ?? item.locale ?? item.uvType ?? item.device ?? item.network ?? "未知";
}

function RatioList({ data = [] }: { data?: RatioStat[] | null }) {
  const safeData = data ?? [];
  return (
    <div className="grid gap-3">
      {safeData.slice(0, 6).map((item) => (
        <div key={nameOf(item)} className="grid gap-1">
          <div className="flex justify-between text-sm">
            <span className="text-slate-700">{nameOf(item)}</span>
            <span className="text-slate-500">{item.cnt} 次</span>
          </div>
          <div className="h-2 rounded-full bg-slate-100">
            <div
              className="h-2 rounded-full bg-blue-600"
              style={{ width: `${Math.min(100, Math.round((item.ratio ?? 0) * 100))}%` }}
            />
          </div>
        </div>
      ))}
      {!safeData.length ? <p className="text-sm text-slate-500">暂无数据</p> : null}
    </div>
  );
}

function PieBlock({ title, data = [] }: { title: string; data?: RatioStat[] | null }) {
  const chartData = (data ?? []).map((item) => ({ name: nameOf(item), value: item.cnt }));

  return (
    <Card>
      <CardHeader>
        <CardTitle>{title}</CardTitle>
      </CardHeader>
      <CardContent>
        {chartData.length ? (
          <div className="h-48">
            <ResponsiveContainer width="100%" height="100%" initialDimension={{ width: 320, height: 192 }}>
              <PieChart>
                <Pie data={chartData} dataKey="value" nameKey="name" outerRadius={72}>
                  {chartData.map((entry, index) => (
                    <Cell key={entry.name} fill={colors[index % colors.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <p className="text-sm text-slate-500">暂无数据</p>
        )}
      </CardContent>
    </Card>
  );
}

export function DistributionCharts({
  browserStats,
  osStats,
  deviceStats,
  networkStats,
  localeCnStats,
  uvTypeStats,
  topIpStats,
}: {
  browserStats?: RatioStat[];
  osStats?: RatioStat[];
  deviceStats?: RatioStat[];
  networkStats?: RatioStat[];
  localeCnStats?: RatioStat[];
  uvTypeStats?: RatioStat[];
  topIpStats?: TopIpStat[];
}) {
  return (
    <div className="grid gap-4 xl:grid-cols-2">
      <Card>
        <CardHeader>
          <CardTitle>浏览器</CardTitle>
        </CardHeader>
        <CardContent>
          <RatioList data={browserStats} />
        </CardContent>
      </Card>
      <PieBlock title="操作系统" data={osStats} />
      <Card>
        <CardHeader>
          <CardTitle>设备与网络</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-6 md:grid-cols-2">
          <RatioList data={deviceStats} />
          <RatioList data={networkStats} />
        </CardContent>
      </Card>
      <PieBlock title="地区" data={localeCnStats} />
      <PieBlock title="访客类型" data={uvTypeStats} />
      <Card>
        <CardHeader>
          <CardTitle>高频 IP</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid gap-2">
            {(topIpStats ?? []).slice(0, 8).map((item) => (
              <div key={item.ip} className="flex justify-between rounded-md bg-slate-50 px-3 py-2 text-sm">
                <span>{item.ip}</span>
                <span className="text-slate-500">{item.cnt} 次</span>
              </div>
            ))}
            {!topIpStats?.length ? <p className="text-sm text-slate-500">暂无数据</p> : null}
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
