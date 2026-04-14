import Link from "next/link";

import type { ReportSummary } from "@/entities/report/model/types";
import { StatusBadge } from "@/shared/ui/status-badge";

interface RecentReportsPanelProps {
  loading: boolean;
  reports: ReportSummary[];
}

export function RecentReportsPanel({
  loading,
  reports,
}: RecentReportsPanelProps) {
  return (
    <section className="rounded-[28px] border border-slate-200/80 bg-white/85 p-5 shadow-[0_18px_45px_rgba(15,23,42,0.06)] sm:p-6">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
            Queue Activity
          </p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
            Recent reports
          </h3>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Keep an eye on the newest submissions without leaving the creation
            workspace.
          </p>
        </div>
        <Link
          href="/reports"
          className="inline-flex items-center rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
        >
          View all
        </Link>
      </div>

      <div className="mt-6 space-y-3">
        {loading ? (
          <div className="rounded-[24px] border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
            Loading recent reports...
          </div>
        ) : reports.length === 0 ? (
          <div className="rounded-[24px] border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
            No reports yet. New submissions will appear here.
          </div>
        ) : (
          reports.map((report) => (
            <article
              key={report.id}
              className="rounded-[24px] border border-slate-200/80 bg-[linear-gradient(180deg,#ffffff_0%,#f8fafc_100%)] p-4 transition hover:border-slate-300"
            >
              <div className="flex items-start justify-between gap-4">
                <div>
                  <div className="text-sm font-semibold text-slate-950">
                    {report.customerName}
                  </div>
                  <div className="mt-1 text-xs text-slate-500">
                    Report #{report.id} · Customer ID {report.customerId}
                  </div>
                </div>
                <StatusBadge status={report.status} />
              </div>
              <div className="mt-4 flex items-center justify-between gap-3">
                <div className="text-xs text-slate-500">
                  Created {report.createdAt}
                </div>
                <Link
                  href={
                    report.status === "completed"
                      ? `/report/${report.id}`
                      : "/reports"
                  }
                  className="text-sm font-semibold text-blue-600 hover:text-blue-700"
                >
                  {report.status === "completed" ? "Open report" : "Track status"}
                </Link>
              </div>
            </article>
          ))
        )}
      </div>
    </section>
  );
}
