package com.ct.fastreport.service;

import com.ct.fastreport.dto.ReportResultMessage;
import com.ct.fastreport.monitoring.MonitoringService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class ReportSseService {

    private static final Logger log = LoggerFactory.getLogger(ReportSseService.class);
    private static final long SSE_TIMEOUT_MS = 10 * 60 * 1000L;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> emittersByReportId = new ConcurrentHashMap<>();
    private final MonitoringService monitoringService;

    public ReportSseService(MonitoringService monitoringService) {
        this.monitoringService = monitoringService;
    }

    public SseEmitter subscribe(Long reportId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT_MS);
        emittersByReportId.computeIfAbsent(reportId, ignored -> new CopyOnWriteArrayList<>()).add(emitter);
        updateSubscriptionGauge();

        emitter.onCompletion(() -> removeEmitter(reportId, emitter));
        emitter.onTimeout(() -> removeEmitter(reportId, emitter));
        emitter.onError(ex -> removeEmitter(reportId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data("subscribed:" + reportId));
        } catch (IOException ex) {
            removeEmitter(reportId, emitter);
            emitter.completeWithError(ex);
        }

        return emitter;
    }

    public void publishResult(ReportResultMessage message) {
        List<SseEmitter> emitters = emittersByReportId.get(message.getReportId());
        if (emitters == null || emitters.isEmpty()) {
            log.info("No active SSE subscribers for report id={}", message.getReportId());
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("report-result")
                        .data(message));
                monitoringService.recordSsePublish(true);
                emitter.complete();
            } catch (IOException ex) {
                monitoringService.recordSsePublish(false);
                emitter.completeWithError(ex);
            } finally {
                removeEmitter(message.getReportId(), emitter);
            }
        }
    }

    private void removeEmitter(Long reportId, SseEmitter emitter) {
        CopyOnWriteArrayList<SseEmitter> emitters = emittersByReportId.get(reportId);
        if (emitters == null) {
            return;
        }

        emitters.remove(emitter);
        if (emitters.isEmpty()) {
            emittersByReportId.remove(reportId);
        }
        updateSubscriptionGauge();
    }

    private void updateSubscriptionGauge() {
        int total = emittersByReportId.values().stream()
                .mapToInt(List::size)
                .sum();
        monitoringService.setActiveSseSubscriptions(total);
    }
}
