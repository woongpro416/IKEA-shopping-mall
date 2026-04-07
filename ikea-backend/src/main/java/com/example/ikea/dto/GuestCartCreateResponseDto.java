package com.example.ikea.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class GuestCartCreateResponseDto {

    private String guestCartKey;

    private Long cartId;
}
