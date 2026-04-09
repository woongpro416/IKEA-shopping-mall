package com.example.ikea.exception;

public class ProductStockNotFoundException extends RuntimeException {

    public ProductStockNotFoundException(String message) {
        super(message);
    }
}