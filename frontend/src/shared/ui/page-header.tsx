interface PageHeaderProps {
  title: string;
  subtitle: string;
}

export function PageHeader({ title, subtitle }: PageHeaderProps) {
  return (
    <section className="rounded-[28px] border border-slate-200/70 bg-[linear-gradient(135deg,#0f172a_0%,#1d4ed8_58%,#38bdf8_100%)] px-6 py-8 text-white shadow-[0_24px_60px_rgba(15,23,42,0.18)] sm:px-8">
      <div className="max-w-3xl">
        <p className="text-xs font-semibold uppercase tracking-[0.28em] text-sky-100/90">
          Report Workspace
        </p>
        <h2 className="mt-3 text-3xl font-semibold tracking-tight sm:text-4xl">
          {title}
        </h2>
        <p className="mt-3 max-w-2xl text-sm leading-6 text-sky-50/90 sm:text-base">
          {subtitle}
        </p>
      </div>
    </section>
  );
}
