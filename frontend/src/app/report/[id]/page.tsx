"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";

import { getReportById } from "@/entities/report/api/report-api";
import type { ReportDetail } from "@/entities/report/model/types";
import { ReportDetailView } from "@/features/report-detail/ui/report-detail-view";
import { FeedbackBanner } from "@/shared/ui/feedback-banner";
import { SectionCard } from "@/shared/ui/section-card";

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
      <SectionCard>
        <FeedbackBanner
          tone="error"
          title="Unable to load report"
          message={error}
        />
      </SectionCard>
    );
  if (!report) {
    return (
      <SectionCard>
        <div className="text-sm text-slate-500">Loading report...</div>
      </SectionCard>
    );
  }

  return <ReportDetailView report={report} reportId={String(id)} />;
}
