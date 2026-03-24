import Link from "next/link";

import type { ReportSummary } from "@/entities/report/model/types";
import { StatusBadge } from "@/shared/ui/status-badge";

interface ReportListProps {
  fetched: boolean;
  reports: ReportSummary[];
}

export function ReportList({ fetched, reports }: ReportListProps) {
  return (
    <section className="rounded-lg bg-white p-6 shadow">
      <h2 className="mb-4 text-lg font-semibold">📋 历史报告</h2>
      {!fetched ? (
        <p className="text-sm text-gray-500">点击 "刷新列表" 加载</p>
      ) : reports.length === 0 ? (
        <p className="text-sm text-gray-500">暂无报告</p>
      ) : (
        <table className="w-full text-sm">
          <thead className="bg-gray-100">
            <tr>
              <th className="px-3 py-2 text-left">ID</th>
              <th className="px-3 py-2 text-left">客户</th>
              <th className="px-3 py-2 text-left">状态</th>
              <th className="px-3 py-2 text-left">创建时间</th>
              <th className="px-3 py-2 text-left">操作</th>
            </tr>
          </thead>
          <tbody>
            {reports.map((report) => (
              <tr key={report.id} className="border-t">
                <td className="px-3 py-2">{report.id}</td>
                <td className="px-3 py-2">
                  {report.customerName} ({report.customerId})
                </td>
                <td className="px-3 py-2">
                  <StatusBadge status={report.status} />
                </td>
                <td className="px-3 py-2">{report.createdAt}</td>
                <td className="px-3 py-2">
                  {report.status === "completed" && (
                    <Link
                      href={`/report/${report.id}`}
                      className="text-blue-600 hover:underline"
                    >
                      查看报告
                    </Link>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </section>
  );
}
