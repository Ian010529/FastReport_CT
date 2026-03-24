import type { ReportFormValues } from "@/entities/report/model/types";

export const DEFAULT_REPORT_FORM: ReportFormValues = {
  customerId: "10000001",
  customerName: "张三",
  nationalId: "110101199003077758",
  managerName: "李经理",
  managerId: "200001",
  serviceCode: "FTTH_500M",
  currentPlan: "畅享融合 199 套餐",
  additionalServices: "天翼云盘,天翼高清",
  spendingLast6: "199,199,210,185,199,220",
  complaintHistory: "2024-12 宽带网速慢,2025-01 客服响应时间过长",
  networkQuality: "下载速率偶尔低于签约带宽的 50%",
  overrideReason: "",
};
