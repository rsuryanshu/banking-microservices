package com.banking.notification_service.service;

import com.banking.common_config.events.BalanceUpdatedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotificationService {

    @KafkaListener(topics = "transaction-completed", groupId = "notification-service")
    public void handleNotification(BalanceUpdatedEvent event) {
        if (event.isSuccess()) {
            log.info("Transaction {} completed", event.getTransactionId());
        } else {
            log.warn("Transaction {} failed: {}", event.getTransactionId(), event.getFailureReason());
        }
    }
}
