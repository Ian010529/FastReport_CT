"use client";

import { useEffect, useState } from "react";
import { usePathname, useRouter, useSearchParams } from "next/navigation";

export function GlobalSearch() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const [query, setQuery] = useState(searchParams.get("search") ?? "");

  useEffect(() => {
    setQuery(searchParams.get("search") ?? "");
  }, [searchParams]);

  function submitSearch(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const nextQuery = query.trim();
    const params = new URLSearchParams(searchParams.toString());

    if (nextQuery) {
      params.set("search", nextQuery);
    } else {
      params.delete("search");
    }

    if (pathname !== "/reports") {
      router.push(`/reports${params.toString() ? `?${params.toString()}` : ""}`);
      return;
    }

    router.push(`/reports${params.toString() ? `?${params.toString()}` : ""}`);
  }

  return (
    <form
      onSubmit={submitSearch}
      className="flex flex-1 items-center rounded-[24px] border border-slate-200/80 bg-white px-4 py-3 shadow-sm"
      role="search"
    >
      <SearchIcon />
      <input
        aria-label="Search reports"
        value={query}
        onChange={(event) => setQuery(event.target.value)}
        placeholder="Search customer, manager, service, or report ID"
        className="ml-3 w-full border-0 bg-transparent text-sm text-slate-900 outline-none placeholder:text-slate-400"
      />
    </form>
  );
}

function SearchIcon() {
  return (
    <svg width="20" height="20" viewBox="0 0 24 24" fill="none" aria-hidden="true">
      <circle
        cx="11"
        cy="11"
        r="6.5"
        stroke="currentColor"
        strokeWidth="1.8"
        className="text-slate-500"
      />
      <path
        d="m16 16 4 4"
        stroke="currentColor"
        strokeWidth="1.8"
        strokeLinecap="round"
        className="text-slate-500"
      />
    </svg>
  );
}
