package com.ct.fastreport.controller;

import com.ct.fastreport.dto.ReportRequest;
import com.ct.fastreport.dto.ReportPageResponse;
import com.ct.fastreport.dto.ReportResponse;
import com.ct.fastreport.service.ReportApplicationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private final ReportApplicationService reportApplicationService;

    public ReportController(ReportApplicationService reportApplicationService) {
        this.reportApplicationService = reportApplicationService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> create(@RequestBody ReportRequest req) {
        return ResponseEntity.accepted().body(reportApplicationService.create(req));
    }

    @GetMapping
    public List<ReportResponse> list(@RequestParam(required = false) String search,
                                     @RequestParam(required = false) String status,
                                     @RequestParam(required = false) Integer limit,
                                     @RequestParam(required = false) Integer offset) {
        return reportApplicationService.list(search, status, limit, offset);
    }

    @GetMapping("/page")
    public ReportPageResponse page(@RequestParam(required = false) String search,
                                   @RequestParam(required = false) String status,
                                   @RequestParam(defaultValue = "20") Integer limit,
                                   @RequestParam(defaultValue = "0") Integer offset) {
        return reportApplicationService.page(search, status, limit, offset);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReportResponse> get(@PathVariable Long id) {
        return reportApplicationService.get(id);
    }

    @GetMapping(value = "/{id}/events", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(@PathVariable Long id) {
        return reportApplicationService.subscribe(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           @RequestParam(defaultValue = "txt") String format) {
        return reportApplicationService.download(id, format);
    }
}
