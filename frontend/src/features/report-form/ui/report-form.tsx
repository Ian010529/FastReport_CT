"use client";

import type { ChangeEvent, FormEvent } from "react";

import type { ReportFormValues } from "@/entities/report/model/types";
import { FeedbackBanner } from "@/shared/ui/feedback-banner";
import { SectionCard } from "@/shared/ui/section-card";

const labelClassName = "mb-1.5 block text-sm font-medium text-slate-700";
const inputClassName =
  "w-full rounded-2xl border border-slate-200 bg-white px-3.5 py-3 text-sm text-slate-900 shadow-sm outline-none transition focus:border-sky-300 focus:ring-4 focus:ring-sky-100";

const sectionTitleClassName = "text-sm font-semibold uppercase tracking-[0.18em] text-slate-500";
const sectionDescriptionClassName = "mt-1 text-sm text-slate-500";

interface ReportFormProps {
  feedback: {
    tone: "error" | "warning" | "success";
    title: string;
    message: string;
    details: string[];
  } | null;
  form: ReportFormValues;
  highlightedFields: (keyof ReportFormValues)[];
  loading: boolean;
  liveMessage: string | null;
  onChange: (
    field: keyof ReportFormValues,
  ) => (event: ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => void;
  onRefresh: () => Promise<void>;
  onSubmit: (event: FormEvent<HTMLFormElement>) => Promise<void>;
}

export function ReportForm({
  feedback,
  form,
  highlightedFields,
  loading,
  liveMessage,
  onChange,
  onRefresh,
  onSubmit,
}: ReportFormProps) {
  const fieldClassName = (field: keyof ReportFormValues) =>
    highlightedFields.includes(field)
      ? `${inputClassName} border-rose-300 bg-rose-50 ring-4 ring-rose-100`
      : inputClassName;

  return (
    <SectionCard className="h-full">
      <div className="flex flex-col gap-2 sm:flex-row sm:items-end sm:justify-between">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
            Create
          </p>
          <h3 className="mt-2 text-2xl font-semibold tracking-tight text-slate-950">
            Create Report
          </h3>
          <p className="mt-2 max-w-xl text-sm leading-6 text-slate-500">
            Submit a new report request, review validation guidance, and confirm warning cases when needed.
          </p>
        </div>
        <button
          type="button"
          onClick={() => void onRefresh()}
          className="inline-flex items-center justify-center rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-medium text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50"
        >
          Refresh Reports
        </button>
      </div>

      {liveMessage && (
        <div className="mt-5">
          <FeedbackBanner
            tone="info"
            title="Live update"
            message={liveMessage}
          />
        </div>
      )}
      {feedback && (
        <div className="mt-4">
          <FeedbackBanner {...feedback} />
        </div>
      )}
      <form onSubmit={onSubmit} className="mt-6 space-y-8">
        <div className="grid gap-6 lg:grid-cols-2">
          <section className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 sm:p-5">
            <h4 className={sectionTitleClassName}>Customer Information</h4>
            <p className={sectionDescriptionClassName}>
              Identify the customer and validate their report ownership details.
            </p>
            <div className="mt-5 grid gap-4">
              <div>
                <label className={labelClassName}>Customer ID</label>
                <input
                  className={fieldClassName("customerId")}
                  value={form.customerId}
                  onChange={onChange("customerId")}
                  required
                />
              </div>
              <div>
                <label className={labelClassName}>Customer Name</label>
                <input
                  className={fieldClassName("customerName")}
                  value={form.customerName}
                  onChange={onChange("customerName")}
                  required
                />
              </div>
              <div>
                <label className={labelClassName}>National ID</label>
                <input
                  className={fieldClassName("nationalId")}
                  value={form.nationalId}
                  onChange={onChange("nationalId")}
                  required
                />
              </div>
            </div>
          </section>

          <section className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 sm:p-5">
            <h4 className={sectionTitleClassName}>Manager Information</h4>
            <p className={sectionDescriptionClassName}>
              Assign accountability and make sure report ownership is consistent.
            </p>
            <div className="mt-5 grid gap-4">
              <div>
                <label className={labelClassName}>Manager ID</label>
                <input
                  className={fieldClassName("managerId")}
                  value={form.managerId}
                  onChange={onChange("managerId")}
                  required
                />
              </div>
              <div>
                <label className={labelClassName}>Manager Name</label>
                <input
                  className={fieldClassName("managerName")}
                  value={form.managerName}
                  onChange={onChange("managerName")}
                  required
                />
              </div>
            </div>
          </section>
        </div>

        <section className="rounded-2xl border border-slate-200/80 bg-slate-50/70 p-4 sm:p-5">
          <h4 className={sectionTitleClassName}>Report Details</h4>
          <p className={sectionDescriptionClassName}>
            Describe the report context, service profile, and quality signals used for generation.
          </p>
          <div className="mt-5 grid grid-cols-1 gap-4 md:grid-cols-2">
            <div>
              <label className={labelClassName}>Service Code</label>
              <input
                className={fieldClassName("serviceCode")}
                value={form.serviceCode}
                onChange={onChange("serviceCode")}
                required
              />
            </div>
            <div>
              <label className={labelClassName}>Current Plan</label>
              <input
                className={fieldClassName("currentPlan")}
                value={form.currentPlan}
                onChange={onChange("currentPlan")}
                required
              />
            </div>
            <div>
              <label className={labelClassName}>Additional Services</label>
              <input
                className={fieldClassName("additionalServices")}
                value={form.additionalServices}
                onChange={onChange("additionalServices")}
                placeholder="Cloud Storage, IPTV"
              />
            </div>
            <div>
              <label className={labelClassName}>Spending (Last 6 Months)</label>
              <input
                className={fieldClassName("spendingLast6")}
                value={form.spendingLast6}
                onChange={onChange("spendingLast6")}
                placeholder="199,199,210,185,199,220"
              />
            </div>
            <div className="md:col-span-2">
              <label className={labelClassName}>Complaint History</label>
              <textarea
                className={fieldClassName("complaintHistory")}
                rows={3}
                value={form.complaintHistory}
                onChange={onChange("complaintHistory")}
                placeholder="2024-12 Slow broadband speed, 2025-01 Delayed customer service response"
              />
            </div>
            <div className="md:col-span-2">
              <label className={labelClassName}>Network Quality</label>
              <textarea
                className={fieldClassName("networkQuality")}
                rows={3}
                value={form.networkQuality}
                onChange={onChange("networkQuality")}
                required
                placeholder="Download speed occasionally drops below 50% of the subscribed bandwidth."
              />
            </div>
            <div className="md:col-span-2">
              <label className={labelClassName}>Override Reason</label>
              <textarea
                className={fieldClassName("overrideReason")}
                rows={3}
                value={form.overrideReason}
                onChange={onChange("overrideReason")}
                placeholder="Explain why this warning case should proceed."
              />
              <p className="mt-2 text-xs leading-5 text-slate-500">
                Required only when the backend returns a warning that must be explicitly confirmed.
              </p>
            </div>
          </div>
        </section>

        <div className="flex flex-col gap-3 sm:flex-row">
          <button
            type="submit"
            disabled={loading}
            className="inline-flex items-center justify-center rounded-full bg-slate-950 px-6 py-3 text-sm font-semibold text-white shadow-sm transition hover:bg-slate-800 disabled:cursor-not-allowed disabled:opacity-60"
          >
            {loading ? "Submitting..." : "Submit Report"}
          </button>
          <button
            type="button"
            onClick={() => void onRefresh()}
            className="inline-flex items-center justify-center rounded-full border border-slate-200 bg-white px-6 py-3 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-slate-300 hover:bg-slate-50"
          >
            Refresh List
          </button>
        </div>
      </form>
    </SectionCard>
  );
}
