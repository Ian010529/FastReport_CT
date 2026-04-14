import { ApiError } from "@/entities/report/api/report-api";
import type { ReportFormValues } from "@/entities/report/model/types";

export type FeedbackTone = "error" | "warning" | "success";

export interface SubmissionFeedback {
  tone: FeedbackTone;
  title: string;
  message: string;
  details: string[];
}

export function buildSubmissionFeedback(error: unknown): SubmissionFeedback {
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
      message: "Please review the highlighted fields before submitting the report again.",
      details: extractDetails(error),
    };
  }

  if (error.type === "WARNING") {
    return {
      tone: "warning",
      title: "Confirmation required",
      message: "This request can continue only after you provide an override reason.",
      details: extractDetails(error),
    };
  }

  if (error.type === "BLOCK") {
    return {
      tone: "error",
      title: "Submission blocked",
      message: "This report conflicts with an existing business rule and could not be created.",
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

export function getHighlightedFields(error: unknown): (keyof ReportFormValues)[] {
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

export function readableStatus(status: string): string {
  switch (status) {
    case "pending":
      return "pending";
    case "processing":
      return "processing";
    case "completed":
      return "completed";
    case "failed":
      return "failed";
    default:
      return status;
  }
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
