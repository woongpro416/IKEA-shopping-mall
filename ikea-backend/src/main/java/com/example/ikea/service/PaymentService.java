package com.example.ikea.service;

import com.example.ikea.domain.Member;
import com.example.ikea.domain.Order;
import com.example.ikea.domain.OrderItem;
import com.example.ikea.domain.OrderStatus;
import com.example.ikea.domain.Payment;
import com.example.ikea.domain.PaymentMethod;
import com.example.ikea.domain.PaymentStatus;
import com.example.ikea.dto.KakaoConfirmRequestDto;
import com.example.ikea.dto.KakaoReadyRequestDto;
import com.example.ikea.dto.KakaoReadyResponseDto;
import com.example.ikea.dto.PaymentResponseDto;
import com.example.ikea.dto.TossConfirmRequestDto;
import com.example.ikea.dto.TossReadyRequestDto;
import com.example.ikea.dto.TossReadyResponseDto;
import com.example.ikea.repository.MemberRepository;
import com.example.ikea.repository.OrderRepository;
import com.example.ikea.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

    @Value("${payment.kakao.secret-key}")
    private String kakaoSecretKey;

    @Value("${payment.kakao.base-url}")
    private String kakaoBaseUrl;

    @Value("${payment.kakao.cid:TC0ONETIME}")
    private String kakaoCid;

    @Value("${payment.redirect-base-url:http://localhost:5173}")
    private String paymentRedirectBaseUrl;

    // ============= TOSS ==============

    // 토스 결제 준비
    @Transactional
    public TossReadyResponseDto tossReady(Long memberId, TossReadyRequestDto dto) {
        Order order = getMemberOrderForPayment(memberId, dto.getOrderId());
        validatePendingOrder(order);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = buildTossHeaders();

            Map<String, Object> body = new HashMap<>();
            body.put("method", "CARD");
            body.put("amount", order.getFinalPrice());
            body.put("orderId", order.getOrderNo());
            body.put("orderName", resolveOrderName(order));
            body.put("successUrl", resolvePaymentRedirectUrl(dto.getSuccessUrl(), "/payment/toss/success"));
            body.put("failUrl", resolvePaymentRedirectUrl(dto.getFailUrl(), "/payment/toss/fail"));
            body.put("flowMode", "DIRECT");
            body.put("easyPay", "TOSSPAY");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tossBaseUrl + "/payments", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String checkoutUrl = resolveTossCheckoutUrl(response.getBody());

                if (checkoutUrl == null || checkoutUrl.isBlank()) {
                    throw new IllegalArgumentException("토스 결제창 URL을 확인할 수 없습니다.");
                }

                return TossReadyResponseDto.builder()
                        .orderId(order.getOrderId())
                        .orderNo(order.getOrderNo())
                        .redirectUrl(checkoutUrl)
                        .build();
            }
        } catch (Exception e) {
            log.error("토스 결제 준비 실패: {}", e.getMessage(), e);
            throw new IllegalArgumentException("토스 결제 준비에 실패했습니다.");
        }

        throw new IllegalArgumentException("토스 결제 준비에 실패했습니다.");
    }

    // 비회원 토스 결제 준비
    @Transactional
    public TossReadyResponseDto tossReadyForGuest(TossReadyRequestDto dto) {
        Order order = getGuestOrderForPayment(dto.getOrderId());
        validatePendingOrder(order);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = buildTossHeaders();

            Map<String, Object> body = new HashMap<>();
            body.put("method", "CARD");
            body.put("amount", order.getFinalPrice());
            body.put("orderId", order.getOrderNo());
            body.put("orderName", resolveOrderName(order));
            body.put("successUrl", resolvePaymentRedirectUrl(dto.getSuccessUrl(), "/payment/toss/success"));
            body.put("failUrl", resolvePaymentRedirectUrl(dto.getFailUrl(), "/payment/toss/fail"));
            body.put("flowMode", "DIRECT");
            body.put("easyPay", "TOSSPAY");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tossBaseUrl + "/payments", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                String checkoutUrl = resolveTossCheckoutUrl(response.getBody());

                if (checkoutUrl == null || checkoutUrl.isBlank()) {
                    throw new IllegalArgumentException("토스 결제창 URL을 확인할 수 없습니다.");
                }

                return TossReadyResponseDto.builder()
                        .orderId(order.getOrderId())
                        .orderNo(order.getOrderNo())
                        .redirectUrl(checkoutUrl)
                        .build();
            }
        } catch (Exception e) {
            log.error("비회원 토스 결제 준비 실패: {}", e.getMessage(), e);
            throw new IllegalArgumentException("토스 결제 준비에 실패했습니다.");
        }

        throw new IllegalArgumentException("토스 결제 준비에 실패했습니다.");
    }

    // 토스 결제 확인
    @Transactional
    public PaymentResponseDto confirmTossPayment(Long memberId, TossConfirmRequestDto dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 회원입니다."));
        Order order = getMemberOrderForPayment(memberId, dto.getOrderNo());

        return confirmTossPayment(order, member, dto);
    }

    // 비회원 토스 결제 확인
    @Transactional
    public PaymentResponseDto confirmGuestTossPayment(TossConfirmRequestDto dto) {
        Order order = getGuestOrderForPayment(dto.getOrderNo());
        return confirmTossPayment(order, null, dto);
    }

    // ============== KAKAO ==============

    // 카카오 결제 준비
    @Transactional
    public KakaoReadyResponseDto kakaoReady(Long memberId, KakaoReadyRequestDto dto) {
        Order order = getMemberOrderForPayment(memberId, dto.getOrderId());
        return requestKakaoReady(order, String.valueOf(memberId), dto);
    }

    // 비회원 카카오 결제 준비
    @Transactional
    public KakaoReadyResponseDto kakaoReadyForGuest(KakaoReadyRequestDto dto) {
        Order order = getGuestOrderForPayment(dto.getOrderId());
        return requestKakaoReady(order, "guest-" + order.getOrderId(), dto);
    }

    // 카카오 결제 확인
    @Transactional
    public PaymentResponseDto confirmKakaoPayment(Long memberId, KakaoConfirmRequestDto dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));
        Order order = getMemberOrderForPayment(memberId, dto.getOrderId());

        return confirmKakaoPayment(order, member, dto, String.valueOf(memberId));
    }

    // 비회원 카카오 결제 확인
    @Transactional
    public PaymentResponseDto confirmGuestKakaoPayment(KakaoConfirmRequestDto dto) {
        Order order = getGuestOrderForPayment(dto.getOrderId());
        return confirmKakaoPayment(order, null, dto, "guest-" + order.getOrderId());
    }

    private PaymentResponseDto confirmTossPayment(Order order, Member member, TossConfirmRequestDto dto) {
        if (!order.getFinalPrice().equals(dto.getAmount())) {
            throw new IllegalArgumentException("결제 금액이 일치하지 않습니다.");
        }

        if (paymentRepository.findByTransactionId(dto.getPaymentKey()).isPresent()) {
            throw new IllegalArgumentException("이미 처리된 결제입니다.");
        }

        validatePendingOrder(order);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = buildTossHeaders();

            Map<String, Object> body = new HashMap<>();
            body.put("paymentKey", dto.getPaymentKey());
            body.put("orderId", order.getOrderNo());
            body.put("amount", dto.getAmount());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    tossBaseUrl + "/payments/confirm", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = response.getBody();
                Payment payment = buildApprovedPayment(order, member, PaymentMethod.TOSS, dto.getPaymentKey(), dto.getAmount(), data);
                return new PaymentResponseDto(payment);
            }
        } catch (Exception e) {
            log.error("토스 결제 확인 실패: {}", e.getMessage(), e);
            throw new IllegalArgumentException("결제 확인에 실패했습니다.");
        }

        throw new IllegalArgumentException("결제 확인에 실패했습니다.");
    }

    private KakaoReadyResponseDto requestKakaoReady(Order order, String partnerUserId, KakaoReadyRequestDto dto) {
        validatePendingOrder(order);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = buildKakaoHeaders();

            Map<String, Object> body = new HashMap<>();
            body.put("cid", kakaoCid);
            body.put("partner_order_id", order.getOrderNo());
            body.put("partner_user_id", partnerUserId);
            body.put("item_name", resolveOrderName(order));
            body.put("quantity", 1);
            body.put("total_amount", order.getFinalPrice());
            body.put("tax_free_amount", 0);
            body.put("approval_url", resolvePaymentRedirectUrl(dto.getSuccessUrl(), "/payment/kakao/success"));
            body.put("cancel_url", resolvePaymentRedirectUrl(dto.getCancelUrl(), "/payment/kakao/cancel"));
            body.put("fail_url", resolvePaymentRedirectUrl(dto.getFailUrl(), "/payment/kakao/fail"));

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
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
            log.error("카카오 결제 준비 실패: {}", e.getMessage(), e);
            throw new IllegalArgumentException("카카오 결제 준비에 실패했습니다.");
        }

        throw new IllegalArgumentException("카카오 결제 준비에 실패했습니다.");
    }

    private PaymentResponseDto confirmKakaoPayment(Order order, Member member, KakaoConfirmRequestDto dto, String partnerUserId) {
        if (paymentRepository.findByTransactionId(dto.getTid()).isPresent()) {
            throw new IllegalArgumentException("이미 처리된 결제입니다.");
        }

        validatePendingOrder(order);

        try {
            RestTemplate restTemplate = new RestTemplate();
            HttpHeaders headers = buildKakaoHeaders();

            Map<String, Object> body = new HashMap<>();
            body.put("cid", kakaoCid);
            body.put("tid", dto.getTid());
            body.put("partner_order_id", order.getOrderNo());
            body.put("partner_user_id", partnerUserId);
            body.put("pg_token", dto.getPgToken());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoBaseUrl + "/approve", entity, Map.class);

            if (response.getStatusCode() == HttpStatus.OK) {
                Map<String, Object> data = response.getBody();
                Payment payment = buildApprovedPayment(order, member, PaymentMethod.KAKAO, dto.getTid(), order.getFinalPrice(), data);
                return new PaymentResponseDto(payment);
            }
        } catch (Exception e) {
            log.error("카카오 결제 확인 실패: {}", e.getMessage(), e);
            throw new IllegalArgumentException("카카오 결제 확인에 실패했습니다.");
        }

        throw new IllegalArgumentException("카카오 결제 확인에 실패했습니다.");
    }

    private Payment buildApprovedPayment(
            Order order,
            Member member,
            PaymentMethod paymentMethod,
            String transactionId,
            Integer amount,
            Map<String, Object> responseData
    ) {
        Payment payment = paymentRepository.findByOrder_OrderId(order.getOrderId())
                .orElseGet(() -> Payment.builder()
                        .order(order)
                        .member(member)
                        .paymentMethod(paymentMethod)
                        .amount(amount)
                        .build());

        payment.setMember(member);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionId(transactionId);
        payment.setAmount(amount);
        payment.setPaymentStatus(PaymentStatus.OK);
        payment.setResponseData(responseData != null ? responseData.toString() : null);
        payment.setPaidAt(LocalDateTime.now());

        Payment savedPayment = paymentRepository.save(payment);
        order.setOrderStatus(OrderStatus.PAID);
        return savedPayment;
    }

    private HttpHeaders buildTossHeaders() {
        HttpHeaders headers = new HttpHeaders();
        String encoded = Base64.getEncoder()
                .encodeToString((tossSecretKey + ":").getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private HttpHeaders buildKakaoHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "SECRET_KEY " + kakaoSecretKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        return headers;
    }

    private Order getMemberOrderForPayment(Long memberId, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 결제할 수 있습니다.");
        }

        return order;
    }

    private Order getMemberOrderForPayment(Long memberId, String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 결제할 수 있습니다.");
        }

        return order;
    }

    private Order getGuestOrderForPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() != null) {
            throw new AccessDeniedException("비회원 주문만 결제할 수 있습니다.");
        }

        return order;
    }

    private Order getGuestOrderForPayment(String orderNo) {
        Order order = orderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() != null) {
            throw new AccessDeniedException("비회원 주문만 결제할 수 있습니다.");
        }

        return order;
    }

    private void validatePendingOrder(Order order) {
        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("이미 처리된 주문입니다.");
        }
    }

    private String resolveOrderName(Order order) {
        if (order.getOrderItemList() == null || order.getOrderItemList().isEmpty()) {
            return "쇼핑몰 주문";
        }

        OrderItem firstItem = order.getOrderItemList().get(0);
        String firstProductName = firstItem.getProduct().getName();

        if (order.getOrderItemList().size() == 1) {
            return firstProductName;
        }

        return firstProductName + " 외 " + (order.getOrderItemList().size() - 1) + "건";
    }

    private String resolvePaymentRedirectUrl(String requestedUrl, String fallbackPath) {
        if (requestedUrl != null && !requestedUrl.isBlank()) {
            return requestedUrl;
        }

        return paymentRedirectBaseUrl + fallbackPath;
    }

    private String resolveTossCheckoutUrl(Map<String, Object> responseData) {
        if (responseData == null) {
            return null;
        }

        Object redirectUrl = responseData.get("checkoutUrl");
        if (redirectUrl instanceof String value && !value.isBlank()) {
            return value;
        }

        Object checkout = responseData.get("checkout");
        if (checkout instanceof Map<?, ?> checkoutMap) {
            Object url = checkoutMap.get("url");
            if (url instanceof String value && !value.isBlank()) {
                return value;
            }
        }

        return null;
    }

    // ============= 공통 ===============

    @Transactional
    public void registerBankTransferPaymentIfNeeded(Order order, Member member, String rawPaymentMethod) {
        PaymentMethod paymentMethod = PaymentMethod.fromRequest(rawPaymentMethod);

        if (paymentMethod == null || !paymentMethod.isBankTransfer()) {
            return;
        }

        if (paymentRepository.existsByOrder_OrderId(order.getOrderId())) {
            return;
        }

        Payment payment = Payment.builder()
                .order(order)
                .member(member)
                .paymentMethod(PaymentMethod.BANK_TRANSFER)
                .amount(order.getFinalPrice())
                .paymentStatus(PaymentStatus.PENDING)
                .build();

        paymentRepository.save(payment);
    }

    @Transactional
    public void confirmBankTransferPayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        Payment payment = paymentRepository.findByOrder_OrderId(orderId)
                .orElseThrow(() -> new IllegalStateException("결제 정보를 찾을 수 없습니다."));

        if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
            throw new IllegalArgumentException("무통장 입금 주문만 확인할 수 있습니다.");
        }

        if (payment.getPaymentStatus() == PaymentStatus.OK) {
            throw new IllegalArgumentException("이미 결제 확인이 완료되었습니다.");
        }

        payment.setPaymentStatus(PaymentStatus.OK);
        payment.setPaidAt(LocalDateTime.now());
        order.setOrderStatus(OrderStatus.PAID);
    }

    @Transactional
    public void syncPaymentStatusByOrderStatus(Order order) {
        paymentRepository.findByOrder_OrderId(order.getOrderId()).ifPresent(payment -> {
            if (payment.getPaymentMethod() != PaymentMethod.BANK_TRANSFER) {
                return;
            }

            if (order.getOrderStatus() == OrderStatus.PAID && payment.getPaymentStatus() == PaymentStatus.PENDING) {
                payment.setPaymentStatus(PaymentStatus.OK);
                if (payment.getPaidAt() == null) {
                    payment.setPaidAt(LocalDateTime.now());
                }
            }
        });
    }

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
            headers.set("Authorization", "SECRET_KEY " + kakaoSecretKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("cid", "TC0ONETIME");
            body.put("tid", tid);
            body.put("cancel_amount", amount);
            body.put("cancel_tax_free_amount", 0);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    kakaoBaseUrl + "/cancel", entity, Map.class);

            if (response.getStatusCode() != HttpStatus.OK) {
                throw new IllegalArgumentException("카카오 결제 취소에 실패했습니다.");
            }
        } catch (Exception e) {
            log.error("카카오 결제 취소 실패: {}", e.getMessage(), e);
            throw new IllegalArgumentException("카카오 결제 취소에 실패했습니다.");
        }
    }

    // 관리자 전용 전체 결제 목록
    public List<PaymentResponseDto> getAllPaymentList() {
        return paymentRepository.findAll()
                .stream()
                .map(PaymentResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<PaymentResponseDto> getMyPaymentList(Long memberId) {
        return paymentRepository.findByMember_MemberIdOrderByCreatedAtDesc(memberId)
                .stream()
                .map(PaymentResponseDto::new)
                .collect(Collectors.toList());
    }
}
