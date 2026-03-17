import { API_BASE_URL } from "@/shared/config/api";
import type {
  CreateReportPayload,
  CreateReportResponse,
  ReportDetail,
  ReportSummary,
} from "@/entities/report/model/types";

export async function getReports(): Promise<ReportSummary[]> {
  const response = await fetch(`${API_BASE_URL}/api/reports`);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function createReport(
  payload: CreateReportPayload,
): Promise<CreateReportResponse> {
  const response = await fetch(`${API_BASE_URL}/api/reports`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(payload),
  });

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export async function getReportById(id: string): Promise<ReportDetail> {
  const response = await fetch(`${API_BASE_URL}/api/reports/${id}`);

  if (!response.ok) {
    throw new Error(`HTTP ${response.status}`);
  }

  return response.json();
}

export function getReportEventsUrl(reportId: number): string {
  return `${API_BASE_URL}/api/reports/${reportId}/events`;
}

export function getReportDownloadUrl(id: string, format: string): string {
  return `${API_BASE_URL}/api/reports/${id}/download?format=${format}`;
}
