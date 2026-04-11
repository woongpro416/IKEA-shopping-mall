package com.example.ikea.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TossConfirmRequestDto {

    @NotBlank(message = "paymentKey를 입력해주세요.")
    private String paymentKey;

    //토스에 넘길 주문번호
    @NotBlank(message = "주문번호를 입력해주세요.")
    private String orderNo;

    @NotNull(message = "가격은 필수 입력입니다.")
    private Integer amount;
}
