"use client";

import { useEffect, useState } from "react";

import {
  createReport,
  getReportById,
  getReportEventsUrl,
} from "@/entities/report/api/report-api";
import type {
  ReportFormValues,
  ReportResultEvent,
} from "@/entities/report/model/types";
import { DEFAULT_REPORT_FORM } from "@/features/report-form/model/default-form";
import { buildCreateReportPayload } from "@/features/report-form/model/serializers";
import {
  buildSubmissionFeedback,
  getHighlightedFields,
  readableStatus,
  type SubmissionFeedback,
} from "@/features/report-workspace/model/submission-feedback";
import { APP_RUNTIME, WS_BASE_URL } from "@/shared/config/api";

const POLLING_INTERVAL_MS = 3000;

export function useReportWorkspace() {
  const [form, setForm] = useState<ReportFormValues>(DEFAULT_REPORT_FORM);
  const [loading, setLoading] = useState(false);
  const [activeReportId, setActiveReportId] = useState<number | null>(null);
  const [liveMessage, setLiveMessage] = useState<string | null>(null);
  const [submissionFeedback, setSubmissionFeedback] = useState<SubmissionFeedback | null>(null);
  const [highlightedFields, setHighlightedFields] = useState<(keyof ReportFormValues)[]>([]);

  useEffect(() => {
    if (activeReportId == null) return;

    if (APP_RUNTIME === "aws") {
      let stopped = false;
      let pollTimer: ReturnType<typeof setInterval> | null = null;
      let socket: WebSocket | null = null;

      const pollOnce = async () => {
        try {
          const report = await getReportById(String(activeReportId));
          setLiveMessage(`Report #${activeReportId} is now ${readableStatus(report.status)}.`);
          if (report.status === "completed" || report.status === "failed") {
            setActiveReportId(null);
          }
        } catch {
          setLiveMessage(`Checking report #${activeReportId} status...`);
        }
      };

      const startPolling = () => {
        if (pollTimer != null || stopped) return;
        setLiveMessage(`Waiting for report #${activeReportId}. Falling back to status checks.`);
        void pollOnce();
        pollTimer = setInterval(() => {
          void pollOnce();
        }, POLLING_INTERVAL_MS);
      };

      if (!WS_BASE_URL) {
        startPolling();
      } else {
        try {
          socket = new WebSocket(WS_BASE_URL);

          socket.onopen = () => {
            socket?.send(JSON.stringify({
              action: "subscribeReport",
              reportId: activeReportId,
            }));
            setLiveMessage(`Report #${activeReportId} is connected for AWS live updates.`);
          };

          socket.onmessage = (event) => {
            const data = JSON.parse(event.data) as ReportResultEvent;
            if (data.reportId !== activeReportId) return;
            setLiveMessage(`Report #${data.reportId} is now ${readableStatus(data.status)}.`);
            stopped = true;
            setActiveReportId(null);
            socket?.close();
          };

          socket.onerror = () => {
            socket?.close();
            startPolling();
          };

          socket.onclose = () => {
            if (!stopped && activeReportId != null) {
              startPolling();
            }
          };
        } catch {
          startPolling();
        }
      }

      return () => {
        stopped = true;
        if (pollTimer != null) {
          clearInterval(pollTimer);
        }
        socket?.close();
      };
    }

    const source = new EventSource(getReportEventsUrl(activeReportId));

    source.addEventListener("connected", () => {
      setLiveMessage(`Report #${activeReportId} is connected for live status updates.`);
    });

    source.addEventListener("report-result", async (event) => {
      const data = JSON.parse((event as MessageEvent).data) as ReportResultEvent;
      setLiveMessage(`Report #${data.reportId} is now ${readableStatus(data.status)}.`);
      setActiveReportId(null);
      source.close();
    });

    source.onerror = () => {
      setLiveMessage(`Live updates were interrupted for report #${activeReportId}.`);
      source.close();
    };

    return () => {
      source.close();
    };
  }, [activeReportId]);

  function setField(
    field: keyof ReportFormValues,
  ) {
    return (event: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
      setForm((current) => ({ ...current, [field]: event.target.value }));
      setSubmissionFeedback(null);
      setHighlightedFields((current) => current.filter((item) => item !== field));
    };
  }

  async function submit(event: React.FormEvent) {
    event.preventDefault();
    setLoading(true);
    setSubmissionFeedback(null);
    setHighlightedFields([]);

    try {
      const created = await createReport(buildCreateReportPayload(form));
      setActiveReportId(created.id);
      setLiveMessage(`Report #${created.id} was submitted successfully.`);
      setSubmissionFeedback({
        tone: "success",
        title: "Report submitted",
        message: created.message,
        details: [],
      });
    } catch (error: unknown) {
      setSubmissionFeedback(buildSubmissionFeedback(error));
      setHighlightedFields(getHighlightedFields(error));
    } finally {
      setLoading(false);
    }
  }

  return {
    form,
    loading,
    liveMessage,
    submissionFeedback,
    highlightedFields,
    setField,
    submit,
  };
}
