import {
  REPORT_STATUS_LABELS,
  REPORT_STATUS_STYLES,
} from "@/entities/report/model/status";

interface StatusBadgeProps {
  status: string;
}

export function StatusBadge({ status }: StatusBadgeProps) {
  return (
    <span
      className={`inline-flex items-center rounded-full px-2.5 py-1 text-xs font-semibold ${
        REPORT_STATUS_STYLES[status] || "border border-slate-200 bg-slate-100 text-slate-700"
      }`}
    >
      {REPORT_STATUS_LABELS[status] || status}
    </span>
  );
}
