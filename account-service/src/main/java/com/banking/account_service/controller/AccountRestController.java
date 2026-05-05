package com.banking.account_service.controller;

import com.banking.account_service.dto.AccountDTO;
import com.banking.account_service.entity.Account;
import com.banking.account_service.service.AccountService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ROLE_ADMIN')")
@Slf4j
public class AccountRestController {

    @Autowired
    private AccountService accountService;


    @GetMapping("/account/{id}")
    public AccountDTO getAccountById(@PathVariable Long id) {
        return accountService.findById(id);
    }

    @PostMapping("/account")
    public AccountDTO saveAccount(@Valid @RequestBody Account account) {
        return accountService.save(account);
    }

    @GetMapping("/account/number/{accountNumber}")
    public AccountDTO getAccountByNumber(@PathVariable String accountNumber) {
        log.info("Init api/account/number/{}", accountNumber);
        AccountDTO number = accountService.findByAccountNumber(accountNumber);
        log.info("Ended /api/account/number/{} successfully", accountNumber);
        return number;
    }

    @PutMapping("/account/{accountNumber}/balance")
    public AccountDTO updateBalance(@PathVariable String accountNumber, @RequestParam("amount") BigDecimal amount,
                                    @RequestParam("type") String type) {
        log.info("Init api/account/{}/balance/{}", accountNumber, amount);
        AccountDTO accountDTO = accountService.updateBalance(accountNumber, amount, type);
        log.info("Ended /api/account/{}/balance successfully", accountNumber);
        return accountDTO;
    }
}
