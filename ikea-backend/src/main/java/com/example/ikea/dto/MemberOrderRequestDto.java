package com.example.ikea.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MemberOrderRequestDto {

    @NotBlank(message = "주소는 필수 입력")
    private String address;

}
