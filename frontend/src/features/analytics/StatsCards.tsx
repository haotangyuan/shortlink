import { BarChart3, MousePointerClick, UsersRound } from "lucide-react";
import type { LinkStatsVO } from "../../api/types";
import { Card, CardContent } from "../../components/ui/Card";

export function StatsCards({ stats }: { stats?: LinkStatsVO }) {
  const items = [
    { label: "PV", value: stats?.pv ?? 0, icon: MousePointerClick },
    { label: "UV", value: stats?.uv ?? 0, icon: UsersRound },
    { label: "UIP", value: stats?.uip ?? 0, icon: BarChart3 },
  ];

  return (
    <div className="grid gap-4 md:grid-cols-3">
      {items.map((item) => (
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
  );
}
