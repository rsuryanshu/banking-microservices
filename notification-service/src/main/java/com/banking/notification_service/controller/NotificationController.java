package com.banking.notification_service.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
public class NotificationController {

    private static final Logger logger = LoggerFactory.getLogger(NotificationController.class);

    @PostMapping("/notification/alert")
    public ResponseEntity<String> sendAlert(
            @RequestParam("accountNumber") String accountNumber,
            @RequestParam("type") String type,
            @RequestParam("amount") BigDecimal amount) {

        logger.info("ALERT: {} of {} on account {}", type, amount, accountNumber);
        return ResponseEntity.ok("Notification sent for account: " + accountNumber);
    }
}
