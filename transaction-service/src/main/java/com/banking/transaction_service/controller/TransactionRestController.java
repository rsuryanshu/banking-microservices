package com.banking.transaction_service.controller;

import com.banking.transaction_service.dto.TransactionDTO;
import com.banking.transaction_service.service.TransactionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api")
@Slf4j
public class TransactionRestController {

    @Autowired
    private TransactionService transactionService;

    @PostMapping("/transaction/process")
    public TransactionDTO processTransaction(@RequestParam("accountNumber") String accountNumber,
                                             @RequestParam("amount") BigDecimal amount, @RequestParam("type") String type) {
        log.info("Init api/transaction/process");
        TransactionDTO transactionDTO = transactionService.initiateTransaction(accountNumber, amount, type);
        log.info("Ended api/transaction/process successfully");
        return transactionDTO;
    }

    @GetMapping("/transaction/{accountNumber}")
    public List<TransactionDTO> getTransactions(@PathVariable String accountNumber) {
        return transactionService.getTransactionsByAccount(accountNumber);
    }
}
