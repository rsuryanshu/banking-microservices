package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.TransactionDTO;
import com.banking.transaction_service.service.TransactionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
public class TransactionRestController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transaction/process")
    public TransactionDTO processTransaction(@RequestParam("accountNumber") String accountNumber,
                                             @RequestParam("amount") BigDecimal amount, @RequestParam("type") String type) {
        return transactionService.initiateTransaction(accountNumber, amount, type);
    }

    @GetMapping("/transaction/{accountNumber}")
    public List<TransactionDTO> getTransactions(@PathVariable String accountNumber) {
        return transactionService.getTransactionsByAccount(accountNumber);
    }
}
