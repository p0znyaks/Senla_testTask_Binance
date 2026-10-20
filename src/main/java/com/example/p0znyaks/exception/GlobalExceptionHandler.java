package com.example.p0znyaks.exception;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Translates every exception into the same JSON error shape: {@code {"error": "<message>"}}.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(InvalidRangeException.class)
    public ResponseEntity<Map<String, Object>> handleInvalidRange(InvalidRangeException ex) {
        return ResponseEntity.badRequest().body(body(ex.getMessage()));
    }

    /**
     * Binding errors raised while constructing a request object, for example a missing
     * {@code from}/{@code to} parameter, surface as a plain {@link BindException}.
     */
    @ExceptionHandler({MethodArgumentNotValidException.class, BindException.class})
    public ResponseEntity<Map<String, Object>> handleBinding(BindException ex) {
        return ResponseEntity.badRequest().body(body(describe(ex.getBindingResult())));
    }

    /**
     * Constraint violations on plain method parameters, for example a negative {@code page}.
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Map<String, Object>> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getPropertyPath() + " " + violation.getMessage())
                .orElse("Invalid request");
        return ResponseEntity.badRequest().body(body(message));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String expected = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("the expected type");
        return ResponseEntity.badRequest()
                .body(body("Parameter '%s' must be of type %s".formatted(ex.getName(), expected)));
    }

    @ExceptionHandler(PriceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handlePriceNotFound(PriceNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body(ex.getMessage()));
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<Map<String, Object>> handleNoHandler(Exception ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body("Resource not found"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).body(body(ex.getMessage()));
    }

    /**
     * Any other unexpected failure is reported with the same error body shape,
     * so clients never have to parse a framework-specific error page.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("Unexpected error while handling request", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body("Internal server error"));
    }

    /**
     * Builds a readable message from the first binding error, preferring a field error
     * over a global one because it names the offending parameter.
     */
    private static String describe(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + " " + error.getDefaultMessage())
                .or(() -> bindingResult.getGlobalErrors().stream().findFirst().map(GlobalExceptionHandler::describe))
                .orElse("Invalid request");
    }

    private static String describe(ObjectError error) {
        return error.getObjectName() + " " + error.getDefaultMessage();
    }

    private static Map<String, Object> body(String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", message);
        return body;
    }
}