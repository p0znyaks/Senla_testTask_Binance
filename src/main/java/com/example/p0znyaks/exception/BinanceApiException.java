package com.example.p0znyaks.exception;

import lombok.Getter;

@Getter
public class BinanceApiException extends RuntimeException {
    private final int statusCode;
    private final int binanceCode;
    private final Long retryAfterSeconds;

    public BinanceApiException(String message, int statusCode, int binanceCode, Long retryAfterSeconds) {
        super(message);
        this.statusCode = statusCode;
        this.binanceCode = binanceCode;
        this.retryAfterSeconds = retryAfterSeconds;
    }
}
