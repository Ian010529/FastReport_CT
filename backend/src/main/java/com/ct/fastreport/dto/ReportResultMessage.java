package com.ct.fastreport.dto;

public class ReportResultMessage {
    private Long reportId;
    private String status;
    private String reportContent;

    public ReportResultMessage() {
    }

    public ReportResultMessage(Long reportId, String status, String reportContent) {
        this.reportId = reportId;
        this.status = status;
        this.reportContent = reportContent;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReportContent() {
        return reportContent;
    }

    public void setReportContent(String reportContent) {
        this.reportContent = reportContent;
    }
}
