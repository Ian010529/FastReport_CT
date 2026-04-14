package com.ct.fastreport.application.port;

import com.ct.fastreport.dto.ReportResultMessage;

public interface ReportResultNotifier {
    void notifyResult(ReportResultMessage message);
}
