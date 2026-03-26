import Link from "next/link";
import ReactMarkdown from "react-markdown";

import type { ReportDetail } from "@/entities/report/model/types";
import { getReportDownloadUrl } from "@/entities/report/api/report-api";
import { SectionCard } from "@/shared/ui/section-card";
import { StatusBadge } from "@/shared/ui/status-badge";

interface ReportDetailViewProps {
  report: ReportDetail;
  reportId: string;
}

export function ReportDetailView({
  report,
  reportId,
}: ReportDetailViewProps) {
  const parsedReport = parseStructuredReport(report.reportContent);

  return (
    <div className="space-y-6">
      <Link href="/" className="text-sm font-medium text-slate-600 hover:text-slate-950">
        ← Back to workspace
      </Link>

      <SectionCard>
        <div className="flex flex-col gap-4 lg:flex-row lg:items-start lg:justify-between">
          <div>
            <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
              Report Overview
            </p>
            <h2 className="mt-2 text-3xl font-semibold tracking-tight text-slate-950">
              Report #{report.id}
            </h2>
            <p className="mt-2 text-sm leading-6 text-slate-500">
              Review the report metadata, monitor delivery state, and download completed files.
            </p>
          </div>
          <div className="flex items-center gap-3">
            <StatusBadge status={report.status} />
          {report.status === "completed" && (
            <div className="flex gap-2">
              <a
                href={getReportDownloadUrl(reportId, "txt")}
                className="rounded-full border border-slate-200 bg-white px-3 py-2 text-xs font-semibold text-slate-700 shadow-sm hover:border-slate-300 hover:bg-slate-50"
              >
                TXT
              </a>
              <a
                href={getReportDownloadUrl(reportId, "pdf")}
                className="rounded-full border border-rose-200 bg-rose-50 px-3 py-2 text-xs font-semibold text-rose-700 shadow-sm hover:border-rose-300 hover:bg-rose-100"
              >
                PDF
              </a>
              <a
                href={getReportDownloadUrl(reportId, "csv")}
                className="rounded-full border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs font-semibold text-emerald-700 shadow-sm hover:border-emerald-300 hover:bg-emerald-100"
              >
                CSV
              </a>
            </div>
          )}
          </div>
        </div>
        <dl className="mt-6 grid grid-cols-1 gap-4 text-sm sm:grid-cols-2 xl:grid-cols-3">
          <DetailItem label="Customer">
            {report.customerName} ({report.customerId})
          </DetailItem>
          <DetailItem label="National ID">{report.nationalId}</DetailItem>
          <DetailItem label="Manager">
            {report.managerName} ({report.managerId})
          </DetailItem>
          <DetailItem label="Service Code">{report.serviceCode}</DetailItem>
          <DetailItem label="Current Plan">{report.currentPlan}</DetailItem>
          <DetailItem label="Created At">{report.createdAt}</DetailItem>
        </dl>
      </SectionCard>

      {report.status === "completed" && report.reportContent ? (
        <div className="space-y-5">
          <SectionCard className="overflow-hidden">
          <div className="flex flex-col gap-4 border-b border-slate-200 pb-5 sm:flex-row sm:items-end sm:justify-between">
            <div>
              <p className="text-xs font-semibold uppercase tracking-[0.24em] text-slate-500">
                Report Content
              </p>
              <h3 className="mt-2 text-xl font-semibold tracking-tight text-slate-950">
                {parsedReport.title}
              </h3>
              <p className="mt-2 max-w-2xl text-sm leading-6 text-slate-500">
                The final report is presented as a structured reading view with clear sections for faster review and handoff.
              </p>
            </div>
            <div className="rounded-full border border-slate-200 bg-slate-50 px-3 py-1 text-xs font-medium text-slate-600">
              {parsedReport.sections.length} structured sections
            </div>
          </div>
          {parsedReport.sections.length > 0 ? (
            <div className="mt-6 grid gap-4 xl:grid-cols-2">
              {parsedReport.sections.map((section, index) => (
                <article
                  key={section.title}
                  className={`rounded-[24px] border border-slate-200 bg-gradient-to-b from-white to-slate-50/70 p-5 sm:p-6 ${
                    index === 0 || index === parsedReport.sections.length - 1
                      ? "xl:col-span-2"
                      : ""
                  }`}
                >
                  <div className="flex items-start justify-between gap-4 border-b border-slate-200 pb-4">
                    <div>
                      <p className="text-xs font-semibold uppercase tracking-[0.2em] text-slate-500">
                        Section {index + 1}
                      </p>
                      <h4 className="mt-2 text-lg font-semibold tracking-tight text-slate-950">
                        {section.title}
                      </h4>
                    </div>
                  </div>
                  <div className="prose prose-slate mt-5 max-w-none prose-headings:tracking-tight prose-p:text-[15px] prose-p:leading-7 prose-li:my-1 prose-li:marker:text-sky-500 prose-strong:text-slate-950">
                    <ReactMarkdown>{section.body}</ReactMarkdown>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <div className="mt-6 rounded-[24px] border border-slate-200 bg-gradient-to-b from-white to-slate-50/70 p-5 sm:p-7">
              <div className="prose prose-slate max-w-none prose-headings:mb-4 prose-headings:tracking-tight prose-h1:text-3xl prose-h2:mt-10 prose-h2:border-t prose-h2:border-slate-200 prose-h2:pt-6 prose-h2:text-xl prose-h3:mt-6 prose-h3:text-lg prose-p:text-[15px] prose-p:leading-7 prose-li:my-1 prose-li:marker:text-sky-500 prose-strong:text-slate-950">
                <ReactMarkdown>{report.reportContent}</ReactMarkdown>
              </div>
            </div>
          )}
          </SectionCard>
        </div>
      ) : (
        <SectionCard>
          <div className="rounded-2xl border border-amber-200 bg-amber-50 px-4 py-4 text-sm text-amber-900">
            <div className="font-semibold">Report not ready</div>
            <div className="mt-1">
              Current status: <span className="font-medium capitalize">{report.status}</span>
            </div>
            {report.status === "failed" && report.reportContent && (
              <p className="mt-3 text-sm leading-6">{report.reportContent}</p>
            )}
          </div>
        </SectionCard>
      )}
    </div>
  );
}

function parseStructuredReport(content: string | null): {
  title: string;
  sections: Array<{ title: string; body: string }>;
} {
  if (!content || !content.trim()) {
    return { title: "Final Report", sections: [] };
  }

  const lines = content.split("\n");
  let title = "Final Report";
  const sections: Array<{ title: string; body: string }> = [];
  let currentSection: { title: string; body: string[] } | null = null;

  for (const rawLine of lines) {
    const line = rawLine.trimEnd();

    if (line.startsWith("# ")) {
      title = line.replace(/^#\s+/, "").trim() || title;
      continue;
    }

    if (line.startsWith("## ")) {
      if (currentSection) {
        sections.push({
          title: currentSection.title,
          body: currentSection.body.join("\n").trim(),
        });
      }
      currentSection = {
        title: line.replace(/^##\s+/, "").trim(),
        body: [],
      };
      continue;
    }

    if (currentSection) {
      currentSection.body.push(rawLine);
    }
  }

  if (currentSection) {
    sections.push({
      title: currentSection.title,
      body: currentSection.body.join("\n").trim(),
    });
  }

  return {
    title,
    sections: sections.filter((section) => section.body.length > 0),
  };
}

function DetailItem({
  label,
  children,
}: {
  label: string;
  children: React.ReactNode;
}) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-slate-50/80 px-4 py-3">
      <dt className="text-xs font-semibold uppercase tracking-[0.16em] text-slate-500">
        {label}
      </dt>
      <dd className="mt-2 text-sm font-medium text-slate-900">{children}</dd>
    </div>
  );
}
