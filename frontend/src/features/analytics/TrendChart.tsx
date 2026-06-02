import {
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import type { LinkStatsDailyVO } from "../../api/types";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";

export function TrendChart({ data = [] }: { data?: LinkStatsDailyVO[] }) {
  return (
    <Card>
      <CardHeader>
        <CardTitle>访问趋势</CardTitle>
      </CardHeader>
      <CardContent>
        {data.length ? (
          <div className="h-72">
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={data}>
                <CartesianGrid strokeDasharray="3 3" />
                <XAxis dataKey="date" />
                <YAxis allowDecimals={false} />
                <Tooltip />
                <Line type="monotone" dataKey="pv" name="PV" stroke="#2563eb" strokeWidth={2} />
                <Line type="monotone" dataKey="uv" name="UV" stroke="#059669" strokeWidth={2} />
                <Line type="monotone" dataKey="uip" name="UIP" stroke="#d97706" strokeWidth={2} />
              </LineChart>
            </ResponsiveContainer>
          </div>
        ) : (
          <EmptyState title="暂无趋势数据" />
        )}
      </CardContent>
    </Card>
  );
}
