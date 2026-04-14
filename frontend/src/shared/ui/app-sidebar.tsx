"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";

const NAV_ITEMS = [
  {
    href: "/",
    label: "Workspace",
    description: "Create and monitor",
    icon: HomeIcon,
  },
  {
    href: "/reports",
    label: "History",
    description: "Browse reports",
    icon: ArchiveIcon,
  },
];

export function AppSidebar() {
  const pathname = usePathname();

  return (
    <aside className="flex h-full min-h-[680px] w-full flex-col rounded-[30px] border border-white/70 bg-[linear-gradient(180deg,rgba(255,255,255,0.9)_0%,rgba(241,245,249,0.96)_100%)] p-4 shadow-[0_24px_60px_rgba(15,23,42,0.08)]">
      <div className="flex items-center gap-3 rounded-[24px] bg-white/90 px-4 py-4 shadow-sm">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_60%,#60a5fa_100%)] text-white shadow-[0_12px_24px_rgba(37,99,235,0.28)]">
          <LogoIcon />
        </div>
        <div>
          <div className="text-lg font-semibold tracking-tight text-slate-950">
            FastReport
          </div>
          <div className="text-xs font-medium text-slate-500">
            Report operations
          </div>
        </div>
      </div>

      <nav className="mt-6 space-y-2">
        {NAV_ITEMS.map((item) => {
          const active = pathname === item.href;
          const Icon = item.icon;

          return (
            <Link
              key={item.href}
              href={item.href}
              className={[
                "flex items-center gap-3 rounded-[22px] px-4 py-3 transition",
                active
                  ? "bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_100%)] text-white shadow-[0_18px_30px_rgba(37,99,235,0.28)]"
                  : "text-slate-700 hover:bg-white hover:text-slate-950",
              ].join(" ")}
            >
              <span
                className={[
                  "flex h-11 w-11 items-center justify-center rounded-2xl border",
                  active
                    ? "border-white/20 bg-white/10"
                    : "border-slate-200 bg-white",
                ].join(" ")}
              >
                <Icon />
              </span>
              <span className="min-w-0">
                <span className="block text-sm font-semibold">{item.label}</span>
                <span
                  className={[
                    "block text-xs",
                    active ? "text-blue-100" : "text-slate-500",
                  ].join(" ")}
                >
                  {item.description}
                </span>
              </span>
            </Link>
          );
        })}
      </nav>

      <div className="mt-auto rounded-[24px] border border-slate-200/80 bg-white/90 p-4">
        <div className="flex items-center gap-3">
          <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-slate-950 text-white">
            <SettingsIcon />
          </div>
          <div>
            <div className="text-sm font-semibold text-slate-900">System Mode</div>
            <div className="text-xs text-slate-500">Queue-backed generation</div>
          </div>
        </div>
        <p className="mt-3 text-xs leading-5 text-slate-500">
          Keep submissions in the workspace and use history when you need to inspect finished reports.
        </p>
      </div>
    </aside>
  );
}

function LogoIcon() {
  return (
    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <rect x="4" y="3.5" width="16" height="17" rx="3" fill="currentColor" opacity="0.18" />
      <path
        d="M8 8.25C8 7.56 8.56 7 9.25 7h5.5C15.44 7 16 7.56 16 8.25v7.5c0 .69-.56 1.25-1.25 1.25h-5.5C8.56 17 8 16.44 8 15.75v-7.5Zm1.75 1.25v5h4.5v-5h-4.5Z"
        fill="currentColor"
      />
    </svg>
  );
}

function HomeIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M4.75 10.25 12 4.75l7.25 5.5v8A1.75 1.75 0 0 1 17.5 20h-11a1.75 1.75 0 0 1-1.75-1.75v-8Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ArchiveIcon() {
  return (
    <svg width="22" height="22" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M5 8.25h14M6.5 4.75h11A1.75 1.75 0 0 1 19.25 6.5v11A1.75 1.75 0 0 1 17.5 19.25h-11A1.75 1.75 0 0 1 4.75 17.5v-11A1.75 1.75 0 0 1 6.5 4.75Z"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinejoin="round"
      />
      <path d="M9 12h6" stroke="currentColor" strokeWidth="1.8" strokeLinecap="round" />
    </svg>
  );
}

function SettingsIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <path
        d="M12 8.5a3.5 3.5 0 1 0 0 7 3.5 3.5 0 0 0 0-7Zm8 3.5-.9-.31a7.83 7.83 0 0 0-.56-1.35l.42-.85a1 1 0 0 0-.19-1.15l-1.38-1.38a1 1 0 0 0-1.15-.19l-.85.42c-.43-.23-.88-.42-1.35-.56L14 4a1 1 0 0 0-.97-.75h-2.06A1 1 0 0 0 10 4l-.31.9c-.47.14-.92.33-1.35.56l-.85-.42a1 1 0 0 0-1.15.19L4.96 6.61a1 1 0 0 0-.19 1.15l.42.85c-.23.43-.42.88-.56 1.35L3.75 12a1 1 0 0 0 0 2l.88.31c.14.47.33.92.56 1.35l-.42.85a1 1 0 0 0 .19 1.15l1.38 1.38a1 1 0 0 0 1.15.19l.85-.42c.43.23.88.42 1.35.56l.31.9a1 1 0 0 0 .97.75h2.06a1 1 0 0 0 .97-.75l.31-.9c.47-.14.92-.33 1.35-.56l.85.42a1 1 0 0 0 1.15-.19l1.38-1.38a1 1 0 0 0 .19-1.15l-.42-.85c.23-.43.42-.88.56-1.35l.9-.31a1 1 0 0 0 .75-.97v-2.06A1 1 0 0 0 20 12Z"
        stroke="currentColor"
        strokeWidth="1.5"
        strokeLinejoin="round"
      />
    </svg>
  );
}
