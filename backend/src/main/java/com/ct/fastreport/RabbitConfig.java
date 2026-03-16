package com.ct.fastreport;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EXCHANGE = "report.exchange";
    public static final String MAIN_QUEUE = "report.generate.queue";
    public static final String MAIN_ROUTING_KEY = "report.generate";
    public static final String RETRY_QUEUE_1 = "report.generate.retry.1.queue";
    public static final String RETRY_QUEUE_2 = "report.generate.retry.2.queue";
    public static final String RETRY_QUEUE_3 = "report.generate.retry.3.queue";
    public static final String RESULT_QUEUE = "report.result.queue";
    public static final String RETRY_ROUTING_KEY_1 = "report.generate.retry.1";
    public static final String RETRY_ROUTING_KEY_2 = "report.generate.retry.2";
    public static final String RETRY_ROUTING_KEY_3 = "report.generate.retry.3";
    public static final String RESULT_ROUTING_KEY = "report.result";
    public static final long RETRY_DELAY_1_MS = 5_000L;
    public static final long RETRY_DELAY_2_MS = 10_000L;
    public static final long RETRY_DELAY_3_MS = 20_000L;

    @Bean
    public DirectExchange reportExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue reportGenerateQueue() {
        return QueueBuilder.durable(MAIN_QUEUE).build();
    }

    @Bean
    public Queue reportRetryQueue1() {
        return retryQueue(RETRY_QUEUE_1, RETRY_DELAY_1_MS);
    }

    @Bean
    public Queue reportRetryQueue2() {
        return retryQueue(RETRY_QUEUE_2, RETRY_DELAY_2_MS);
    }

    @Bean
    public Queue reportRetryQueue3() {
        return retryQueue(RETRY_QUEUE_3, RETRY_DELAY_3_MS);
    }

    @Bean
    public Queue reportResultQueue() {
        return QueueBuilder.durable(RESULT_QUEUE).build();
    }

    @Bean
    public Binding reportGenerateBinding() {
        return BindingBuilder.bind(reportGenerateQueue()).to(reportExchange()).with(MAIN_ROUTING_KEY);
    }

    @Bean
    public Binding reportRetryBinding1() {
        return BindingBuilder.bind(reportRetryQueue1()).to(reportExchange()).with(RETRY_ROUTING_KEY_1);
    }

    @Bean
    public Binding reportRetryBinding2() {
        return BindingBuilder.bind(reportRetryQueue2()).to(reportExchange()).with(RETRY_ROUTING_KEY_2);
    }

    @Bean
    public Binding reportRetryBinding3() {
        return BindingBuilder.bind(reportRetryQueue3()).to(reportExchange()).with(RETRY_ROUTING_KEY_3);
    }

    @Bean
    public Binding reportResultBinding() {
        return BindingBuilder.bind(reportResultQueue()).to(reportExchange()).with(RESULT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jacksonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    private Queue retryQueue(String queueName, long ttlMs) {
        return QueueBuilder.durable(queueName)
                .withArgument("x-message-ttl", ttlMs)
                .withArgument("x-dead-letter-exchange", EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MAIN_ROUTING_KEY)
                .build();
    }
}
