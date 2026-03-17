"use client";

import { useEffect, useState } from "react";

import {
  createReport,
  getReportEventsUrl,
  getReports,
} from "@/entities/report/api/report-api";
import type {
  ReportFormValues,
  ReportResultEvent,
  ReportSummary,
} from "@/entities/report/model/types";
import { DEFAULT_REPORT_FORM } from "@/features/report-form/model/default-form";
import { buildCreateReportPayload } from "@/features/report-form/model/serializers";
import { ReportForm } from "@/features/report-form/ui/report-form";
import { ReportList } from "@/features/report-list/ui/report-list";

export default function Home() {
  const [form, setForm] = useState<ReportFormValues>(DEFAULT_REPORT_FORM);
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [fetched, setFetched] = useState(false);
  const [activeReportId, setActiveReportId] = useState<number | null>(null);
  const [liveMessage, setLiveMessage] = useState<string | null>(null);

  useEffect(() => {
    fetchReports();
  }, []); // 确保只在组件挂载时调用一次

  useEffect(() => {
    if (activeReportId == null) return;

    const source = new EventSource(getReportEventsUrl(activeReportId));

    source.addEventListener("connected", () => {
      setLiveMessage(`SSE connected for report #${activeReportId}. Waiting for worker result...`);
    });

    source.addEventListener("report-result", async (event) => {
      const data = JSON.parse((event as MessageEvent).data) as ReportResultEvent;
      setLiveMessage(`Report #${data.reportId} is now ${data.status}.`);
      setReports((current) =>
        current.map((report) =>
          report.id === data.reportId
            ? { ...report, status: data.status }
            : report
        )
      );
      await fetchReports();
      setActiveReportId(null);
      source.close();
    });

    source.onerror = () => {
      setLiveMessage(`SSE connection for report #${activeReportId} was interrupted.`);
      source.close();
    };

    return () => {
      source.close();
    };
  }, [activeReportId]);

  const set = (k: keyof ReportFormValues) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [k]: e.target.value });

  async function fetchReports() {
    const data = await getReports();
    setReports(data);
    setFetched(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const created = await createReport(buildCreateReportPayload(form));
      setActiveReportId(created.id);
      setLiveMessage(`Report #${created.id} submitted. Opening SSE channel...`);
      await fetchReports();
    } catch (err: unknown) {
      alert("生成失败: " + (err instanceof Error ? err.message : err));
    } finally {
      setLoading(false);
    }
  }

  const label = "block text-sm font-medium text-gray-700 mb-1";
  const input = "w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none";

  return (
    <div className="space-y-8">
      <ReportForm
        form={form}
        loading={loading}
        liveMessage={liveMessage}
        onChange={set}
        onRefresh={fetchReports}
        onSubmit={submit}
      />
      <ReportList fetched={fetched} reports={reports} />
    </div>
  );
}
