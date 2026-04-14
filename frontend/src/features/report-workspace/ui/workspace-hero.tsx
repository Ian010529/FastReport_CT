import Link from "next/link";

import { WorkspaceMetricCard } from "@/features/report-workspace/ui/workspace-metric-card";

interface WorkspaceHeroProps {
  recentCount: number;
  completedCount: number;
  attentionCount: number;
}

export function WorkspaceHero({
  recentCount,
  completedCount,
  attentionCount,
}: WorkspaceHeroProps) {
  return (
    <section className="rounded-[30px] border border-slate-200/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.92)_0%,rgba(239,246,255,0.92)_48%,rgba(254,242,242,0.88)_100%)] p-6 shadow-[0_22px_55px_rgba(15,23,42,0.06)] sm:p-8">
      <div className="flex flex-col gap-6 xl:flex-row xl:items-start xl:justify-between">
        <div className="max-w-3xl">
          <p className="text-xs font-semibold uppercase tracking-[0.26em] text-blue-600">
            Workspace
          </p>
          <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">
            Submit reports and track queue activity from one screen
          </h2>
          <p className="mt-3 max-w-2xl text-sm leading-7 text-slate-600 sm:text-base">
            Use the left panel to create a new report request, then follow the
            most recent jobs from the activity stream on the right.
          </p>
        </div>

        <div className="flex flex-wrap gap-3">
          <Link
            href="/reports"
            className="inline-flex items-center justify-center rounded-full border border-slate-200 bg-white px-5 py-3 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
          >
            Open history
          </Link>
          <div className="inline-flex items-center justify-center rounded-full bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_100%)] px-5 py-3 text-sm font-semibold text-white shadow-[0_16px_28px_rgba(37,99,235,0.28)]">
            Create new report
          </div>
        </div>
      </div>

      <div className="mt-8 grid gap-4 md:grid-cols-3">
        <WorkspaceMetricCard
          label="Recent jobs"
          value={String(recentCount)}
          accent="blue"
        />
        <WorkspaceMetricCard
          label="Completed"
          value={String(completedCount)}
          accent="emerald"
        />
        <WorkspaceMetricCard
          label="Attention needed"
          value={String(attentionCount)}
          accent="amber"
        />
      </div>
    </section>
  );
}
