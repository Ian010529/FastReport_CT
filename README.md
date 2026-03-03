# FastReport_CT(v0.1)

面向中国电信客服场景的智能报告生成系统（MVP）。

系统流程：前端录入客户信息 → 后端落库并调用 LLM → 生成结构化中文分析报告 → 支持列表查询、详情查看与文件下载（TXT/PDF/CSV）。

## 1. 项目概览

FastReport_CT 是一个前后端分离项目，聚焦「客户服务优化报告」生成：

- 前端提供报表创建表单与历史记录查询。
- 后端使用 Spring Boot + JdbcTemplate 对 PostgreSQL 进行读写。
- 通过 OpenAI 兼容接口生成中文 Markdown 报告。
- 支持导出报告为 TXT、PDF、CSV。

## 2. 技术栈

| 模块 | 技术 |
|---|---|
| 前端 | Next.js 14、React 18、TypeScript、Tailwind CSS |
| 后端 | Java 17、Spring Boot 3.2.5、Spring Web、JdbcTemplate |
| 数据库 | PostgreSQL 16 |
| 文档导出 | iText 8（PDF） |
| 模型接口 | OpenAI-compatible Chat Completions API |
| 部署方式 | Docker Compose |

## 3. 仓库结构

```text
.
├── docker-compose.yml
├── backend/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/
│       ├── java/com/ct/fastreport/
│       │   ├── Application.java
│       │   ├── CorsConfig.java
│       │   └── ReportController.java
│       └── resources/
│           ├── application.yml
│           └── schema.sql
└── frontend/
    ├── Dockerfile
    ├── package.json
    └── src/app/
        ├── layout.tsx
        ├── page.tsx
        └── report/[id]/page.tsx
```

## 4. 功能说明

### 4.1 报告创建

- 页面：`/`
- 输入字段：客户信息、套餐、附加服务、近 6 月消费、投诉记录、网络质量。
- 提交后后端执行同步生成流程：
  1. 插入记录（`pending`）
  2. 更新为 `processing`
  3. 调用 LLM 生成报告
  4. 成功则 `completed`，失败则 `failed`

### 4.2 报告查询

- 支持历史列表查询与关键字搜索（客户编号/姓名/经理/业务编码等）。
- 报告详情页：`/report/{id}`。

### 4.3 报告下载

- TXT：纯文本报告。
- PDF：基于 iText 生成（包含中文字体回退逻辑）。
- CSV：UTF-8 BOM，便于 Excel 打开。

## 5. API 一览

| 方法 | 路径 | 说明 |
|---|---|---|
| POST | `/api/reports` | 创建并生成报告（同步） |
| GET | `/api/reports` | 获取报告列表（支持 `search` 参数） |
| GET | `/api/reports/{id}` | 获取报告详情 |
| GET | `/api/reports/{id}/download?format=txt\|pdf\|csv` | 下载报告 |

### POST /api/reports 请求示例

```json
{
  "customerId": "10000001",
  "customerName": "张三",
  "nationalId": "110101199003077758",
  "managerName": "李经理",
  "managerId": "200001",
  "serviceCode": "FTTH_500M",
  "currentPlan": "畅享融合 199 套餐",
  "additionalServices": ["天翼云盘", "天翼高清"],
  "spendingLast6": [199, 199, 210, 185, 199, 220],
  "complaintHistory": ["2024-12 宽带网速慢", "2025-01 客服响应时间过长"],
  "networkQuality": "下载速率偶尔低于签约带宽的 50%"
}
```

## 6. 运行方式（Docker）

### 6.1 准备环境变量

在项目根目录创建 `.env` 文件，至少包含：

```env
OPENAI_API_KEY=your_api_key
OPENAI_BASE_URL=https://api.openai.com
OPENAI_MODEL=gpt-4o-mini
```

### 6.2 启动服务

```bash
docker compose up --build
```

### 6.3 访问地址

- 前端：http://localhost:3000
- 后端：http://localhost:8080
- 数据库：localhost:5432（DB: `fastreport` / user: `fastreport` / pass: `fastreport123`）

## 7. 关键配置

- CORS 已允许 `http://localhost:3000`。
- 前端通过 `NEXT_PUBLIC_API_URL` 指向后端（Compose 中默认 `http://localhost:8080`）。
- 后端数据库配置可由环境变量覆盖：
  - `SPRING_DATASOURCE_URL`
  - `SPRING_DATASOURCE_USERNAME`
  - `SPRING_DATASOURCE_PASSWORD`

## 8. 当前版本说明（v0.1）

- 已实现：核心报表生成闭环（创建/查询/详情/下载）。
- 当前为 MVP：
  - 暂未实现复杂参数校验与权限体系。
  - LLM 生成为同步调用，请求耗时受模型响应影响。
  - 后端逻辑集中在 `ReportController`，后续可进一步分层（Service/Repository）。
