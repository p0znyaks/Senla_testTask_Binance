package com.example.p0znyaks.exception;

/**
 * Thrown when the requested time range does not make sense, for example when {@code from} is after {@code to}.
 */
public class InvalidRangeException extends RuntimeException {
    public InvalidRangeException(String message) {
        super(message);
    }
}