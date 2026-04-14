import Link from "next/link";
import type { ReactNode } from "react";

import type { ReportSummary } from "@/entities/report/model/types";
import { SectionCard } from "@/shared/ui/section-card";
import { StatusBadge } from "@/shared/ui/status-badge";

interface ReportListProps {
  fetched: boolean;
  reports: ReportSummary[];
  title?: string;
  subtitle?: string;
  eyebrow?: string;
  countLabel?: string;
  headerAction?: ReactNode;
  footer?: ReactNode;
  emptyMessage?: string;
}

export function ReportList({
  fetched,
  reports,
  title = "Recent Reports",
  subtitle = "Review recent submissions, track processing state, and open completed reports.",
  eyebrow = "Monitor",
  countLabel,
  headerAction,
  footer,
  emptyMessage = "No reports yet. Your newly submitted reports will appear here.",
}: ReportListProps) {
  return (
    <SectionCard className="h-full rounded-[28px] border border-slate-200/70 bg-[linear-gradient(180deg,rgba(255,255,255,0.96)_0%,rgba(248,250,252,0.96)_100%)]">
      <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-blue-600">
            {eyebrow}
          </p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
            {title}
          </h3>
          <p className="mt-2 text-sm leading-6 text-slate-500">
            {subtitle}
          </p>
        </div>
        <div className="flex flex-wrap items-center gap-3">
          {headerAction}
          <div className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
            {countLabel ?? `${reports.length} items`}
          </div>
        </div>
      </div>

      <div className="mt-6">
      {!fetched ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
          Refresh the workspace to load the latest reports.
        </div>
      ) : reports.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-slate-200 bg-slate-50 px-4 py-10 text-center text-sm text-slate-500">
          {emptyMessage}
        </div>
      ) : (
        <>
          <div className="grid gap-4">
            {reports.map((report) => (
              <article
                key={report.id}
                className="rounded-[24px] border border-slate-200/80 bg-white p-4 shadow-sm transition hover:border-blue-300 hover:shadow-[0_16px_30px_rgba(15,23,42,0.06)] sm:p-5"
              >
                <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                  <div>
                    <div className="text-xs font-semibold uppercase tracking-[0.18em] text-slate-400">
                      Report #{report.id}
                    </div>
                    <div className="mt-2 text-xl font-semibold tracking-tight text-slate-950">
                      {report.customerName}
                    </div>
                    <div className="mt-1 text-sm text-slate-500">
                      Customer ID {report.customerId}
                    </div>
                  </div>

                  <div className="grid flex-1 gap-4 sm:grid-cols-3 xl:max-w-2xl">
                    <RecordMeta label="Created" value={report.createdAt} />
                    <RecordMeta label="Status">
                      <StatusBadge status={report.status} />
                    </RecordMeta>
                    <RecordMeta label="Action">
                      {report.status === "completed" ? (
                        <Link
                          href={`/report/${report.id}`}
                          className="inline-flex items-center rounded-full bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_100%)] px-4 py-2 text-xs font-semibold text-white shadow-[0_12px_24px_rgba(37,99,235,0.24)] transition hover:brightness-105"
                        >
                          Open report
                        </Link>
                      ) : (
                        <span className="text-xs font-medium text-slate-500">Waiting for completion</span>
                      )}
                    </RecordMeta>
                  </div>
                </div>
              </article>
            ))}
          </div>
        </>
      )}
      </div>
      {footer ? <div className="mt-6 border-t border-slate-200/80 pt-4">{footer}</div> : null}
    </SectionCard>
  );
}

function RecordMeta({
  label,
  value,
  children,
}: {
  label: string;
  value?: string;
  children?: ReactNode;
}) {
  return (
    <div className="rounded-[20px] border border-slate-200/80 bg-slate-50/80 px-4 py-3">
      <div className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-400">
        {label}
      </div>
      <div className="mt-2 text-sm font-medium text-slate-700">
        {children ?? value}
      </div>
    </div>
  );
}
