import type { Metadata } from "next";
import "./globals.css";

export const metadata: Metadata = {
  title: "中国电信 · 客户服务优化报告",
  description: "Customer Service Optimization Report Generator",
};

export default function RootLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  return (
    <html lang="zh-CN">
      <body className="min-h-screen">
        <header className="bg-blue-700 text-white py-4 px-6 shadow-md">
          <h1 className="text-xl font-bold">中国电信 · 客户服务优化报告系统</h1>
        </header>
        <main className="max-w-5xl mx-auto py-8 px-4">{children}</main>
      </body>
    </html>
  );
}
