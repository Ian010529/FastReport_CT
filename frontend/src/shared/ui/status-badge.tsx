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
      className={`rounded-full px-2 py-0.5 text-xs font-medium ${
        REPORT_STATUS_STYLES[status] || "bg-gray-100"
      }`}
    >
      {REPORT_STATUS_LABELS[status] || status}
    </span>
  );
}
