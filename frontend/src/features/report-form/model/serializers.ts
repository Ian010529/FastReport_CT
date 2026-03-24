import type {
  CreateReportPayload,
  ReportFormValues,
} from "@/entities/report/model/types";

export function buildCreateReportPayload(
  form: ReportFormValues,
): CreateReportPayload {
  return {
    customerId: form.customerId,
    customerName: form.customerName,
    nationalId: form.nationalId,
    managerName: form.managerName,
    managerId: form.managerId,
    serviceCode: form.serviceCode,
    currentPlan: form.currentPlan,
    additionalServices: form.additionalServices
      ? form.additionalServices.split(",").map((service) => service.trim())
      : [],
    spendingLast6: form.spendingLast6
      ? form.spendingLast6
          .split(",")
          .map((amount) => Number.parseFloat(amount.trim()))
          .filter((amount) => !Number.isNaN(amount))
      : [],
    complaintHistory: form.complaintHistory
      ? form.complaintHistory.split(",").map((record) => record.trim())
      : [],
    networkQuality: form.networkQuality,
    overrideReason: form.overrideReason.trim() || undefined,
  };
}
