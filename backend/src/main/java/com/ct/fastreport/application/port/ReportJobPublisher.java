package com.ct.fastreport.application.port;

public interface ReportJobPublisher {
    void publishNewReport(Long reportId);
}
