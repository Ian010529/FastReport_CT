import type { Metadata } from "next";
import { Suspense } from "react";
import "./globals.css";

import { AppSidebar } from "@/shared/ui/app-sidebar";
import { GlobalSearch } from "@/shared/ui/global-search";
import { TopNav } from "@/shared/ui/top-nav";

export const metadata: Metadata = {
  title: "FastReport",
  description: "Generate, review, and track customer reports.",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="en">
      <body>
        <div className="min-h-screen px-4 py-4 sm:px-6 lg:px-8">
          <div className="mx-auto max-w-[1600px]">
            <header className="app-shell rounded-[30px] border border-white/70 px-4 py-4 shadow-[0_24px_70px_rgba(15,23,42,0.08)] sm:px-6">
              <div className="flex flex-col gap-4 xl:flex-row xl:items-center xl:justify-between">
                <div className="flex flex-1 items-center gap-4">
                  <div className="hidden min-w-[220px] xl:block">
                    <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                      Report Operations Suite
                    </p>
                    <h1 className="mt-1 text-xl font-semibold tracking-tight text-slate-950">
                      FastReport Dashboard
                    </h1>
                  </div>
                  <Suspense fallback={<div className="flex-1 rounded-[24px] border border-slate-200/80 bg-white px-4 py-3 shadow-sm" />}>
                    <GlobalSearch />
                  </Suspense>
                </div>

                <div className="flex flex-wrap items-center gap-3">
                  <TopNav />
                  <div className="hidden items-center gap-2 rounded-full border border-slate-200/80 bg-white px-4 py-2 text-sm font-medium text-slate-600 shadow-sm sm:flex">
                    <span className="h-2.5 w-2.5 rounded-full bg-emerald-500" />
                    Queue online
                  </div>
                  <div className="flex items-center gap-3 rounded-[24px] border border-slate-200/80 bg-white px-3 py-2 shadow-sm">
                    <div className="flex h-11 w-11 items-center justify-center rounded-2xl bg-[linear-gradient(135deg,#fde68a_0%,#fb7185_100%)] text-sm font-semibold text-slate-950">
                      FR
                    </div>
                    <div className="hidden sm:block">
                      <div className="text-sm font-semibold text-slate-900">FastReport Ops</div>
                      <div className="text-xs text-slate-500">queue@fastreport.local</div>
                    </div>
                  </div>
                </div>
              </div>
            </header>

            <main className="mt-5 grid gap-5 xl:grid-cols-[260px_minmax(0,1fr)]">
              <div className="xl:sticky xl:top-5 xl:self-start">
                <AppSidebar />
              </div>
              <div className="app-shell rounded-[32px] border border-white/70 p-4 shadow-[0_30px_80px_rgba(15,23,42,0.08)] sm:p-6 lg:p-8">
                {children}
              </div>
            </main>
          </div>
        </div>
      </body>
    </html>
  );
}
