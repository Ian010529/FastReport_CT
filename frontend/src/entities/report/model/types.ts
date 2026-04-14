export interface ReportSummary {
  id: number;
  customerId: string;
  customerName: string;
  status: string;
  createdAt: string;
}

export interface ReportDetail extends ReportSummary {
  nationalId: string;
  managerName: string;
  managerId: string;
  serviceCode: string;
  currentPlan: string;
  reportContent: string | null;
}

export interface ReportPage {
  items: ReportSummary[];
  total: number;
  limit: number;
  offset: number;
}

export interface ReportFormValues {
  customerId: string;
  customerName: string;
  nationalId: string;
  managerName: string;
  managerId: string;
  serviceCode: string;
  currentPlan: string;
  additionalServices: string;
  spendingLast6: string;
  complaintHistory: string;
  networkQuality: string;
  overrideReason: string;
}

export interface CreateReportPayload {
  customerId: string;
  customerName: string;
  nationalId: string;
  managerName: string;
  managerId: string;
  serviceCode: string;
  currentPlan: string;
  additionalServices: string[];
  spendingLast6: number[];
  complaintHistory: string[];
  networkQuality: string;
  overrideReason?: string;
}

export interface CreateReportResponse {
  id: number;
  status: string;
  message: string;
}

export interface AppErrorPayload {
  type: string;
  code: string;
  message: string;
  detail: Record<string, unknown>;
  httpStatus: number;
}

export interface ReportResultEvent {
  reportId: number;
  status: string;
  reportContent: string | null;
}
