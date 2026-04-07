package com.example.ikea.service;

import com.example.ikea.domain.*;
import com.example.ikea.dto.*;
import com.example.ikea.repository.MemberRepository;
import com.example.ikea.repository.OrderRepository;
import com.example.ikea.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PaymentService {

    private final OrderRepository orderRepository;
    private final MemberRepository memberRepository;
    private final PaymentRepository paymentRepository;
    private final ProductStockService productStockService;

    @Value("${payment.toss.secret-key}")
    private String tossSecretKey;

    @Value("${payment.toss.base-url}")
    private String tossBaseUrl;

    @Value("${payment.kakao.admin-key}")
    private String kakaoAdminKey;

    @Value("${payment.kakao.base-url}")
    private String kakaoBaseUrl;

    // ============= TOSS ==============

    // 토스 결제 확인
    @Transactional
    public PaymentResponseDto confirmTossPayment(Long memberId, TossConfirmRequestDto dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));

        Order order = orderRepository.findByOrderNo(dto.getOrderNo())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 결제할 수 있습니다.");
        }

        if (!order.getFinalPrice().equals(dto.getAmount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        if (paymentRepository.findByTransactionId(dto.getPaymentKey()).isPresent()) {
            throw new IllegalArgumentException("이미 처리된 결제입니다.");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("이미 결제된 주문입니다.");
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();

            String encoded = Base64.getEncoder()
                    .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", dto.getPaymentKey());
            body.put("orderId", dto.getOrderNo());
            body.put("amount", dto.getAmount());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tossBaseUrl + "/payments/confirm", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = response.getBody();

                for (OrderItem orderItem : order.getOrderItemList()) {
                    productStockService.decreaseStock(
                            orderItem.getProduct().getProductId(),
                            orderItem.getQuantity()
                    );
                }

                Payment payment = Payment.builder()
                        .order(order)
                        .member(member)
                        .paymentMethod(PaymentMethod.TOSS)
                        .transactionId(dto.getPaymentKey())
                        .amount(dto.getAmount())
                        .paymentStatus(PaymentStatus.OK)
                        .responseData(data != null ? data.toString() : null)
                        .paidAt(LocalDateTime.now())
                        .build();

                paymentRepository.save(payment);
                order.setOrderStatus(OrderStatus.PAID);

                return new PaymentResponseDto(payment);
            }
        } catch (Exception e) {
            log.error("토스 결제 확인 실패: {}", e.getMessage());
            throw new IllegalArgumentException("결제 확인에 실패했습니다.");
        }

        throw new IllegalArgumentException("결제 확인에 실패했습니다.");
    }

    // ============== KAKAO ==============

    // 카카오 결제 준비
    @Transactional
    public KakaoReadyResponseDto kakaoReady(Long memberId, KakaoReadyRequestDto dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 결제할 수 있습니다.");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 주문입니다.");
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("cid", "TC0ONETIME");
            body.add("partner_order_id", order.getOrderNo());
            body.add("partner_user_id", String.valueOf(memberId));
            body.add("item_name", "쇼핑몰 주문");
            body.add("quantity", "1");
            body.add("total_amount", String.valueOf(order.getFinalPrice()));
            body.add("tax_free_amount", "0");
            body.add("approval_url", "http://localhost:5173/payment/kakao/success");
            body.add("cancel_url", "http://localhost:5173/payment/kakao/cancel");
            body.add("fail_url", "http://localhost:5173/payment/kakao/fail");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoBaseUrl + "/ready", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = response.getBody();
                return KakaoReadyResponseDto.builder()
                        .tid((String) data.get("tid"))
                        .nextRedirectPcUrl((String) data.get("next_redirect_pc_url"))
                        .nextRedirectMobileUrl((String) data.get("next_redirect_mobile_url"))
                        .build();
            }
        } catch (Exception e) {
            log.error("카카오 결제 준비 실패: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 결제 준비에 실패했습니다.");
        }

        throw new IllegalArgumentException("카카오 결제 준비에 실패했습니다.");
    }

    // 카카오 결제 확인
    @Transactional
    public PaymentResponseDto confirmKakaoPayment(Long memberId, KakaoConfirmRequestDto dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 결제할 수 있습니다.");
        }

        if (paymentRepository.findByTransactionId(dto.getTid()).isPresent()) {
            throw new IllegalArgumentException("이미 처리된 결제입니다.");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 주문입니다.");
        }

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("cid", "TC0ONETIME");
            body.add("tid", dto.getTid());
            body.add("partner_order_id", order.getOrderNo());
            body.add("partner_user_id", String.valueOf(memberId));
            body.add("pg_token", dto.getPgToken());

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoBaseUrl + "/approve", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = response.getBody();

                for (OrderItem orderItem : order.getOrderItemList()) {
                    productStockService.decreaseStock(
                            orderItem.getProduct().getProductId(),
                            orderItem.getQuantity()
                    );
                }

                Payment payment = Payment.builder()
                        .order(order)
                        .member(member)
                        .paymentMethod(PaymentMethod.KAKAO)
                        .transactionId(dto.getTid())
                        .amount(order.getFinalPrice())
                        .paymentStatus(PaymentStatus.OK)
                        .responseData(data != null ? data.toString() : null)
                        .paidAt(LocalDateTime.now())
                        .build();

                paymentRepository.save(payment);
                order.setOrderStatus(OrderStatus.PAID);

                return new PaymentResponseDto(payment);
            }
        } catch (Exception e) {
            log.error("카카오 결제 확인 실패: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 결제 확인에 실패했습니다.");
        }

        throw new IllegalArgumentException("카카오 결제 확인에 실패했습니다.");
    }

    // ============= 공통 ===============

    // 결제 취소
    @Transactional
    public void cancelPayment(Long orderId, Long memberId, String reason) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("주문 취소 권한이 없습니다.");
        }

        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        if (payment.getPaymentStatus() == PaymentStatus.CANCEL) {
            throw new IllegalArgumentException("이미 취소된 결제입니다.");
        }

        if (payment.getPaymentStatus() != PaymentStatus.OK) {
            throw new IllegalArgumentException("취소 가능한 결제가 아닙니다.");
        }

        if (order.getOrderStatus() != OrderStatus.PAID && order.getOrderStatus() != OrderStatus.ORDERED) {
            throw new IllegalArgumentException("현재 주문 상태에서는 결제를 취소할 수 없습니다.");
        }

        if (payment.getPaymentMethod() == PaymentMethod.TOSS) {
            cancelTossPayment(payment.getTransactionId(), payment.getAmount(), reason);
        } else if (payment.getPaymentMethod() == PaymentMethod.KAKAO) {
            cancelKakaoPayment(payment.getTransactionId(), payment.getAmount());
        }

        payment.setPaymentStatus(PaymentStatus.CANCEL);
        payment.setCancelReason(reason);
        payment.setCancelledAt(LocalDateTime.now());

        for (OrderItem orderItem : order.getOrderItemList()) {
            productStockService.increaseStock(
                    orderItem.getProduct().getProductId(),
                    orderItem.getQuantity()
            );
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
    }

    // Toss 취소 API 호출
    private void cancelTossPayment(String transactionId, Integer amount, String reason) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            String encoded = Base64.getEncoder()
                    .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
            headers.set("Authorization", "Basic " + encoded);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("cancelReason", reason);
            body.put("cancelAmount", amount);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tossBaseUrl + "/payments/" + transactionId + "/cancel", entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new IllegalArgumentException("토스 결제 취소에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("토스 결제 취소 실패: {}", e.getMessage());
            throw new IllegalArgumentException("토스 결제 취소에 실패했습니다.");
        }
    }

    // Kakao 취소 API 호출
    private void cancelKakaoPayment(String tid, Integer amount) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoAdminKey);
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("cid", "TC0ONETIME");
            body.add("tid", tid);
            body.add("cancel_amount", String.valueOf(amount));
            body.add("cancel_tax_free_amount", "0");

            HttpEntity<MultiValueMap<String, String>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoBaseUrl + "/cancel", entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new IllegalArgumentException("카카오 결제 취소에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("카카오 결제 취소 실패: {}", e.getMessage());
            throw new IllegalArgumentException("카카오 결제 취소에 실패했습니다.");
        }
    }

    // 내 결제 목록
    public List<PaymentResponseDto> getMyPaymentList(Long memberId) {
        return paymentRepository.findByMember_MemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(PaymentResponseDto::new)
                .collect(Collectors.toList());
    }

    // 관리자 전용 전체 결제 목록
    public List<PaymentResponseDto> getAllPaymentList() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentResponseDto::new)
                .collect(Collectors.toList());
    }
}