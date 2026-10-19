package com.example.p0znyaks.exception;

import lombok.Getter;

@Getter
public class PriceNotFoundException extends RuntimeException {
    public PriceNotFoundException(String message) {
        super(message);
    }
}