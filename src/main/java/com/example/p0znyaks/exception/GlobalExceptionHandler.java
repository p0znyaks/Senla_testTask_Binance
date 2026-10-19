package com.example.p0znyaks.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PriceNotFoundException.class)
    public ResponseEntity<Void> handlePriceNotFound(PriceNotFoundException ex) {
        return ResponseEntity.notFound().build();
    }

    @ExceptionHandler(BinanceApiException.class)
    public ResponseEntity<Map<String, Object>> handleBinanceApi(BinanceApiException ex) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", ex.getMessage());
        body.put("statusCode", ex.getStatusCode());
        body.put("binanceCode", ex.getBinanceCode());
        if (ex.getRetryAfterSeconds() != null) {
            body.put("retryAfterSeconds", ex.getRetryAfterSeconds());
        }
        return ResponseEntity.status(ex.getStatusCode()).body(body);
    }
}