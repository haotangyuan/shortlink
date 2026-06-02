import type { LinkStatsAccessRecordVO, PageResult } from "../../api/types";
import { Card, CardContent, CardHeader, CardTitle } from "../../components/ui/Card";
import { EmptyState } from "../../components/ui/EmptyState";

export function AccessRecordsTable({ page }: { page?: PageResult<LinkStatsAccessRecordVO> }) {
  const records = page?.records ?? [];

  return (
    <Card>
      <CardHeader>
        <CardTitle>访问记录</CardTitle>
      </CardHeader>
      <CardContent>
        {records.length ? (
          <div className="overflow-x-auto">
            <table className="w-full min-w-[840px] text-left text-sm">
              <thead className="border-b border-slate-200 text-slate-500">
                <tr>
                  <th className="py-3">时间</th>
                  <th className="py-3">IP</th>
                  <th className="py-3">地区</th>
                  <th className="py-3">设备</th>
                  <th className="py-3">浏览器</th>
                  <th className="py-3">访客</th>
                </tr>
              </thead>
              <tbody>
                {records.map((record, index) => (
                  <tr key={`${record.ip}-${record.createTime}-${index}`} className="border-b border-slate-100">
                    <td className="py-3 text-slate-600">{record.createTime}</td>
                    <td className="py-3 text-slate-600">{record.ip}</td>
                    <td className="py-3 text-slate-600">{record.locale}</td>
                    <td className="py-3 text-slate-600">{record.device}</td>
                    <td className="py-3 text-slate-600">{record.browser}</td>
                    <td className="py-3 text-slate-600">{record.uvType}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : (
          <EmptyState title="暂无访问记录" />
        )}
      </CardContent>
    </Card>
  );
}
