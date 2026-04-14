package com.ct.fastreport.monitoring;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MonitoringService {

    private final MeterRegistry meterRegistry;
    private final Counter reportsCreatedCounter;
    private final Counter retriesScheduledCounter;
    private final AtomicInteger activeSseSubscriptions = new AtomicInteger();

    public MonitoringService(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.reportsCreatedCounter = Counter.builder("fastreport_reports_created_total")
                .description("Number of reports accepted for background generation")
                .register(meterRegistry);
        this.retriesScheduledCounter = Counter.builder("fastreport_report_retries_total")
                .description("Number of report retries scheduled")
                .register(meterRegistry);
        Gauge.builder("fastreport_sse_active_subscriptions", activeSseSubscriptions, AtomicInteger::get)
                .description("Current number of active SSE subscriptions")
                .register(meterRegistry);
    }

    public void recordReportCreated() {
        reportsCreatedCounter.increment();
    }

    public void recordReportTerminalStatus(String status, Duration duration) {
        meterRegistry.counter("fastreport_report_terminal_total", "status", status).increment();
        Timer.builder("fastreport_report_end_to_end_duration")
                .description("Time from report creation to terminal status")
                .tag("status", status)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordRetryScheduled() {
        retriesScheduledCounter.increment();
    }

    public void recordLlmCall(String provider, boolean success, Duration duration) {
        String outcome = success ? "success" : "error";
        meterRegistry.counter("fastreport_llm_requests_total", "provider", provider, "outcome", outcome)
                .increment();
        Timer.builder("fastreport_llm_request_duration")
                .description("Latency for outbound LLM requests")
                .tag("provider", provider)
                .tag("outcome", outcome)
                .register(meterRegistry)
                .record(duration);
    }

    public void recordSsePublish(boolean success) {
        meterRegistry.counter("fastreport_sse_publish_total", "outcome", success ? "success" : "error")
                .increment();
    }

    public void setActiveSseSubscriptions(int count) {
        activeSseSubscriptions.set(count);
    }
}
