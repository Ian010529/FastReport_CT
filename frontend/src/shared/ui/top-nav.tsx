"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_ITEMS = [
  { href: "/", label: "Workspace" },
  { href: "/reports", label: "History" },
];

export function TopNav() {
  const pathname = usePathname();

  return (
    <nav className="flex items-center gap-1 rounded-full border border-slate-200/80 bg-white/90 p-1 shadow-sm">
      {NAV_ITEMS.map((item) => {
        const active = pathname === item.href;

        return (
          <Link
            key={item.href}
            href={item.href}
            className={[
              "rounded-full px-4 py-2 text-xs font-semibold transition",
              active
                ? "bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_100%)] text-white shadow-[0_10px_18px_rgba(37,99,235,0.25)]"
                : "text-slate-600 hover:bg-slate-50 hover:text-slate-900",
            ].join(" ")}
          >
            {item.label}
          </Link>
        );
      })}
    </nav>
  );
}
