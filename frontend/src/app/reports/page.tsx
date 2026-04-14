"use client";

import Link from "next/link";
import { Suspense, useEffect, useState } from "react";
import { useRouter, useSearchParams } from "next/navigation";

import { ApiError, getReportPage } from "@/entities/report/api/report-api";
import type { ReportPage } from "@/entities/report/model/types";
import { ReportList } from "@/features/report-list/ui/report-list";

const PAGE_SIZE = 10;

export default function AllReportsPage() {
  return (
    <Suspense fallback={<div className="rounded-[28px] border border-slate-200/70 bg-white/80 p-8 text-sm text-slate-500">Loading report history...</div>}>
      <AllReportsPageContent />
    </Suspense>
  );
}

function AllReportsPageContent() {
  const router = useRouter();
  const searchParams = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("search") ?? "");
  const [search, setSearch] = useState(searchParams.get("search") ?? "");
  const [statusFilter, setStatusFilter] = useState(searchParams.get("status") ?? "");
  const [pageIndex, setPageIndex] = useState(0);
  const [data, setData] = useState<ReportPage | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const urlSearch = searchParams.get("search") ?? "";
    const urlStatus = searchParams.get("status") ?? "";

    setQuery(urlSearch);
    setSearch(urlSearch);
    setStatusFilter(urlStatus);
    setPageIndex(0);
  }, [searchParams]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);

    getReportPage({
      search: search || undefined,
      status: statusFilter || undefined,
      limit: PAGE_SIZE,
      offset: pageIndex * PAGE_SIZE,
    })
      .then((response) => {
        if (!cancelled) {
          setData(response);
        }
      })
      .catch((reason: unknown) => {
        if (!cancelled) {
          setData(null);
          setError(reason instanceof ApiError ? reason.message : String(reason));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, [pageIndex, search, statusFilter]);

  const total = data?.total ?? 0;
  const totalPages = Math.max(1, Math.ceil(total / PAGE_SIZE));
  const from = total === 0 ? 0 : pageIndex * PAGE_SIZE + 1;
  const to = total === 0 ? 0 : Math.min((pageIndex + 1) * PAGE_SIZE, total);
  const pageNumbers = buildVisiblePageNumbers(pageIndex, totalPages);

  function submitSearch(event: React.FormEvent) {
    event.preventDefault();
    syncUrl(query.trim(), statusFilter);
  }

  function syncUrl(nextSearch: string, nextStatus: string) {
    const params = new URLSearchParams(searchParams.toString());

    if (nextSearch) {
      params.set("search", nextSearch);
    } else {
      params.delete("search");
    }

    if (nextStatus) {
      params.set("status", nextStatus);
    } else {
      params.delete("status");
    }

    router.push(`/reports${params.toString() ? `?${params.toString()}` : ""}`);
  }

  const statusPills = [
    { value: "", label: "All" },
    { value: "processing", label: "Processing" },
    { value: "completed", label: "Completed" },
    { value: "failed", label: "Failed" },
  ];

  return (
    <div className="space-y-8">
      <section className="rounded-[30px] border border-slate-200/70 bg-[linear-gradient(135deg,rgba(255,255,255,0.94)_0%,rgba(239,246,255,0.92)_56%,rgba(254,242,242,0.9)_100%)] p-6 shadow-[0_22px_55px_rgba(15,23,42,0.06)] sm:p-8">
        <div className="flex flex-col gap-6 xl:flex-row xl:items-end xl:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.26em] text-blue-600">
              Archive
            </p>
            <h2 className="mt-3 text-3xl font-semibold tracking-tight text-slate-950 sm:text-4xl">
              Report history in a card-based review flow
            </h2>
            <p className="mt-3 max-w-3xl text-sm leading-7 text-slate-600 sm:text-base">
              Filter by job state, search by customer or manager, and open completed reports from a cleaner operational list.
            </p>
          </div>

          <div className="flex flex-wrap gap-3">
            {statusPills.map((item) => (
              <button
                key={item.value || "all"}
                type="button"
                onClick={() => {
                  syncUrl(search, item.value);
                }}
                className={[
                  "rounded-full border px-5 py-3 text-sm font-semibold transition",
                  statusFilter === item.value
                    ? "border-blue-600 bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_100%)] text-white shadow-[0_14px_24px_rgba(37,99,235,0.24)]"
                    : "border-slate-200 bg-white text-slate-700 hover:border-slate-300 hover:bg-slate-50",
                ].join(" ")}
              >
                {item.label}
              </button>
            ))}
          </div>
        </div>
      </section>

      <ReportList
        fetched={!loading}
        reports={data?.items ?? []}
        eyebrow="History"
        title="Operational archive"
        subtitle="Move through the archive page by page and open any completed report from the list below."
        countLabel={total === 0 ? "0 items" : `${from}-${to} of ${total}`}
        emptyMessage={error ?? "No reports matched the current search."}
        headerAction={(
          <Link
            href="/"
            className="inline-flex items-center rounded-full border border-slate-200 bg-white px-3 py-1.5 text-xs font-semibold text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50"
          >
            Back to workspace
          </Link>
        )}
        footer={(
          <div className="flex flex-col gap-4 lg:flex-row lg:items-end lg:justify-between">
            <form className="flex flex-1 flex-col gap-3 sm:flex-row sm:items-end" onSubmit={submitSearch}>
              <label className="flex-1">
                <span className="mb-2 block text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                  Search
                </span>
                <input
                  value={query}
                  onChange={(event) => setQuery(event.target.value)}
                  placeholder="Customer ID, customer name, manager, service, or plan"
                  className="w-full rounded-[22px] border border-slate-200 bg-white px-4 py-3 text-sm text-slate-900 outline-none transition focus:border-sky-400 focus:ring-4 focus:ring-sky-100"
                />
              </label>
              <button
                type="submit"
                className="inline-flex h-[50px] items-center justify-center rounded-[22px] bg-[linear-gradient(135deg,#1d4ed8_0%,#2563eb_100%)] px-5 text-sm font-semibold text-white shadow-[0_16px_35px_rgba(37,99,235,0.24)] transition hover:brightness-105"
              >
                Apply search
              </button>
            </form>

            <div className="flex flex-col gap-3 sm:items-end">
              <div className="text-xs font-semibold uppercase tracking-[0.22em] text-slate-500">
                Page Navigation
              </div>
              <div className="flex flex-wrap items-center gap-2">
                <button
                  type="button"
                  disabled={pageIndex === 0}
                  onClick={() => setPageIndex((current) => Math.max(0, current - 1))}
                  className="inline-flex h-[50px] items-center justify-center rounded-2xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
                >
                  Previous
                </button>
                {pageNumbers.map((pageNumber, index) =>
                  pageNumber === "ellipsis" ? (
                    <span
                      key={`ellipsis-${index}`}
                      className="inline-flex h-[50px] items-center justify-center px-2 text-sm font-semibold text-slate-400"
                    >
                      ...
                    </span>
                  ) : (
                    <button
                      key={pageNumber}
                      type="button"
                      onClick={() => setPageIndex(pageNumber)}
                      aria-current={pageIndex === pageNumber ? "page" : undefined}
                      className={[
                        "inline-flex h-[50px] min-w-[50px] items-center justify-center rounded-2xl border px-4 text-sm font-semibold transition",
                        pageIndex === pageNumber
                          ? "border-slate-950 bg-slate-950 text-white shadow-[0_16px_35px_rgba(15,23,42,0.18)]"
                          : "border-slate-200 bg-white text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50",
                      ].join(" ")}
                    >
                      {pageNumber + 1}
                    </button>
                  ),
                )}
                <button
                  type="button"
                  disabled={pageIndex + 1 >= totalPages}
                  onClick={() => setPageIndex((current) => current + 1)}
                  className="inline-flex h-[50px] items-center justify-center rounded-2xl border border-slate-200 bg-white px-4 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50 disabled:cursor-not-allowed disabled:opacity-45"
                >
                  Next
                </button>
              </div>
            </div>
          </div>
        )}
      />
    </div>
  );
}

function buildVisiblePageNumbers(
  currentPage: number,
  totalPages: number,
): Array<number | "ellipsis"> {
  if (totalPages <= 7) {
    return Array.from({ length: totalPages }, (_, index) => index);
  }

  if (currentPage <= 3) {
    return [0, 1, 2, 3, 4, "ellipsis", totalPages - 1];
  }

  if (currentPage >= totalPages - 4) {
    return [0, "ellipsis", totalPages - 5, totalPages - 4, totalPages - 3, totalPages - 2, totalPages - 1];
  }

  return [
    0,
    "ellipsis",
    currentPage - 1,
    currentPage,
    currentPage + 1,
    "ellipsis",
    totalPages - 1,
  ];
}
