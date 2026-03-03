"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import ReactMarkdown from "react-markdown";

const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface Report {
  id: number;
  customerId: string;
  customerName: string;
  nationalId: string;
  managerName: string;
  managerId: string;
  serviceCode: string;
  currentPlan: string;
  status: string;
  reportContent: string | null;
  createdAt: string;
}

export default function ReportDetail() {
  const { id } = useParams();
  const [report, setReport] = useState<Report | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    fetch(`${API}/api/reports/${id}`)
      .then((r) => {
        if (!r.ok) throw new Error(`HTTP ${r.status}`);
        return r.json();
      })
      .then(setReport)
      .catch((e) => setError(e.message));
  }, [id]);

  if (error)
    return (
      <div className="bg-red-50 text-red-700 p-4 rounded">Error: {error}</div>
    );
  if (!report) return <p className="text-gray-500">加载中…</p>;

  function downloadUrl(format: string) {
    return `${API}/api/reports/${id}/download?format=${format}`;
  }

  return (
    <div className="space-y-6">
      <Link href="/" className="text-blue-600 hover:underline text-sm">
        ← 返回列表
      </Link>

      {/* Meta */}
      <div className="bg-white rounded-lg shadow p-6">
        <div className="flex items-center justify-between mb-3">
          <h2 className="text-lg font-semibold">报告 #{report.id}</h2>
          {report.status === "completed" && (
            <div className="flex gap-2">
              <a
                href={downloadUrl("txt")}
                className="bg-gray-100 hover:bg-gray-200 text-gray-700 text-xs font-medium px-3 py-1.5 rounded-md"
              >
                📄 TXT
              </a>
              <a
                href={downloadUrl("pdf")}
                className="bg-red-50 hover:bg-red-100 text-red-700 text-xs font-medium px-3 py-1.5 rounded-md"
              >
                📕 PDF
              </a>
              <a
                href={downloadUrl("csv")}
                className="bg-green-50 hover:bg-green-100 text-green-700 text-xs font-medium px-3 py-1.5 rounded-md"
              >
                📊 CSV
              </a>
            </div>
          )}
        </div>
        <dl className="grid grid-cols-2 md:grid-cols-3 gap-x-6 gap-y-2 text-sm">
          <div>
            <dt className="text-gray-500">客户</dt>
            <dd>{report.customerName} ({report.customerId})</dd>
          </div>
          <div>
            <dt className="text-gray-500">身份证</dt>
            <dd>{report.nationalId}</dd>
          </div>
          <div>
            <dt className="text-gray-500">客户经理</dt>
            <dd>{report.managerName} ({report.managerId})</dd>
          </div>
          <div>
            <dt className="text-gray-500">业务编码</dt>
            <dd>{report.serviceCode}</dd>
          </div>
          <div>
            <dt className="text-gray-500">当前套餐</dt>
            <dd>{report.currentPlan}</dd>
          </div>
          <div>
            <dt className="text-gray-500">创建时间</dt>
            <dd>{report.createdAt}</dd>
          </div>
        </dl>
      </div>

      {/* Report body */}
      {report.status === "completed" && report.reportContent ? (
        <div className="bg-white rounded-lg shadow p-6 prose prose-sm max-w-none">
          <ReactMarkdown>{report.reportContent}</ReactMarkdown>
        </div>
      ) : (
        <div className="bg-yellow-50 text-yellow-800 p-4 rounded">
          报告状态: {report.status}
          {report.status === "failed" && report.reportContent && (
            <p className="mt-2 text-sm">{report.reportContent}</p>
          )}
        </div>
      )}
    </div>
  );
}
