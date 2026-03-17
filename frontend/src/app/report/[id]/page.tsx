"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

import { getReportById } from "@/entities/report/api/report-api";
import type { ReportDetail } from "@/entities/report/model/types";
import { ReportDetailView } from "@/features/report-detail/ui/report-detail-view";

export default function ReportDetail() {
  const { id } = useParams();
  const [report, setReport] = useState<ReportDetail | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getReportById(String(id))
      .then(setReport)
      .catch((e) => setError(e.message));
  }, [id]);

  if (error)
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded">Error: {error}</div>
    );
  if (!report) return <p className="text-gray-500">加载中…</p>;

  return <ReportDetailView report={report} reportId={String(id)} />;
}
