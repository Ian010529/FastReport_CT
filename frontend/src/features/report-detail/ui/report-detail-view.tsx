import Link from "next/link";
import ReactMarkdown from "react-markdown";

import type { ReportDetail } from "@/entities/report/model/types";
import { getReportDownloadUrl } from "@/entities/report/api/report-api";
import { SectionCard } from "@/shared/ui/section-card";
import { StatusBadge } from "@/shared/ui/status-badge";

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
      <Link href="/" className="text-sm font-medium text-slate-600 hover:text-slate-950">
        ← Back to workspace
      </Link>

      <SectionCard>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
              Report Overview
            </p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
              Report #{report.id}
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Review the report metadata, monitor delivery state, and download completed files.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={report.status} />
          {report.status === "completed" && (
            <div className="flex gap-2">
              <a
                href={getReportDownloadUrl(reportId, "txt")}
                className="rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50"
              >
                TXT
              </a>
              <a
                href={getReportDownloadUrl(reportId, "pdf")}
                className="rounded-full border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 shadow-sm hover:border-rose-300 hover:bg-rose-100"
              >
                PDF
              </a>
              <a
                href={getReportDownloadUrl(reportId, "csv")}
                className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-700 shadow-sm hover:border-emerald-300 hover:bg-emerald-100"
              >
                CSV
              </a>
            </div>
          )}
          </div>
        </div>
        <dl className="mt-6 grid grid-cols-1 gap-4 text-sm sm:grid-cols-2 xl:grid-cols-3">
          <DetailItem label="Customer">
            {report.customerName} ({report.customerId})
          </DetailItem>
          <DetailItem label="National ID">{report.nationalId}</DetailItem>
          <DetailItem label="Manager">
            {report.managerName} ({report.managerId})
          </DetailItem>
          <DetailItem label="Service Code">{report.serviceCode}</DetailItem>
          <DetailItem label="Current Plan">{report.currentPlan}</DetailItem>
          <DetailItem label="Created At">{report.createdAt}</DetailItem>
        </dl>
      </SectionCard>

      {report.status === "completed" && report.reportContent ? (
        <SectionCard className="overflow-hidden">
          <div className="mb-4">
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
              Report Content
            </p>
            <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">
              Generated report output
            </h3>
          </div>
          <div className="prose prose-slate max-w-none prose-headings:tracking-tight prose-p:text-slate-700">
            <ReactMarkdown>{report.reportContent}</ReactMarkdown>
          </div>
        </SectionCard>
      ) : (
        <SectionCard>
          <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-900">
            <div className="font-semibold">Report not ready</div>
            <div className="mt-1">
              Current status: <span className="font-medium capitalize">{report.status}</span>
            </div>
            {report.status === "failed" && report.reportContent && (
              <p className="mt-3 text-sm leading-6">{report.reportContent}</p>
            )}
          </div>
        </SectionCard>
      )}
    </div>
  );
}

function DetailItem({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
      <dt className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {label}
      </dt>
      <dd className="mt-2 text-sm font-medium text-slate-900">{children}</dd>
    </div>
  );
}
