package com.example.exception;

/**
 * Thrown when a requested entity (account, customer, payee, transaction,
 * etc.) does not exist. Mapped to HTTP 404 by GlobalExceptionHandler -
 * see that class for the full exception-to-status mapping used across the
 * REST API.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
