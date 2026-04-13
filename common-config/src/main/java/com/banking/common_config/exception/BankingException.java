package com.banking.common_config.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

import java.io.Serial;

@Getter
public class BankingException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final String frontMessage;
    private final String extendMessage;
    private final BankingExceptionLevel level;
    private final BankingExceptionType type;
    private final HttpStatus httpCode;
    private final boolean withTrace;

    public BankingException(BankingExceptionLevel level, BankingExceptionType type,
                            String extendMessage, String frontMessage, Throwable cause, boolean withTrace) {
        super(extendMessage, cause);
        this.level = level == null ? BankingExceptionLevel.ERROR : level;
        this.type = type == null ? BankingExceptionType.GENERAL : type;
        this.extendMessage = extendMessage == null ? this.type.getDescription() : extendMessage;
        this.frontMessage = frontMessage == null ? this.extendMessage : frontMessage;
        this.httpCode = this.type.getHttpStatus();
        this.withTrace = withTrace;
    }

    // simple constructor — most common use case
    public BankingException(BankingExceptionType type, String frontMessage) {
        this(BankingExceptionLevel.ERROR, type, frontMessage, frontMessage, null, false);
    }

    // with cause — logs stack trace
    public BankingException(BankingExceptionType type, String frontMessage, Throwable cause) {
        this(BankingExceptionLevel.ERROR, type, cause == null ? frontMessage : cause.toString(), frontMessage, cause, true);
    }

    // with level — for warnings/info
    public BankingException(BankingExceptionLevel level, BankingExceptionType type, String frontMessage) {
        this(level, type, frontMessage, frontMessage, null, false);
    }
}