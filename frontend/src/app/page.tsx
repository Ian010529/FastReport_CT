"use client";

import { useEffect, useState } from "react";

import {
  ApiError,
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

type FeedbackTone = "error" | "warning" | "success";

interface SubmissionFeedback {
  tone: FeedbackTone;
  title: string;
  message: string;
  details: string[];
}

export default function Home() {
  const [form, setForm] = useState<ReportFormValues>(DEFAULT_REPORT_FORM);
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState<ReportSummary[]>([]);
  const [fetched, setFetched] = useState(false);
  const [activeReportId, setActiveReportId] = useState<number | null>(null);
  const [liveMessage, setLiveMessage] = useState<string | null>(null);
  const [submissionFeedback, setSubmissionFeedback] = useState<SubmissionFeedback | null>(null);
  const [highlightedFields, setHighlightedFields] = useState<(keyof ReportFormValues)[]>([]);

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
    {
      setForm({ ...form, [k]: e.target.value });
      setSubmissionFeedback(null);
      setHighlightedFields((current) => current.filter((field) => field !== k));
    };

  async function fetchReports() {
    const data = await getReports();
    setReports(data);
    setFetched(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    setSubmissionFeedback(null);
    setHighlightedFields([]);
    try {
      const created = await createReport(buildCreateReportPayload(form));
      setActiveReportId(created.id);
      setLiveMessage(`Report #${created.id} submitted. Opening SSE channel...`);
      setSubmissionFeedback({
        tone: "success",
        title: "Report submitted",
        message: created.message,
        details: [],
      });
      await fetchReports();
    } catch (err: unknown) {
      const feedback = buildSubmissionFeedback(err);
      setSubmissionFeedback(feedback);
      setHighlightedFields(getHighlightedFields(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-8">
      <ReportForm
        form={form}
        feedback={submissionFeedback}
        highlightedFields={highlightedFields}
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

function buildSubmissionFeedback(error: unknown): SubmissionFeedback {
  if (!(error instanceof ApiError)) {
    return {
      tone: "error",
      title: "Unexpected error",
      message: error instanceof Error ? error.message : String(error),
      details: [],
    };
  }

  if (error.type === "VALIDATION_ERROR") {
    return {
      tone: "error",
      title: "Input validation failed",
      message: error.message,
      details: extractDetails(error),
    };
  }

  if (error.type === "WARNING") {
    return {
      tone: "warning",
      title: "Confirmation required",
      message: error.message,
      details: extractDetails(error),
    };
  }

  if (error.type === "BLOCK") {
    return {
      tone: "error",
      title: "Request blocked by business rules",
      message: error.message,
      details: extractDetails(error),
    };
  }

  return {
    tone: "error",
    title: error.type,
    message: error.message,
    details: extractDetails(error),
  };
}

function extractDetails(error: ApiError): string[] {
  const warnings = error.detail.warnings;
  if (Array.isArray(warnings)) {
    return warnings.filter((item): item is string => typeof item === "string");
  }

  const fieldErrors = error.detail.fieldErrors;
  if (Array.isArray(fieldErrors)) {
    return fieldErrors
      .map((item) => {
        if (!item || typeof item !== "object") {
          return null;
        }
        const field = "field" in item ? item.field : null;
        const message = "message" in item ? item.message : null;
        return typeof field === "string" && typeof message === "string"
          ? `${field}: ${message}`
          : null;
      })
      .filter((item): item is string => item !== null);
  }

  return [];
}

function getHighlightedFields(error: unknown): (keyof ReportFormValues)[] {
  if (!(error instanceof ApiError)) {
    return [];
  }

  switch (error.code) {
    case "INVALID_CUSTOMER_ID":
      return ["customerId"];
    case "INVALID_MANAGER_ID":
      return ["managerId"];
    case "INVALID_NATIONAL_ID":
      return ["nationalId"];
    case "INVALID_SERVICE_CODE":
      return ["serviceCode"];
    case "INVALID_SPENDING_LAST6":
      return ["spendingLast6"];
    case "INVALID_ADDITIONAL_SERVICES":
      return ["additionalServices"];
    case "INVALID_COMPLAINT_HISTORY":
      return ["complaintHistory"];
    case "OVERRIDE_REASON_REQUIRED":
      return ["overrideReason"];
    case "CUSTOMER_ID_NATIONAL_ID_CONFLICT":
      return ["customerId", "nationalId"];
    case "MANAGER_ID_NAME_CONFLICT":
      return ["managerId", "managerName"];
    case "DUPLICATE_REPORT_SAME_DAY":
      return ["customerId", "serviceCode"];
    default:
      return [];
  }
}
