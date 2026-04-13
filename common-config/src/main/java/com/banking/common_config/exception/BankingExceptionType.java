package com.banking.common_config.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum BankingExceptionType {

    GENERAL("GENERAL_ERROR", "General error", HttpStatus.INTERNAL_SERVER_ERROR),
    BAD_REQUEST("BAD_REQUEST", "Bad request", HttpStatus.BAD_REQUEST),
    UNAUTHORIZED("UNAUTHORIZED", "Unauthorized", HttpStatus.FORBIDDEN),
    ACCOUNT_NOT_FOUND("ACCOUNT_NOT_FOUND", "Account not found", HttpStatus.NOT_FOUND),
    USER_NOT_FOUND("USER_NOT_FOUND", "User not found", HttpStatus.NOT_FOUND),
    ALREADY_EXIST("ALREADY EXIST", "User already exist", HttpStatus.CONFLICT),
    ELEMENT_NOT_FOUND("ELEMENT_NOT_FOUND", "Element not found", HttpStatus.NOT_FOUND),
    DUPLICATE_ACCOUNT("DUPLICATE_ACCOUNT", "Account already exists", HttpStatus.CONFLICT),
    INSUFFICIENT_FUNDS("INSUFFICIENT_FUNDS", "Insufficient funds", HttpStatus.BAD_REQUEST),
    INVALID_TRANSACTION("INVALID_TRANSACTION", "Invalid transaction", HttpStatus.BAD_REQUEST),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE", "Service unavailable", HttpStatus.SERVICE_UNAVAILABLE),
    VALIDATION_FAILED("VALIDATION_FAILED", "Validation failed", HttpStatus.BAD_REQUEST);

    private final String code;
    private final String description;
    private final HttpStatus httpStatus;

    BankingExceptionType(String code, String description, HttpStatus httpStatus) {
        this.code = code;
        this.description = description;
        this.httpStatus = httpStatus;
    }
}