package com.banking.transaction_service.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;

@FeignClient(name = "notification-service",  fallback = NotificationClientFallback.class)
public interface NotificationClient {

    @PostMapping("/api/notification/alert")
    String sendAlert(@RequestParam("accountNumber") String accountNumber,
                     @RequestParam("type") String type,
                     @RequestParam("amount") BigDecimal amount);
}
