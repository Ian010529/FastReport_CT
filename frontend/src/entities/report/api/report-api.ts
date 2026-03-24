import { API_BASE_URL } from "@/shared/config/api";
import type {
  AppErrorPayload,
  CreateReportPayload,
  CreateReportResponse,
  ReportDetail,
  ReportSummary,
} from "@/entities/report/model/types";

export class ApiError extends Error {
  type: string;
  code: string;
  detail: Record<string, unknown>;
  httpStatus: number;

  constructor(payload: AppErrorPayload) {
    super(payload.message);
    this.name = "ApiError";
    this.type = payload.type;
    this.code = payload.code;
    this.detail = payload.detail;
    this.httpStatus = payload.httpStatus;
  }
}

export async function getReports(): Promise<ReportSummary[]> {
  const response = await fetch(`${API_BASE_URL}/api/reports`);

  if (!response.ok) {
    throw await toApiError(response);
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
    throw await toApiError(response);
  }

  return response.json();
}

export async function getReportById(id: string): Promise<ReportDetail> {
  const response = await fetch(`${API_BASE_URL}/api/reports/${id}`);

  if (!response.ok) {
    throw await toApiError(response);
  }

  return response.json();
}

export function getReportEventsUrl(reportId: number): string {
  return `${API_BASE_URL}/api/reports/${reportId}/events`;
}

export function getReportDownloadUrl(id: string, format: string): string {
  return `${API_BASE_URL}/api/reports/${id}/download?format=${format}`;
}

async function toApiError(response: Response): Promise<ApiError> {
  try {
    const payload = (await response.json()) as Partial<AppErrorPayload>;
    if (payload.message && payload.type && payload.code) {
      return new ApiError({
        type: payload.type,
        code: payload.code,
        message: payload.message,
        detail: payload.detail ?? {},
        httpStatus: payload.httpStatus ?? response.status,
      });
    }
  } catch {
    // Ignore JSON parsing issues and fall back to a generic error payload.
  }

  return new ApiError({
    type: "HTTP_ERROR",
    code: "UNEXPECTED_HTTP_ERROR",
    message: `HTTP ${response.status}`,
    detail: {},
    httpStatus: response.status,
  });
}
