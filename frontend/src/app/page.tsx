"use client";

import { useEffect, useState } from "react";

import { getReportPage } from "@/entities/report/api/report-api";
import type { ReportPage } from "@/entities/report/model/types";
import { ReportForm } from "@/features/report-form/ui/report-form";
import { useReportWorkspace } from "@/features/report-workspace/model/use-report-workspace";
import { RecentReportsPanel } from "@/features/report-workspace/ui/recent-reports-panel";
import { WorkspaceHero } from "@/features/report-workspace/ui/workspace-hero";

export default function Home() {
  const {
    form,
    loading,
    liveMessage,
    submissionFeedback,
    highlightedFields,
    setField,
    submit,
  } = useReportWorkspace();
  const [reportPage, setReportPage] = useState<ReportPage | null>(null);
  const [loadingReports, setLoadingReports] = useState(true);

  useEffect(() => {
    let cancelled = false;

    getReportPage({ limit: 6, offset: 0 })
      .then((response) => {
        if (!cancelled) {
          setReportPage(response);
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoadingReports(false);
        }
      });

    return () => {
      cancelled = true;
    };
  }, []);

  const reports = reportPage?.items ?? [];
  const completedCount = reports.filter((report) => report.status === "completed").length;
  const processingCount = reports.filter((report) => report.status === "processing").length;
  const failedCount = reports.filter((report) => report.status === "failed").length;

  return (
    <div className="space-y-8">
      <WorkspaceHero
        recentCount={reports.length}
        completedCount={completedCount}
        attentionCount={failedCount + processingCount}
      />

      <div className="grid gap-6 2xl:grid-cols-[minmax(0,1.2fr)_minmax(360px,0.8fr)]">
        <ReportForm
          form={form}
          feedback={submissionFeedback}
          highlightedFields={highlightedFields}
          loading={loading}
          liveMessage={liveMessage}
          onChange={setField}
          onSubmit={submit}
        />

        <RecentReportsPanel loading={loadingReports} reports={reports} />
      </div>
    </div>
  );
}
