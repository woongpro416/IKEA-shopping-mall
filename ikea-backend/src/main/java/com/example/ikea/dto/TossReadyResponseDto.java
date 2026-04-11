package com.example.ikea.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class TossReadyResponseDto {

    private Long orderId;
    private String orderNo;
    private String redirectUrl;
}
