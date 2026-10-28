package com.ipld11stars.backend.exception;

import org.springframework.http.HttpStatus;

/**
 * Thrown when a request breaks a domain rule (cricket laws, contest rules, wallet limits).
 * Carries the HTTP status the API should answer with.
 */
public class BusinessRuleException extends RuntimeException {

    private final HttpStatus status;

    public BusinessRuleException(String message) {
        this(HttpStatus.BAD_REQUEST, message);
    }

    public BusinessRuleException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static BusinessRuleException notFound(String message) {
        return new BusinessRuleException(HttpStatus.NOT_FOUND, message);
    }

    public static BusinessRuleException conflict(String message) {
        return new BusinessRuleException(HttpStatus.CONFLICT, message);
    }

    public static BusinessRuleException forbidden(String message) {
        return new BusinessRuleException(HttpStatus.FORBIDDEN, message);
    }
}
