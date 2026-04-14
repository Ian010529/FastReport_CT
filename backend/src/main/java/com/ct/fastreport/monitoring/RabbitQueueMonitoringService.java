package com.ct.fastreport.monitoring;

import com.ct.fastreport.config.RabbitConfig;
import com.rabbitmq.client.AMQP;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicInteger;

@Service
@Profile("local")
public class RabbitQueueMonitoringService {

    private final RabbitTemplate rabbitTemplate;
    private final Counter queueProbeFailureCounter;
    private final AtomicInteger mainQueueDepth = new AtomicInteger();
    private final AtomicInteger retryQueueDepth = new AtomicInteger();
    private final AtomicInteger resultQueueDepth = new AtomicInteger();

    public RabbitQueueMonitoringService(MeterRegistry meterRegistry, RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
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
