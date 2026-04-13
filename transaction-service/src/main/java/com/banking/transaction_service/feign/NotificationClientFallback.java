package com.banking.transaction_service.feign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Slf4j
public class NotificationClientFallback implements NotificationClient{

    @Override
    public String sendAlert(String accountNumber, String type, BigDecimal amount) {
        log.warn("Notification service unavailable. Alert not sent for account: {}", accountNumber);
        return "Notification service unavailable";
    }
}
