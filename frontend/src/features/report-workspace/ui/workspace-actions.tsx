import Link from "next/link";

interface WorkspaceActionsProps {
  onRefresh: () => void;
}

export function WorkspaceActions({ onRefresh }: WorkspaceActionsProps) {
  return (
    <section className="flex flex-col gap-3 sm:flex-row sm:items-center sm:justify-between">
      <div>
        <p className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
          Workspace Actions
        </p>
        <p className="mt-2 text-sm leading-6 text-slate-500">
          Jump into the full report history or refresh the latest operational view.
        </p>
      </div>
      <div className="flex flex-wrap items-center gap-3">
        <Link
          href="/reports"
          className="inline-flex items-center rounded-2xl bg-slate-950 px-4 py-2.5 text-sm font-semibold text-white shadow-[0_16px_35px_rgba(15,23,42,0.18)] transition hover:bg-slate-800"
        >
          Open report history
        </Link>
        <button
          type="button"
          onClick={onRefresh}
          className="inline-flex items-center rounded-2xl border border-slate-200 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
        >
          Refresh recent reports
        </button>
      </div>
    </section>
  );
}
