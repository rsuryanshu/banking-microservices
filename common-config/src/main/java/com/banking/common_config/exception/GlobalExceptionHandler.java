package com.banking.common_config.exception;

import com.banking.common_config.dto.ErrorResponse;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@ConditionalOnClass(name = "org.springframework.web.servlet.DispatcherServlet")
@Slf4j
public class GlobalExceptionHandler {

    private String now() {
        return OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    private String getPath(WebRequest request) {
        return request instanceof ServletWebRequest ? ((ServletWebRequest) request).getRequest().getRequestURI() : "";
    }

    @ExceptionHandler(BankingException.class)
    public ResponseEntity<ErrorResponse> handleBankingException(BankingException ex, WebRequest request) {
        String path = getPath(request);

        switch (ex.getLevel()) {
            case INFO -> log.info("BankingException: {} | path={}", ex.getFrontMessage(), path);
            case WARNING -> log.warn("BankingException: {} | path={}", ex.getFrontMessage(), path);
            default -> {
                if (ex.isWithTrace() && ex.getCause() != null) {
                    log.error("BankingException: {} | path={}", ex.getFrontMessage(), path, ex.getCause());
                } else {
                    log.error("BankingException: {} | path={}", ex.getFrontMessage(), path);
                }
            }
        }

        ErrorResponse body = new ErrorResponse(
                now(),
                ex.getHttpCode().value(),
                ex.getHttpCode().getReasonPhrase(),
                ex.getFrontMessage(),
                ex.getType().getCode(),
                path,
                null
        );

        return new ResponseEntity<>(body, new HttpHeaders(), ex.getHttpCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        String path = getPath(request);

        List<Map<String, String>> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> {
                    Map<String, String> item = new HashMap<>();
                    item.put("field", fe.getField());
                    item.put("message", fe.getDefaultMessage());
                    return item;
                }).collect(Collectors.toList());

        log.warn("Validation failed: path={} | errors={}", path, fieldErrors);

        ErrorResponse body = new ErrorResponse(
                now(),
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "Validation failed",
                BankingExceptionType.VALIDATION_FAILED.getCode(),
                path,
                fieldErrors
        );

        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(FeignException.class)
    public ResponseEntity<ErrorResponse> handleFeign(FeignException ex, WebRequest request) {
        String path = getPath(request);
        log.error("Feign error: {} | path={}", ex.getMessage(), path);

        HttpStatus status = ex instanceof FeignException.NotFound
                ? HttpStatus.NOT_FOUND : HttpStatus.SERVICE_UNAVAILABLE;

        ErrorResponse body = new ErrorResponse(
                now(),
                status.value(),
                status.getReasonPhrase(),
                "Service communication error",
                BankingExceptionType.SERVICE_UNAVAILABLE.getCode(),
                path,
                null
        );

        return new ResponseEntity<>(body, new HttpHeaders(), status);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception ex, WebRequest request) {
        String path = getPath(request);
        log.error("Unhandled exception: path={}", path, ex);

        ErrorResponse body = new ErrorResponse(
                now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase(),
                "An unexpected error occurred",
                BankingExceptionType.GENERAL.getCode(),
                path,
                null
        );

        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public  ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, WebRequest request) {
        String path = getPath(request);
        log.error("Bad credentials: path={}", path, ex);

        ErrorResponse body = new ErrorResponse(
                now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Invalid Username or Password",
                BankingExceptionType.UNAUTHORIZED.getCode(),
                path,
                null
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public  ResponseEntity<ErrorResponse> handleAuthorizationException(AuthorizationDeniedException ex, WebRequest request) {
        String path = getPath(request);
        log.error("Unauthorized Access: path={}", path, ex);

        ErrorResponse body = new ErrorResponse(
                now(),
                HttpStatus.UNAUTHORIZED.value(),
                HttpStatus.UNAUTHORIZED.getReasonPhrase(),
                "Forbidden Access",
                BankingExceptionType.UNAUTHORIZED.getCode(),
                path,
                null
        );
        return new ResponseEntity<>(body, new HttpHeaders(), HttpStatus.UNAUTHORIZED);
    }
}