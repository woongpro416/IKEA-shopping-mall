package com.example.ikea.domain;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "orders")
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    //주문 고유번호 - (토스/카카오에 전달할 번호)
    @Column(unique = true, nullable = false, length = 50)
    private String orderNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private OrderStatus orderStatus = OrderStatus.PENDING;

    @Column(nullable = false)
    private Integer totalPrice;

    //최종 결제 금액 ( 할인 등 적용 후 )
    @Column(nullable = false)
    private Integer finalPrice;

    @OneToOne(mappedBy = "order", fetch = FetchType.LAZY)
    private Payment payment;

    @Column(nullable = false, length = 254)
    private String address;

    @Builder.Default
    @OneToMany(mappedBy = "order")
    private List<OrderItem> orderItemList = new ArrayList<>();

    //비회원

    @Column(length = 50)
    private String guestName;

    @Column(length = 20)
    private String guestPhone;

}
