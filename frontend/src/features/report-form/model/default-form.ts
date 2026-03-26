import type { ReportFormValues } from "@/entities/report/model/types";

export const DEFAULT_REPORT_FORM: ReportFormValues = {
  customerId: "10000001",
  customerName: "Alex Chen",
  nationalId: "110101199003077758",
  managerName: "Olivia Park",
  managerId: "200001",
  serviceCode: "FTTH_500M",
  currentPlan: "Fiber Plus 199",
  additionalServices: "Cloud Storage,Smart TV",
  spendingLast6: "199,199,210,185,199,220",
  complaintHistory: "2024-12 Slow broadband speed,2025-01 Delayed customer service response",
  networkQuality: "Download speed occasionally drops below 50% of the subscribed bandwidth.",
  overrideReason: "",
};
