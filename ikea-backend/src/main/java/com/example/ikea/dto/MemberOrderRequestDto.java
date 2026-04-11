package com.example.ikea.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MemberOrderRequestDto {

    @NotBlank(message = "주소는 필수 입력입니다.")
    private String address;

    private String paymentMethod;

    private Integer productTotal;

    private Integer discountTotal;

    private Integer couponDiscount;

    private Integer pointApplied;

    private Integer shippingTotal;

    private Integer finalTotal;

    @Valid
    private List<MemberOrderItemRequestDto> orderItems;
}
