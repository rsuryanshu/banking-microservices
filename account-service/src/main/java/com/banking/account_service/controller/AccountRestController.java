package com.banking.account_service.controller;

import com.banking.account_service.dto.AccountDTO;
import com.banking.account_service.entity.Account;
import com.banking.account_service.service.AccountService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasAnyRole('ROLE_ADMIN')")
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
        return accountService.findByAccountNumber(accountNumber);
    }

    @PutMapping("/account/{accountNumber}/balance")
    public AccountDTO updateBalance(@PathVariable String accountNumber, @RequestParam("amount") BigDecimal amount,
                                    @RequestParam("type") String type) {
        return accountService.updateBalance(accountNumber, amount, type);
    }
}
