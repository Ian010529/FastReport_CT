"use client";

import type { ChangeEvent, FormEvent } from "react";

import type { ReportFormValues } from "@/entities/report/model/types";

const labelClassName = "mb-1 block text-sm font-medium text-gray-700";
const inputClassName =
  "w-full rounded-md border border-gray-300 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-blue-500";

interface ReportFormProps {
  form: ReportFormValues;
  loading: boolean;
  liveMessage: string | null;
  onChange: (
    field: keyof ReportFormValues,
  ) => (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onRefresh: () => Promise<void>;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
}

export function ReportForm({
  form,
  loading,
  liveMessage,
  onChange,
  onRefresh,
  onSubmit,
}: ReportFormProps) {
  return (
    <section className="rounded-lg bg-white p-6 shadow">
      <h2 className="mb-4 text-lg font-semibold">📝 新建报告</h2>
      {liveMessage && (
        <div className="mb-4 rounded-md border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
          {liveMessage}
        </div>
      )}
      <form onSubmit={onSubmit} className="grid grid-cols-1 gap-4 md:grid-cols-2">
        <div>
          <label className={labelClassName}>客户编号 (8位)</label>
          <input
            className={inputClassName}
            value={form.customerId}
            onChange={onChange("customerId")}
            required
          />
        </div>
        <div>
          <label className={labelClassName}>客户姓名</label>
          <input
            className={inputClassName}
            value={form.customerName}
            onChange={onChange("customerName")}
            required
          />
        </div>
        <div>
          <label className={labelClassName}>身份证号 (18位)</label>
          <input
            className={inputClassName}
            value={form.nationalId}
            onChange={onChange("nationalId")}
            required
          />
        </div>
        <div>
          <label className={labelClassName}>客户经理</label>
          <input
            className={inputClassName}
            value={form.managerName}
            onChange={onChange("managerName")}
            required
          />
        </div>
        <div>
          <label className={labelClassName}>经理工号 (6位)</label>
          <input
            className={inputClassName}
            value={form.managerId}
            onChange={onChange("managerId")}
            required
          />
        </div>
        <div>
          <label className={labelClassName}>业务编码</label>
          <input
            className={inputClassName}
            value={form.serviceCode}
            onChange={onChange("serviceCode")}
            required
          />
        </div>
        <div className="md:col-span-2">
          <label className={labelClassName}>当前套餐</label>
          <input
            className={inputClassName}
            value={form.currentPlan}
            onChange={onChange("currentPlan")}
            required
          />
        </div>
        <div>
          <label className={labelClassName}>附加服务 (逗号分隔)</label>
          <input
            className={inputClassName}
            value={form.additionalServices}
            onChange={onChange("additionalServices")}
          />
        </div>
        <div>
          <label className={labelClassName}>近6个月消费 (逗号分隔)</label>
          <input
            className={inputClassName}
            value={form.spendingLast6}
            onChange={onChange("spendingLast6")}
          />
        </div>
        <div className="md:col-span-2">
          <label className={labelClassName}>投诉记录 (逗号分隔)</label>
          <textarea
            className={inputClassName}
            rows={2}
            value={form.complaintHistory}
            onChange={onChange("complaintHistory")}
          />
        </div>
        <div className="md:col-span-2">
          <label className={labelClassName}>网络质量</label>
          <textarea
            className={inputClassName}
            rows={2}
            value={form.networkQuality}
            onChange={onChange("networkQuality")}
          />
        </div>

        <div className="flex gap-3 md:col-span-2">
          <button
            type="submit"
            disabled={loading}
            className="rounded-md bg-blue-600 px-6 py-2 font-medium text-white hover:bg-blue-700 disabled:opacity-50"
          >
            {loading ? "⏳ 生成中…" : "🚀 生成报告"}
          </button>
          <button
            type="button"
            onClick={() => void onRefresh()}
            className="rounded-md bg-gray-200 px-6 py-2 font-medium text-gray-800 hover:bg-gray-300"
          >
            🔄 刷新列表
          </button>
        </div>
      </form>
    </section>
  );
}
