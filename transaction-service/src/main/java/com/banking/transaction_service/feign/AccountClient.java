package com.banking.transaction_service.feign;

import com.banking.transaction_service.dto.AccountDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@FeignClient(name = "account-service", fallback = AccountClientFallback.class)
public interface AccountClient {

    @GetMapping("/api/account/{id}")
    AccountDTO getAccountById(@PathVariable Long id);

    @PutMapping("/api/account/{accountNumber}/balance")
    AccountDTO updateBalance(@PathVariable String accountNumber, @RequestParam("amount") BigDecimal amount,
                             @RequestParam("type") String type);

    @GetMapping("/api/account/number/{accountNumber}")
    AccountDTO getAccountByNumber(@PathVariable String accountNumber);
}
