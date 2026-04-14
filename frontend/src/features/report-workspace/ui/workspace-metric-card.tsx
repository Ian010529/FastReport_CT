interface WorkspaceMetricCardProps {
  label: string;
  value: string;
  accent: "blue" | "emerald" | "amber";
}

export function WorkspaceMetricCard({
  label,
  value,
  accent,
}: WorkspaceMetricCardProps) {
  const accentClassName = {
    blue: "bg-blue-50 text-blue-700",
    emerald: "bg-emerald-50 text-emerald-700",
    amber: "bg-amber-50 text-amber-700",
  }[accent];

  return (
    <div className="rounded-[24px] border border-white/70 bg-white/80 p-4 shadow-sm">
      <div
        className={`inline-flex rounded-full px-3 py-1 text-xs font-semibold ${accentClassName}`}
      >
        {label}
      </div>
      <div className="mt-4 text-3xl font-semibold tracking-tight text-slate-950">
        {value}
      </div>
    </div>
  );
}
