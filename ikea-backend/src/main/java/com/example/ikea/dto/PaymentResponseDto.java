package com.example.ikea.dto;

import com.example.ikea.domain.Order;
import com.example.ikea.domain.Payment;
import com.example.ikea.domain.PaymentMethod;
import com.example.ikea.domain.PaymentStatus;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PaymentResponseDto {

    private Long paymentId;
    private Long orderId;
    private String orderNo;
    private PaymentMethod paymentMethod;
    private String transactionId;
    private Integer amount;
    private PaymentStatus paymentStatus;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;

    public PaymentResponseDto(Payment payment) {
        Order order = payment.getOrder();

        this.paymentId = payment.getPaymentId();
        this.orderId = order != null ? order.getOrderId() : null;
        this.orderNo = order != null ? order.getOrderNo() : null;
        this.paymentMethod = payment.getPaymentMethod();
        this.transactionId = payment.getTransactionId();
        this.amount = payment.getAmount();
        this.paymentStatus = payment.getPaymentStatus();
        this.createdAt = payment.getCreatedAt();
        this.paidAt = payment.getPaidAt();
    }
}
