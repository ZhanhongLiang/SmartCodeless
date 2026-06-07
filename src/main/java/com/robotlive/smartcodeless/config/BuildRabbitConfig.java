package com.robotlive.smartcodeless.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class BuildRabbitConfig {

    public static final String BUILD_EXCHANGE = "code.build.exchange";
    public static final String BUILD_DLX = "code.build.dlx";
    public static final String BUILD_QUEUE = "code.build.queue";
    public static final String BUILD_DLQ = "code.build.dlq";
    public static final String BUILD_ROUTING_KEY = "code.build";
    public static final String BUILD_DLQ_ROUTING_KEY = "code.build.dlq";

    @Bean
    public DirectExchange buildExchange() {
        return new DirectExchange(BUILD_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange buildDeadLetterExchange() {
        return new DirectExchange(BUILD_DLX, true, false);
    }

    @Bean
    public Queue buildQueue() {
        return new Queue(BUILD_QUEUE, true, false, false, Map.of(
                "x-dead-letter-exchange", BUILD_DLX,
                "x-dead-letter-routing-key", BUILD_DLQ_ROUTING_KEY
        ));
    }

    @Bean
    public Queue buildDeadLetterQueue() {
        return new Queue(BUILD_DLQ, true);
    }

    @Bean
    public Binding buildBinding() {
        return BindingBuilder.bind(buildQueue()).to(buildExchange()).with(BUILD_ROUTING_KEY);
    }

    @Bean
    public Binding buildDeadLetterBinding() {
        return BindingBuilder.bind(buildDeadLetterQueue()).to(buildDeadLetterExchange()).with(BUILD_DLQ_ROUTING_KEY);
    }

    @Bean
    public MessageConverter buildMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
