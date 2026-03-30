package com.ct.fastreport.monitoring;

import com.ct.fastreport.config.RabbitConfig;
import com.rabbitmq.client.AMQP;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class MonitoringService {

    private final MeterRegistry meterRegistry;
    private final RabbitTemplate rabbitTemplate;
    private final Counter reportsCreatedCounter;
    private final Counter retriesScheduledCounter;
    private final Counter queueProbeFailureCounter;
    private final AtomicInteger mainQueueDepth = new AtomicInteger();
    private final AtomicInteger retryQueueDepth = new AtomicInteger();
    private final AtomicInteger resultQueueDepth = new AtomicInteger();
    private final AtomicInteger activeSseSubscriptions = new AtomicInteger();

    public MonitoringService(MeterRegistry meterRegistry, RabbitTemplate rabbitTemplate) {
        this.meterRegistry = meterRegistry;
        this.rabbitTemplate = rabbitTemplate;
        this.reportsCreatedCounter = Counter.builder("fastreport_reports_created_total")
                .description("Number of reports accepted for background generation")
                .register(meterRegistry);
        this.retriesScheduledCounter = Counter.builder("fastreport_report_retries_total")
                .description("Number of report retries scheduled")
                .register(meterRegistry);
        this.queueProbeFailureCounter = Counter.builder("fastreport_rabbitmq_queue_probe_failures_total")
                .description("Number of RabbitMQ queue depth probe failures")
                .register(meterRegistry);

        Gauge.builder("fastreport_rabbitmq_main_queue_depth", mainQueueDepth, AtomicInteger::get)
                .description("Current message depth for the main report generation queue")
                .register(meterRegistry);
        Gauge.builder("fastreport_rabbitmq_retry_queue_depth", retryQueueDepth, AtomicInteger::get)
                .description("Current combined message depth for retry queues")
                .register(meterRegistry);
        Gauge.builder("fastreport_rabbitmq_result_queue_depth", resultQueueDepth, AtomicInteger::get)
                .description("Current message depth for the report result queue")
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

    @Scheduled(fixedDelay = 10000L, initialDelay = 5000L)
    public void refreshQueueDepthMetrics() {
        try {
            mainQueueDepth.set(queueDepth(RabbitConfig.MAIN_QUEUE));
            retryQueueDepth.set(
                    queueDepth(RabbitConfig.RETRY_QUEUE_1)
                            + queueDepth(RabbitConfig.RETRY_QUEUE_2)
                            + queueDepth(RabbitConfig.RETRY_QUEUE_3)
            );
            resultQueueDepth.set(queueDepth(RabbitConfig.RESULT_QUEUE));
        } catch (AmqpException ex) {
            queueProbeFailureCounter.increment();
        }
    }

    private int queueDepth(String queueName) {
        return rabbitTemplate.execute(channel -> {
            AMQP.Queue.DeclareOk ok = channel.queueDeclarePassive(queueName);
            return ok.getMessageCount();
        });
    }
}
