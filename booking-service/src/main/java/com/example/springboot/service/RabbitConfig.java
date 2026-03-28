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
    @Bean
    public Queue refundSuccessQueue() {
        return new Queue("refund.succeeded.queue");
    }

    @Bean
    public Queue refundFailedQueue() {
        return new Queue("refund.failed.queue");
    }

    @Bean
    public Queue paymentSuccessQueue() {
        return new Queue("payment.succeeded.queue");
    }

    @Bean
    public Queue paymentFailedQueue() {
        return new Queue("payment.failed.queue");
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
    public Binding bookingCancelFailedBinding(Queue cancelFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(cancelFailedQueue).to(bookingExchange).with("BOOKING_CANCEL_FAILED");
    }
    @Bean
    public Binding bookingModifiedBinding(Queue modifySuccessQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(modifySuccessQueue).to(bookingExchange).with("BOOKING_MODIFIED");
    }
    @Bean
    public Binding bookingModifyFailedBinding(Queue modifySuccessQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(modifySuccessQueue).to(bookingExchange).with("BOOKING_MODIFY_FAILED");
    }
    @Bean
    public Binding bookingRefundFailedBinding(Queue refundFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(refundFailedQueue).to(bookingExchange).with("BOOKING_REFUND_FAILED");
    }

    @Bean
    public Binding bookingRefundedBinding(Queue refundSuccessQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(refundSuccessQueue).to(bookingExchange).with("BOOKING_REFUNDED");
    }
    @Bean
    public Binding modifyPaymentFailedBinding(Queue paymentFailedQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentFailedQueue).to(bookingExchange).with("BOOKING_PAYMENT_FAILED");
    }

    @Bean
    public Binding modifyPaymentSucceededBinding(Queue paymentSuccessQueue, TopicExchange bookingExchange) {
        return BindingBuilder.bind(paymentSuccessQueue).to(bookingExchange).with("BOOKING_PAYMENT_SUCCEEDED");
    }

    // JSON converter for all queues
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }
}