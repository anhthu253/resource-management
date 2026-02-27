package com.example.springboot.service;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {
    public static final String BOOKING_EXCHANGE = "booking.exchange";

    @Bean
    public TopicExchange bookingExchange() {
        return new TopicExchange(BOOKING_EXCHANGE);
    }

    // Queues
    @Bean
    public Queue bookingCreatedQueue() {
        return new Queue("booking.created.queue");
    }
    @Bean
    public Queue bookingFailedQueue() {
        return new Queue("booking.failed.queue");
    }

    @Bean
    public Queue bookingCancelledQueue() {
        return new Queue("cancel.succeeded.queue");
    }

    @Bean
    public Queue cancelFailedQueue() {
        return new Queue("cancel.failed.queue");
    }

    @Bean
    public Queue modifySuccessQueue() {
        return new Queue("modify.succeeded.queue");
    }

    @Bean
    public Queue modifyFailedQueue() {
        return new Queue("modify.failed.queue");
    }

    // Bindings
    @Bean
    public Binding bookingCreatedBinding(Queue bookingCreatedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingCreatedQueue).to(bookingExchange).with("BOOKING_CREATED");
    }
    @Bean
    public Binding bookingFailedBinding(Queue bookingFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingFailedQueue).to(bookingExchange).with("BOOKING_FAILED");
    }
    @Bean
    public Binding bookingCancelledBinding(Queue bookingCancelledQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(bookingCancelledQueue).to(bookingExchange).with("BOOKING_CANCELED");
    }
    @Bean
    public Binding cancelFailedBinding(Queue cancelFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(cancelFailedQueue).to(bookingExchange).with("BOOKING_CANCEL_FAILED");
    }
    @Bean
    public Binding bookingModifiedBinding(Queue modifySuccessQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(modifySuccessQueue).to(bookingExchange).with("BOOKING_MODIFIED");
    }

    @Bean
    public Binding modifyFailedBinding(Queue modifyFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(modifyFailedQueue).to(bookingExchange).with("BOOKING_MODIFY_FAILED");
    }

    // JSON converter for all queues
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}