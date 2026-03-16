package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportRequest;
import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.messaging.ReportJobPublisher;
import com.ct.fastreport.repository.CustomerRepository;
import com.ct.fastreport.repository.ManagerRepository;
import com.ct.fastreport.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReportApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReportApplicationService.class);

    private final ReportRepository reportRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final ReportJobPublisher reportJobPublisher;
    private final ReportSseService reportSseService;
    private final ReportExportService reportExportService;

    public ReportApplicationService(ReportRepository reportRepository,
                                    CustomerRepository customerRepository,
                                    ManagerRepository managerRepository,
                                    ReportJobPublisher reportJobPublisher,
                                    ReportSseService reportSseService,
                                    ReportExportService reportExportService) {
        this.reportRepository = reportRepository;
        this.customerRepository = customerRepository;
        this.managerRepository = managerRepository;
        this.reportJobPublisher = reportJobPublisher;
        this.reportSseService = reportSseService;
        this.reportExportService = reportExportService;
    }

    public ResponseEntity<Map<String, Object>> create(ReportRequest req) {
        log.info("Creating report for customer {}", req.customerId);

        String additionalSvc = req.additionalServices != null ? String.join(",", req.additionalServices) : null;
        Long customerDbId = customerRepository.upsert(req);
        Long managerDbId = managerRepository.upsert(req);
        long id = reportRepository.insertReport(customerDbId, managerDbId, req, additionalSvc);

        log.info("Inserted report id={}", id);

        reportRepository.insertSpendingHistory(id, req.spendingLast6);
        reportRepository.insertComplaints(id, req.complaintHistory);
        reportRepository.insertNetworkQuality(id, req.networkQuality);

        reportJobPublisher.publishNewReport(id);
        log.info("Published RabbitMQ job for report id={}", id);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("status", "pending");
        body.put("message", "已收到，Care Plan 将在后台生成。若你不主动刷新页面，将不会看到状态变化。");
        return ResponseEntity.accepted().body(body);
    }

    public List<ReportResponse> list(String search) {
        return reportRepository.findAll(search);
    }

    public ResponseEntity<ReportResponse> get(Long id) {
        ReportResponse report = reportRepository.findById(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(report);
    }

    public SseEmitter subscribe(Long id) {
        return reportSseService.subscribe(id);
    }

    public ResponseEntity<byte[]> download(Long id, String format) {
        ReportResponse report = reportRepository.findById(id);
        if (report == null) {
            return ResponseEntity.notFound().build();
        }
        return reportExportService.download(report, format);
    }
}
