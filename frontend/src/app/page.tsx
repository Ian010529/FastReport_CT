"use client";

import { useState, useEffect } from "react";
import Link from "next/link";

const API = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

interface Report {
  id: number;
  customerId: string;
  customerName: string;
  status: string;
  createdAt: string;
}

interface FormData {
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
}

interface ReportResultEvent {
  reportId: number;
  status: string;
  reportContent: string | null;
}

const defaultForm: FormData = {
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
};

export default function Home() {
  const [form, setForm] = useState<FormData>(defaultForm);
  const [loading, setLoading] = useState(false);
  const [reports, setReports] = useState<Report[]>([]);
  const [fetched, setFetched] = useState(false);
  const [activeReportId, setActiveReportId] = useState<number | null>(null);
  const [liveMessage, setLiveMessage] = useState<string | null>(null);

  useEffect(() => {
    fetchReports();
  }, []); // 确保只在组件挂载时调用一次

  useEffect(() => {
    if (activeReportId == null) return;

    const source = new EventSource(`${API}/api/reports/${activeReportId}/events`);

    source.addEventListener("connected", () => {
      setLiveMessage(`SSE connected for report #${activeReportId}. Waiting for worker result...`);
    });

    source.addEventListener("report-result", async (event) => {
      const data = JSON.parse((event as MessageEvent).data) as ReportResultEvent;
      setLiveMessage(`Report #${data.reportId} is now ${data.status}.`);
      setReports((current) =>
        current.map((report) =>
          report.id === data.reportId
            ? { ...report, status: data.status }
            : report
        )
      );
      await fetchReports();
      setActiveReportId(null);
      source.close();
    });

    source.onerror = () => {
      setLiveMessage(`SSE connection for report #${activeReportId} was interrupted.`);
      source.close();
    };

    return () => {
      source.close();
    };
  }, [activeReportId]);

  const set = (k: keyof FormData) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm({ ...form, [k]: e.target.value });

  async function fetchReports() {
    const res = await fetch(`${API}/api/reports`);
    const data = await res.json();
    setReports(data);
    setFetched(true);
  }

  async function submit(e: React.FormEvent) {
    e.preventDefault();
    setLoading(true);
    try {
      const body = {
        customerId: form.customerId,
        customerName: form.customerName,
        nationalId: form.nationalId,
        managerName: form.managerName,
        managerId: form.managerId,
        serviceCode: form.serviceCode,
        currentPlan: form.currentPlan,
        additionalServices: form.additionalServices
          ? form.additionalServices.split(",").map((s) => s.trim())
          : [],
        spendingLast6: form.spendingLast6
          ? form.spendingLast6.split(",").map((s) => parseFloat(s.trim()))
          : [],
        complaintHistory: form.complaintHistory
          ? form.complaintHistory.split(",").map((s) => s.trim())
          : [],
        networkQuality: form.networkQuality,
      };

      const res = await fetch(`${API}/api/reports`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!res.ok) throw new Error(`HTTP ${res.status}`);
      const created = await res.json();
      setActiveReportId(created.id);
      setLiveMessage(`Report #${created.id} submitted. Opening SSE channel...`);
      await fetchReports();
    } catch (err: unknown) {
      alert("生成失败: " + (err instanceof Error ? err.message : err));
    } finally {
      setLoading(false);
    }
  }

  const label = "block text-sm font-medium text-gray-700 mb-1";
  const input = "w-full border border-gray-300 rounded-md px-3 py-2 text-sm focus:ring-2 focus:ring-blue-500 focus:outline-none";

  return (
    <div className="space-y-8">
      {/* ───── Form ───── */}
      <section className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold mb-4">📝 新建报告</h2>
        {liveMessage && (
          <div className="mb-4 rounded-md border border-blue-200 bg-blue-50 px-4 py-3 text-sm text-blue-800">
            {liveMessage}
          </div>
        )}
        <form onSubmit={submit} className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <label className={label}>客户编号 (8位)</label>
            <input className={input} value={form.customerId} onChange={set("customerId")} required />
          </div>
          <div>
            <label className={label}>客户姓名</label>
            <input className={input} value={form.customerName} onChange={set("customerName")} required />
          </div>
          <div>
            <label className={label}>身份证号 (18位)</label>
            <input className={input} value={form.nationalId} onChange={set("nationalId")} required />
          </div>
          <div>
            <label className={label}>客户经理</label>
            <input className={input} value={form.managerName} onChange={set("managerName")} required />
          </div>
          <div>
            <label className={label}>经理工号 (6位)</label>
            <input className={input} value={form.managerId} onChange={set("managerId")} required />
          </div>
          <div>
            <label className={label}>业务编码</label>
            <input className={input} value={form.serviceCode} onChange={set("serviceCode")} required />
          </div>
          <div className="md:col-span-2">
            <label className={label}>当前套餐</label>
            <input className={input} value={form.currentPlan} onChange={set("currentPlan")} required />
          </div>
          <div>
            <label className={label}>附加服务 (逗号分隔)</label>
            <input className={input} value={form.additionalServices} onChange={set("additionalServices")} />
          </div>
          <div>
            <label className={label}>近6个月消费 (逗号分隔)</label>
            <input className={input} value={form.spendingLast6} onChange={set("spendingLast6")} />
          </div>
          <div className="md:col-span-2">
            <label className={label}>投诉记录 (逗号分隔)</label>
            <textarea className={input} rows={2} value={form.complaintHistory} onChange={set("complaintHistory")} />
          </div>
          <div className="md:col-span-2">
            <label className={label}>网络质量</label>
            <textarea className={input} rows={2} value={form.networkQuality} onChange={set("networkQuality")} />
          </div>

          <div className="md:col-span-2 flex gap-3">
            <button
              type="submit"
              disabled={loading}
              className="bg-blue-600 hover:bg-blue-700 text-white font-medium px-6 py-2 rounded-md disabled:opacity-50"
            >
              {loading ? "⏳ 生成中…" : "🚀 生成报告"}
            </button>
            <button
              type="button"
              onClick={fetchReports}
              className="bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium px-6 py-2 rounded-md"
            >
              🔄 刷新列表
            </button>
          </div>
        </form>
      </section>

      {/* ───── Report list ───── */}
      <section className="bg-white rounded-lg shadow p-6">
        <h2 className="text-lg font-semibold mb-4">📋 历史报告</h2>
        {!fetched ? (
          <p className="text-gray-500 text-sm">点击 "刷新列表" 加载</p>
        ) : reports.length === 0 ? (
          <p className="text-gray-500 text-sm">暂无报告</p>
        ) : (
          <table className="w-full text-sm">
            <thead className="bg-gray-100">
              <tr>
                <th className="text-left px-3 py-2">ID</th>
                <th className="text-left px-3 py-2">客户</th>
                <th className="text-left px-3 py-2">状态</th>
                <th className="text-left px-3 py-2">创建时间</th>
                <th className="text-left px-3 py-2">操作</th>
              </tr>
            </thead>
            <tbody>
              {reports.map((r) => (
                <tr key={r.id} className="border-t">
                  <td className="px-3 py-2">{r.id}</td>
                  <td className="px-3 py-2">{r.customerName} ({r.customerId})</td>
                  <td className="px-3 py-2">
                    <StatusBadge status={r.status} />
                  </td>
                  <td className="px-3 py-2">{r.createdAt}</td>
                  <td className="px-3 py-2">
                    {r.status === "completed" && (
                      <Link
                        href={`/report/${r.id}`}
                        className="text-blue-600 hover:underline"
                      >
                        查看报告
                      </Link>
                    )}
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </section>
    </div>
  );
}

function StatusBadge({ status }: { status: string }) {
  const styles: Record<string, string> = {
    pending: "bg-yellow-100 text-yellow-800",
    processing: "bg-blue-100 text-blue-800",
    completed: "bg-green-100 text-green-800",
    failed: "bg-red-100 text-red-800",
  };
  const labels: Record<string, string> = {
    pending: "等待中",
    processing: "生成中",
    completed: "已完成",
    failed: "失败",
  };
  return (
    <span className={`px-2 py-0.5 rounded-full text-xs font-medium ${styles[status] || "bg-gray-100"}`}>
      {labels[status] || status}
    </span>
  );
}
