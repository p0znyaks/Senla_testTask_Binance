package com.example.p0znyaks.exception;

/**
 * Thrown when the service has no price records for the requested symbol or range.
 */
public class PriceNotFoundException extends RuntimeException {
    public PriceNotFoundException(String message) {
        super(message);
    }
}