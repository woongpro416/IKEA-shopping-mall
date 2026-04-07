package com.example.ikea.dto;

import com.example.ikea.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuestOrderCreateResponseDto {
    private Long orderId;

    private String orderNo;

    private OrderStatus orderStatus;
}
