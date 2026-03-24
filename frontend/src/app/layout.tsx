import type { Metadata } from "next";
import "./globals.css";

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
        <div className="min-h-screen">
          <header className="border-b border-white/60 bg-white/70">
            <div className="mx-auto flex max-w-7xl items-center justify-between px-5 py-4 sm:px-6 lg:px-8">
              <div>
                <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                  Report Operations Suite
                </p>
                <h1 className="mt-1 text-xl font-semibold tracking-tight text-slate-950">
                  FastReport
                </h1>
              </div>
              <div className="rounded-full border border-slate-200 bg-white px-3 py-1 text-xs font-medium text-slate-600 shadow-sm">
                Live queue-backed reporting
              </div>
            </div>
          </header>
          <main className="mx-auto max-w-7xl px-5 py-8 sm:px-6 lg:px-8">
            <div className="app-shell rounded-[28px] border border-white/70 p-4 shadow-[0_30px_80px_rgba(15,23,42,0.08)] sm:p-6 lg:p-8">
              {children}
            </div>
          </main>
        </div>
      </body>
    </html>
  );
}
