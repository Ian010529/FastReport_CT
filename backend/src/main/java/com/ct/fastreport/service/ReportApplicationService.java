package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportRequest;
import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.exception.BlockError;
import com.ct.fastreport.exception.ValidationError;
import com.ct.fastreport.exception.WarningException;
import com.ct.fastreport.messaging.ReportJobPublisher;
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

    @Transactional
    public Map<String, Object> create(ReportRequest req) {
        validateRequest(req);
        log.info("Creating report for customer {}", req.customerId);
        validateDuplicateErrors(req);

        List<String> warnings = collectWarnings(req);
        if (!warnings.isEmpty() && isBlank(req.overrideReason)) {
            throw new WarningException(
                    "OVERRIDE_REASON_REQUIRED",
                    "Override reason is required for warning cases.",
                    Map.of("warnings", warnings)
            );
        }

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
        body.put("message", "Report accepted. It will be generated in the background.");
        if (!warnings.isEmpty()) {
            body.put("warnings", warnings);
            body.put("overrideReason", req.overrideReason);
        }
        return body;
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

    private void validateRequest(ReportRequest req) {
        if (req == null) {
            throw new ValidationError("REQUEST_BODY_REQUIRED", "Request body is required.");
        }

        if (isBlank(req.customerId) || isBlank(req.customerName) || isBlank(req.nationalId) ||
                isBlank(req.managerId) || isBlank(req.managerName) || isBlank(req.serviceCode) ||
                isBlank(req.currentPlan) || isBlank(req.networkQuality)) {
            throw new ValidationError("REQUIRED_FIELDS_MISSING", "Required fields must not be blank.");
        }

        if (!CUSTOMER_ID_PATTERN.matcher(req.customerId).matches()) {
            throw new ValidationError("INVALID_CUSTOMER_ID", "customerId must be exactly 8 digits.");
        }

        if (!MANAGER_ID_PATTERN.matcher(req.managerId).matches()) {
            throw new ValidationError("INVALID_MANAGER_ID", "managerId must be exactly 6 digits.");
        }

        if (!NATIONAL_ID_PATTERN.matcher(req.nationalId).matches()) {
            throw new ValidationError("INVALID_NATIONAL_ID", "nationalId must match the 18-character ID format.");
        }

        if (!ALLOWED_SERVICE_CODES.contains(req.serviceCode)) {
            throw new ValidationError("INVALID_SERVICE_CODE", "serviceCode is not in the allowed dictionary.");
        }

        if (req.spendingLast6 != null) {
            for (Double amount : req.spendingLast6) {
                if (amount == null) {
                    throw new ValidationError("INVALID_SPENDING_LAST6", "spendingLast6 contains a null value.");
                }
            }
        }

        if (req.additionalServices != null) {
            for (String service : req.additionalServices) {
                if (service == null) {
                    throw new ValidationError("INVALID_ADDITIONAL_SERVICES", "additionalServices contains a null value.");
                }
            }
        }

        if (req.complaintHistory != null) {
            for (String complaint : req.complaintHistory) {
                if (complaint == null) {
                    throw new ValidationError("INVALID_COMPLAINT_HISTORY", "complaintHistory contains a null value.");
                }
            }
        }
    }

    private void validateDuplicateErrors(ReportRequest req) {
        if (customerRepository.existsDifferentNationalId(req.customerId, req.nationalId)) {
            throw new BlockError(
                    "CUSTOMER_ID_NATIONAL_ID_CONFLICT",
                    "customerId already exists with a different nationalId."
            );
        }

        if (managerRepository.existsDifferentManagerName(req.managerId, req.managerName)) {
            throw new BlockError(
                    "MANAGER_ID_NAME_CONFLICT",
                    "managerId already exists with a different managerName."
            );
        }

        if (reportRepository.existsSameCustomerServiceSameDay(req.customerId, req.serviceCode)) {
            throw new BlockError(
                    "DUPLICATE_REPORT_SAME_DAY",
                    "A report already exists for the same customer, service, and generation date."
            );
        }
    }

    private List<String> collectWarnings(ReportRequest req) {
        List<String> warnings = new ArrayList<>();

        if (reportRepository.existsSameCustomerServiceDifferentDay(req.customerId, req.serviceCode)) {
            warnings.add("Same customer and service exist on a different day.");
        }

        if (reportRepository.existsDifferentCustomerForNationalId(req.customerId, req.nationalId)) {
            warnings.add("Same nationalId exists under a different customerId.");
        }

        return warnings;
    }
    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
