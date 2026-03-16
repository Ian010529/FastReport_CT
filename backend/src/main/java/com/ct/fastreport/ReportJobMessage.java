package com.ct.fastreport;

public class ReportJobMessage {
    private Long reportId;
    private int retryCount;

    public ReportJobMessage() {
    }

    public ReportJobMessage(Long reportId, int retryCount) {
        this.reportId = reportId;
        this.retryCount = retryCount;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }
}
