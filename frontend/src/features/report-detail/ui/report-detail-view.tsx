import Link from "next/link";
import ReactMarkdown from "react-markdown";

import type { ReportDetail } from "@/entities/report/model/types";
import { getReportDownloadUrl } from "@/entities/report/api/report-api";

interface ReportDetailViewProps {
  report: ReportDetail;
  reportId: string;
}

export function ReportDetailView({
  report,
  reportId,
}: ReportDetailViewProps) {
  return (
    <div className="space-y-6">
      <Link href="/" className="text-sm text-blue-600 hover:underline">
        ← 返回列表
      </Link>

      <div className="rounded-lg bg-white p-6 shadow">
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-lg font-semibold">报告 #{report.id}</h2>
          {report.status === "completed" && (
            <div className="flex gap-2">
              <a
                href={getReportDownloadUrl(reportId, "txt")}
                className="rounded-md bg-gray-100 px-3 py-1.5 text-xs font-medium text-gray-700 hover:bg-gray-200"
              >
                📄 TXT
              </a>
              <a
                href={getReportDownloadUrl(reportId, "pdf")}
                className="rounded-md bg-red-50 px-3 py-1.5 text-xs font-medium text-red-700 hover:bg-red-100"
              >
                📕 PDF
              </a>
              <a
                href={getReportDownloadUrl(reportId, "csv")}
                className="rounded-md bg-green-50 px-3 py-1.5 text-xs font-medium text-green-700 hover:bg-green-100"
              >
                📊 CSV
              </a>
            </div>
          )}
        </div>
        <dl className="grid grid-cols-2 gap-x-6 gap-y-2 text-sm md:grid-cols-3">
          <div>
            <dt className="text-gray-500">客户</dt>
            <dd>
              {report.customerName} ({report.customerId})
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">身份证</dt>
            <dd>{report.nationalId}</dd>
          </div>
          <div>
            <dt className="text-gray-500">客户经理</dt>
            <dd>
              {report.managerName} ({report.managerId})
            </dd>
          </div>
          <div>
            <dt className="text-gray-500">业务编码</dt>
            <dd>{report.serviceCode}</dd>
          </div>
          <div>
            <dt className="text-gray-500">当前套餐</dt>
            <dd>{report.currentPlan}</dd>
          </div>
          <div>
            <dt className="text-gray-500">创建时间</dt>
            <dd>{report.createdAt}</dd>
          </div>
        </dl>
      </div>

      {report.status === "completed" && report.reportContent ? (
        <div className="prose prose-sm max-w-none rounded-lg bg-white p-6 shadow">
          <ReactMarkdown>{report.reportContent}</ReactMarkdown>
        </div>
      ) : (
        <div className="rounded bg-yellow-50 p-4 text-yellow-800">
          报告状态: {report.status}
          {report.status === "failed" && report.reportContent && (
            <p className="mt-2 text-sm">{report.reportContent}</p>
          )}
        </div>
      )}
    </div>
  );
}
