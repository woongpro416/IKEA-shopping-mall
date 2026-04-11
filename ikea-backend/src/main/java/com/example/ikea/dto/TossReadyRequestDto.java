package com.example.ikea.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossReadyRequestDto {

    @NotNull(message = "주문Id를 입력해주세요.")
    private Long orderId;

    private String orderNo;

    private Integer amount;

    private String successUrl;

    private String failUrl;
}
