package com.example.ikea.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class GuestOrderRequestDto {

    @NotBlank(message = "비회원 장바구니 키를 입력해주세요.")
    private String guestCartKey;

    @NotBlank(message = "비회원 이름을 입력해주세요.")
    private String guestName;

    @NotBlank(message = "비회원 연락처를 입력해주세요.")
    private String guestPhone;

    @NotBlank(message = "주소를 입력해주세요.")
    private String address;

    private String paymentMethod;

    private Integer productTotal;

    private Integer discountTotal;

    private Integer couponDiscount;

    private Integer pointApplied;

    private Integer shippingTotal;

    private Integer finalTotal;
}
