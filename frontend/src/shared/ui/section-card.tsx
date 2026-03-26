import type { ReactNode } from "react";

interface SectionCardProps {
  children: ReactNode;
  className?: string;
}

export function SectionCard({ children, className = "" }: SectionCardProps) {
  return (
    <section
      className={`rounded-[24px] border border-slate-200/80 bg-white/90 p-5 shadow-[0_16px_50px_rgba(15,23,42,0.06)] sm:p-6 ${className}`.trim()}
    >
      {children}
    </section>
  );
}
