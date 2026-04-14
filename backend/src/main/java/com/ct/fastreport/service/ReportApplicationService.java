package com.ct.fastreport.service;

import com.ct.fastreport.application.port.ReportJobPublisher;
import com.ct.fastreport.dto.ReportRequest;
import com.ct.fastreport.dto.ReportPageResponse;
import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.exception.BlockError;
import com.ct.fastreport.exception.ValidationError;
import com.ct.fastreport.exception.WarningException;
import com.ct.fastreport.monitoring.MonitoringService;
import com.ct.fastreport.model.InternalReportRequest;
import com.ct.fastreport.repository.CustomerRepository;
import com.ct.fastreport.repository.ManagerRepository;
import com.ct.fastreport.repository.ReportRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class ReportApplicationService {

    private static final Logger log = LoggerFactory.getLogger(ReportApplicationService.class);
    private static final Pattern CUSTOMER_ID_PATTERN = Pattern.compile("^\\d{8}$");
    private static final Pattern MANAGER_ID_PATTERN = Pattern.compile("^\\d{6}$");
    private static final Pattern NATIONAL_ID_PATTERN = Pattern.compile("^\\d{17}[\\dXx]$");
    private static final Set<String> ALLOWED_SERVICE_CODES = Set.of(
            "FTTH_200M",
            "FTTH_300M",
            "FTTH_500M",
            "FTTH_1000M"
    );

    private final ReportRepository reportRepository;
    private final CustomerRepository customerRepository;
    private final ManagerRepository managerRepository;
    private final ReportJobPublisher reportJobPublisher;
    private final ReportSseService reportSseService;
    private final ReportExportService reportExportService;
    private final ReportRequestMapper reportRequestMapper;
    private final MonitoringService monitoringService;

    public ReportApplicationService(ReportRepository reportRepository,
                                    CustomerRepository customerRepository,
                                    ManagerRepository managerRepository,
                                    ReportJobPublisher reportJobPublisher,
                                    ReportSseService reportSseService,
                                    ReportExportService reportExportService,
                                    ReportRequestMapper reportRequestMapper,
                                    MonitoringService monitoringService) {
        this.reportRepository = reportRepository;
        this.customerRepository = customerRepository;
        this.managerRepository = managerRepository;
        this.reportJobPublisher = reportJobPublisher;
        this.reportSseService = reportSseService;
        this.reportExportService = reportExportService;
        this.reportRequestMapper = reportRequestMapper;
        this.monitoringService = monitoringService;
    }

    @Transactional
    public Map<String, Object> create(ReportRequest req) {
        InternalReportRequest internalRequest = reportRequestMapper.fromLegacyRequest(req);
        validateRequest(internalRequest);
        log.info("Creating report for customer {}", internalRequest.customer().customerId());
        validateDuplicateErrors(internalRequest);

        List<String> warnings = collectWarnings(internalRequest);
        if (!warnings.isEmpty() && isBlank(internalRequest.overrideReason())) {
            throw new WarningException(
                    "OVERRIDE_REASON_REQUIRED",
                    "Override reason is required for warning cases.",
                    Map.of("warnings", warnings)
            );
        }

        Long customerDbId = customerRepository.upsert(internalRequest);
        Long managerDbId = managerRepository.upsert(internalRequest);
        long id = reportRepository.insertReport(customerDbId, managerDbId, internalRequest);

        log.info("Inserted report id={}", id);

        reportRepository.insertSpendingHistory(id, internalRequest.spendingHistory());
        reportRepository.insertComplaints(id, internalRequest.complaintHistory());
        reportRepository.insertNetworkQuality(id, internalRequest.networkQuality());

        reportJobPublisher.publishNewReport(id);
        log.info("Published report generation job for report id={}", id);
        monitoringService.recordReportCreated();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("status", "pending");
        body.put("message", "Report accepted. It will be generated in the background.");
        if (!warnings.isEmpty()) {
            body.put("warnings", warnings);
            body.put("overrideReason", internalRequest.overrideReason());
        }
        return body;
    }

    public List<ReportResponse> list(String search, String status, Integer limit, Integer offset) {
        int safeLimit = clampLimit(limit == null ? 100 : limit);
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        return reportRepository.findAll(search, parseStatuses(status), safeLimit, safeOffset);
    }

    public ReportPageResponse page(String search, String status, Integer limit, Integer offset) {
        int safeLimit = clampLimit(limit);
        int safeOffset = Math.max(0, offset == null ? 0 : offset);
        List<String> statuses = parseStatuses(status);

        ReportPageResponse response = new ReportPageResponse();
        response.items = reportRepository.findAll(search, statuses, safeLimit, safeOffset);
        response.total = reportRepository.countAll(search, statuses);
        response.limit = safeLimit;
        response.offset = safeOffset;
        return response;
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

    private void validateRequest(InternalReportRequest req) {
        if (req == null) {
            throw new ValidationError("REQUEST_BODY_REQUIRED", "Request body is required.");
        }

        if (req.customer() == null || req.manager() == null || req.service() == null || req.networkQuality() == null) {
            throw new ValidationError("REQUIRED_OBJECTS_MISSING", "customer, manager, service, and networkQuality are required.");
        }

        if (isBlank(req.customer().customerId()) || isBlank(req.customer().customerName()) || isBlank(req.customer().nationalId()) ||
                isBlank(req.manager().managerId()) || isBlank(req.manager().managerName()) || isBlank(req.service().serviceCode()) ||
                isBlank(req.currentPlan()) || isBlank(req.networkQuality().summary())) {
            throw new ValidationError("REQUIRED_FIELDS_MISSING", "Required fields must not be blank.");
        }

        if (!CUSTOMER_ID_PATTERN.matcher(req.customer().customerId()).matches()) {
            throw new ValidationError("INVALID_CUSTOMER_ID", "customerId must be exactly 8 digits.");
        }

        if (!MANAGER_ID_PATTERN.matcher(req.manager().managerId()).matches()) {
            throw new ValidationError("INVALID_MANAGER_ID", "managerId must be exactly 6 digits.");
        }

        if (!NATIONAL_ID_PATTERN.matcher(req.customer().nationalId()).matches()) {
            throw new ValidationError("INVALID_NATIONAL_ID", "nationalId must match the 18-character ID format.");
        }

        if (!ALLOWED_SERVICE_CODES.contains(req.service().serviceCode())) {
            throw new ValidationError("INVALID_SERVICE_CODE", "serviceCode is not in the allowed dictionary.");
        }

        if (req.spendingHistory() != null) {
            for (InternalReportRequest.SpendingPoint point : req.spendingHistory()) {
                if (point == null || point.amount() == null) {
                    throw new ValidationError("INVALID_SPENDING_LAST6", "spendingLast6 contains a null value.");
                }
            }
        }

        if (req.additionalServices() != null) {
            for (String service : req.additionalServices()) {
                if (service == null) {
                    throw new ValidationError("INVALID_ADDITIONAL_SERVICES", "additionalServices contains a null value.");
                }
            }
        }

        if (req.complaintHistory() != null) {
            for (InternalReportRequest.ComplaintRecord complaint : req.complaintHistory()) {
                if (complaint == null || complaint.description() == null) {
                    throw new ValidationError("INVALID_COMPLAINT_HISTORY", "complaintHistory contains a null value.");
                }
            }
        }
    }

    private void validateDuplicateErrors(InternalReportRequest req) {
        if (customerRepository.existsDifferentNationalId(req.customer().customerId(), req.customer().nationalId())) {
            throw new BlockError(
                    "CUSTOMER_ID_NATIONAL_ID_CONFLICT",
                    "customerId already exists with a different nationalId."
            );
        }

        if (managerRepository.existsDifferentManagerName(req.manager().managerId(), req.manager().managerName())) {
            throw new BlockError(
                    "MANAGER_ID_NAME_CONFLICT",
                    "managerId already exists with a different managerName."
            );
        }

        if (reportRepository.existsSameCustomerServiceSameDay(req.customer().customerId(), req.service().serviceCode())) {
            throw new BlockError(
                    "DUPLICATE_REPORT_SAME_DAY",
                    "A report already exists for the same customer, service, and generation date."
            );
        }
    }

    private List<String> collectWarnings(InternalReportRequest req) {
        List<String> warnings = new ArrayList<>();

        if (reportRepository.existsSameCustomerServiceDifferentDay(req.customer().customerId(), req.service().serviceCode())) {
            warnings.add("Same customer and service exist on a different day.");
        }

        if (reportRepository.existsDifferentCustomerForNationalId(req.customer().customerId(), req.customer().nationalId())) {
            warnings.add("Same nationalId exists under a different customerId.");
        }

        return warnings;
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private List<String> parseStatuses(String raw) {
        if (isBlank(raw)) {
            return List.of();
        }

        return java.util.Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .map(value -> value.toLowerCase(java.util.Locale.ROOT))
                .distinct()
                .collect(Collectors.toList());
    }

    private int clampLimit(Integer limit) {
        int resolved = limit == null ? 20 : limit;
        if (resolved < 1) {
            return 20;
        }
        return Math.min(resolved, 100);
    }
}
