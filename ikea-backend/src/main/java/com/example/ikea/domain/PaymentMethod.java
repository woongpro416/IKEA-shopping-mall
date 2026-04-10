package com.example.ikea.domain;

public enum PaymentMethod {
    TOSS,
    KAKAO,
    BANK_TRANSFER;

    public static PaymentMethod fromRequest(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }

        String normalized = rawValue.trim().toUpperCase();

        return switch (normalized) {
            case "TOSS" -> TOSS;
            case "KAKAO" -> KAKAO;
            case "BANK_TRANSFER", "ACCOUNT_TRANSFER", "VBANK", "BANK" -> BANK_TRANSFER;
            default -> throw new IllegalArgumentException("지원하지 않는 결제 수단입니다: " + rawValue);
        };
    }

    public boolean isBankTransfer() {
        return this == BANK_TRANSFER;
    }
}
