package com.example.ikea.service;

import com.example.ikea.domain.Cart;
import com.example.ikea.domain.CartItem;
import com.example.ikea.domain.Member;
import com.example.ikea.domain.Order;
import com.example.ikea.domain.OrderItem;
import com.example.ikea.domain.OrderStatus;
import com.example.ikea.domain.Product;
import com.example.ikea.dto.GuestOrderCreateResponseDto;
import com.example.ikea.dto.GuestOrderRequestDto;
import com.example.ikea.dto.MemberOrderItemRequestDto;
import com.example.ikea.dto.MemberOrderRequestDto;
import com.example.ikea.dto.OrderResponseDto;
import com.example.ikea.repository.CartItemRepository;
import com.example.ikea.repository.CartRepository;
import com.example.ikea.repository.MemberRepository;
import com.example.ikea.repository.OrderItemRepository;
import com.example.ikea.repository.OrderRepository;
import lombok.AllArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Transactional(readOnly = true)
@Service
@AllArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final MemberRepository memberRepository;
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductStockService productStockService;
    private final ProductService productService;
    private final PaymentService paymentService;

    // 주문 목록 조회(내 주문 내역)
    public List<OrderResponseDto> getOrderList(Long memberId) {
        return orderRepository.findByMember_MemberId(memberId).stream()
                .map(OrderResponseDto::new)
                .collect(Collectors.toList());
    }

    // 주문 상세 조회
    public OrderResponseDto getDetailOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 조회할 수 있습니다.");
        }

        return new OrderResponseDto(order);
    }

    // 주문 생성 (회원)
    @Transactional
    public Long createMemberOrder(Long memberId, MemberOrderRequestDto dto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 회원입니다."));

        List<OrderLine> orderLines;

        if (dto.getOrderItems() != null && !dto.getOrderItems().isEmpty()) {
            orderLines = buildOrderLinesFromRequest(dto.getOrderItems());
        } else {
            orderLines = buildOrderLinesFromCart(memberId);
        }

        if (orderLines.isEmpty()) {
            throw new IllegalArgumentException("주문 항목이 없습니다.");
        }

        int totalPrice = orderLines.stream()
                .mapToInt(line -> line.product().getPrice() * line.quantity())
                .sum();

        String orderNo = "ORDER_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + UUID.randomUUID().toString().substring(0, 8);

        Order order = Order.builder()
                .member(member)
                .orderNo(orderNo)
                .orderStatus(OrderStatus.PENDING)
                .totalPrice(totalPrice)
                .finalPrice(totalPrice)
                .address(dto.getAddress())
                .guestName(null)
                .guestPhone(null)
                .build();

        order = orderRepository.save(order);

        for (OrderLine line : orderLines) {
            productStockService.decreaseStock(line.product().getProductId(), line.quantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(line.product())
                    .quantity(line.quantity())
                    .orderPrice(line.product().getPrice())
                    .build();

            orderItemRepository.save(orderItem);
        }

        paymentService.registerBankTransferPaymentIfNeeded(order, member, dto.getPaymentMethod());

        if (dto.getOrderItems() == null || dto.getOrderItems().isEmpty()) {
            Cart cart = cartRepository.findByMember_MemberId(memberId)
                    .orElseThrow(() -> new IllegalStateException("존재하지 않는 장바구니입니다."));
            cartItemRepository.deleteByCart_CartId(cart.getCartId());
        }

        return order.getOrderId();
    }

    private List<OrderLine> buildOrderLinesFromCart(Long memberId) {
        Cart cart = cartRepository.findByMember_MemberId(memberId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 장바구니입니다."));

        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어있습니다.");
        }

        List<OrderLine> lines = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            if (cartItem.getQuantity() == null || cartItem.getQuantity() < 1) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
            }
            lines.add(new OrderLine(cartItem.getProduct(), cartItem.getQuantity()));
        }
        return lines;
    }

    private List<OrderLine> buildOrderLinesFromRequest(List<MemberOrderItemRequestDto> items) {
        List<OrderLine> lines = new ArrayList<>();

        for (MemberOrderItemRequestDto item : items) {
            if (item.getQuantity() == null || item.getQuantity() < 1) {
                throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");
            }

            Product product = productService.findProductEntityByRequest(
                    item.getProductId(),
                    item.getProductCode()
            );

            lines.add(new OrderLine(product, item.getQuantity()));
        }

        return lines;
    }

    private record OrderLine(Product product, Integer quantity) {
    }

    // 주문 취소 (미결제 주문만)
    @Transactional
    public void cancelOrder(Long orderId, Long memberId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        if (order.getMember() == null || !order.getMember().getMemberId().equals(memberId)) {
            throw new AccessDeniedException("본인 주문만 취소할 수 있습니다.");
        }

        if (order.getOrderStatus() == OrderStatus.CANCELLED) {
            throw new IllegalArgumentException("이미 취소된 주문입니다.");
        }

        if (order.getOrderStatus() != OrderStatus.PENDING) {
            throw new IllegalArgumentException("미결제 주문만 여기서 취소할 수 있습니다. 결제 완료 주문은 결제 취소를 이용하세요.");
        }

        order.setOrderStatus(OrderStatus.CANCELLED);
    }

    // ====================== 관리자 ===================

    public List<OrderResponseDto> getAllOrderList() {
        return orderRepository.findAll().stream()
                .map(OrderResponseDto::new)
                .collect(Collectors.toList());
    }

    public List<OrderResponseDto> getOrderListByStatus(OrderStatus status) {
        return orderRepository.findByOrderStatus(status).stream()
                .map(OrderResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateOrderStatus(OrderStatus status, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 주문입니다."));

        validateOrderStatusChange(order.getOrderStatus(), status);
        order.setOrderStatus(status);
        paymentService.syncPaymentStatusByOrderStatus(order);
    }

    public Long getOrderCount() {
        return orderRepository.count();
    }

    // ====================== 비회원 전용 =================

    @Transactional
    public GuestOrderCreateResponseDto createGuestOrder(GuestOrderRequestDto dto) {
        Cart cart = cartRepository.findByGuestCartKey(dto.getGuestCartKey())
                .orElseThrow(() -> new IllegalStateException("존재하지 않는 비회원 장바구니입니다."));

        List<CartItem> cartItems = cartItemRepository.findByCart_CartId(cart.getCartId());

        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("장바구니가 비어있습니다.");
        }

        int totalPrice = cartItems.stream()
                .mapToInt(cartItem -> cartItem.getProduct().getPrice() * cartItem.getQuantity())
                .sum();

        String orderNo = "ORDER_"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
                + "_" + UUID.randomUUID().toString().substring(0, 8);

        Order order = Order.builder()
                .member(null)
                .orderNo(orderNo)
                .orderStatus(OrderStatus.PENDING)
                .totalPrice(totalPrice)
                .finalPrice(totalPrice)
                .address(dto.getAddress())
                .guestName(dto.getGuestName())
                .guestPhone(dto.getGuestPhone())
                .build();

        order = orderRepository.save(order);

        for (CartItem cartItem : cartItems) {
            productStockService.decreaseStock(cartItem.getProduct().getProductId(), cartItem.getQuantity());

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .orderPrice(cartItem.getProduct().getPrice())
                    .build();

            orderItemRepository.save(orderItem);
        }

        paymentService.registerBankTransferPaymentIfNeeded(order, null, dto.getPaymentMethod());

        cartItemRepository.deleteByCart_CartId(cart.getCartId());

        return new GuestOrderCreateResponseDto(
                order.getOrderId(),
                order.getOrderNo(),
                order.getOrderStatus()
        );
    }

    private void validateOrderStatusChange(OrderStatus current, OrderStatus next) {
        if (current == OrderStatus.CANCELLED || current == OrderStatus.COMPLETED) {
            throw new IllegalArgumentException("종료된 주문 상태는 변경할 수 없습니다.");
        }

        boolean valid =
                (current == OrderStatus.PENDING && next == OrderStatus.PAID) ||
                        (current == OrderStatus.PAID && next == OrderStatus.ORDERED) ||
                        (current == OrderStatus.ORDERED && next == OrderStatus.DELIVERING) ||
                        (current == OrderStatus.DELIVERING && next == OrderStatus.COMPLETED);

        if (!valid) {
            throw new IllegalArgumentException("허용되지 않는 주문 상태 변경입니다.");
        }
    }
}
