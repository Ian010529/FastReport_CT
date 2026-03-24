import Link from "next/link";

import type { ReportSummary } from "@/entities/report/model/types";
import { SectionCard } from "@/shared/ui/section-card";
import { StatusBadge } from "@/shared/ui/status-badge";

interface ReportListProps {
  fetched: boolean;
  reports: ReportSummary[];
}

export function ReportList({ fetched, reports }: ReportListProps) {
  return (
    <SectionCard className="h-full">
      <div className="flex items-start justify-between gap-4">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
            Monitor
          </p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
            Recent Reports
          </h3>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            Review recent submissions, track processing state, and open completed reports.
          </p>
        </div>
        <div className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
          {reports.length} items
        </div>
      </div>

      <div className="mt-6">
      {!fetched ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
          Refresh the workspace to load the latest reports.
        </div>
      ) : reports.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
          No reports yet. Your newly submitted reports will appear here.
        </div>
      ) : (
        <>
          <div className="hidden overflow-hidden rounded-2xl border border-slate-200 lg:block">
            <table className="w-full text-sm">
              <thead className="bg-slate-50 text-slate-500">
                <tr>
                  <th className="px-4 py-3 text-left font-medium">Report</th>
                  <th className="px-4 py-3 text-left font-medium">Customer</th>
                  <th className="px-4 py-3 text-left font-medium">Status</th>
                  <th className="px-4 py-3 text-left font-medium">Created</th>
                  <th className="px-4 py-3 text-left font-medium">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-200 bg-white">
                {reports.map((report) => (
                  <tr key={report.id} className="align-top">
                    <td className="px-4 py-4">
                      <div className="font-semibold text-slate-900">#{report.id}</div>
                      <div className="mt-1 text-xs text-slate-500">
                        Report record
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <div className="font-medium text-slate-900">
                        {report.customerName}
                      </div>
                      <div className="mt-1 text-xs text-slate-500">
                        Customer ID {report.customerId}
                      </div>
                    </td>
                    <td className="px-4 py-4">
                      <StatusBadge status={report.status} />
                    </td>
                    <td className="px-4 py-4 text-slate-600">{report.createdAt}</td>
                    <td className="px-4 py-4">
                      {report.status === "completed" ? (
                        <Link
                          href={`/report/${report.id}`}
                          className="inline-flex items-center rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50"
                        >
                          Open Report
                        </Link>
                      ) : (
                        <span className="text-xs text-slate-400">Unavailable</span>
                      )}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="grid gap-3 lg:hidden">
            {reports.map((report) => (
              <article
                key={report.id}
                className="rounded-2xl border border-slate-200 bg-white p-4 shadow-sm"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="text-sm font-semibold text-slate-900">
                      {report.customerName}
                    </div>
                    <div className="mt-1 text-xs text-slate-500">
                      Report #{report.id} · Customer ID {report.customerId}
                    </div>
                  </div>
                  <StatusBadge status={report.status} />
                </div>
                <div className="mt-4 text-xs text-slate-500">
                  Created {report.createdAt}
                </div>
                <div className="mt-4">
                  {report.status === "completed" ? (
                    <Link
                      href={`/report/${report.id}`}
                      className="inline-flex items-center rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50"
                    >
                      Open Report
                    </Link>
                  ) : (
                    <span className="text-xs text-slate-400">Report not ready yet</span>
                  )}
                </div>
              </article>
            ))}
          </div>
        </>
      )}
      </div>
    </SectionCard>
  );
}
