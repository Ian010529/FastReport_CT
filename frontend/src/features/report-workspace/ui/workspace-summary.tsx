import type { ReportSummary } from "@/entities/report/model/types";

interface WorkspaceSummaryProps {
  reports: ReportSummary[];
}

export function WorkspaceSummary({ reports }: WorkspaceSummaryProps) {
  const completedCount = reports.filter((report) => report.status === "completed").length;
  const inProgressCount = reports.filter(
    (report) => report.status === "pending" || report.status === "processing",
  ).length;

  return (
    <section className="grid gap-4 xl:grid-cols-[1.15fr_0.85fr]">
      <div className="grid gap-4 sm:grid-cols-3 xl:col-span-2 xl:grid-cols-3">
        <SummaryCard
          label="Reports in view"
          value={String(reports.length)}
          description="Recently loaded records available in the workspace."
        />
        <SummaryCard
          label="Completed"
          value={String(completedCount)}
          description="Reports ready for review and download."
        />
        <SummaryCard
          label="In progress"
          value={String(inProgressCount)}
          description="Reports currently waiting in the queue or being generated."
        />
      </div>
    </section>
  );
}

function SummaryCard({
  label,
  value,
  description,
}: {
  label: string;
  value: string;
  description: string;
}) {
  return (
    <div className="rounded-[22px] border border-slate-200/80 bg-white/85 p-5 shadow-[0_12px_32px_rgba(15,23,42,0.05)]">
      <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
        {label}
      </div>
      <div className="mt-3 text-3xl font-semibold tracking-tight text-slate-950">
        {value}
      </div>
      <p className="mt-2 text-sm leading-6 text-slate-500">{description}</p>
    </div>
  );
}
