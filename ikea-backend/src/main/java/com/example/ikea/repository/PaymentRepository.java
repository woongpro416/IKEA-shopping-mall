package com.example.ikea.repository;

import com.example.ikea.domain.Payment;
import com.example.ikea.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository  extends JpaRepository<Payment, Long> {

    Optional<Payment> findByOrder_OrderId(Long orderId);

    Optional<Payment> findByTransactionId(String transactionId);

    List<Payment> findByMember_MemberIdOrderByCreatedAtDesc(Long memberId);

    boolean existsByMember_MemberIdAndPaymentStatus(Long memerId, PaymentStatus paymentStatus);
}
