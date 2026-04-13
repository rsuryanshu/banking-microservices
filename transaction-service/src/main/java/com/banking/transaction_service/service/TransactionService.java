package com.banking.transaction_service.service;

import com.banking.transaction_service.dto.TransactionDTO;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.feign.AccountClient;
import com.banking.transaction_service.feign.NotificationClient;
import com.banking.transaction_service.repository.TransactionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class TransactionService {

    @Autowired
    private TransactionRepository transactionRepository;

    @Autowired
    private AccountClient accountClient;

    @Autowired
    private NotificationClient notificationClient;

    public TransactionDTO processTransaction(String accountNumber, BigDecimal amount, String type) {
        accountClient.updateBalance(accountNumber, amount, type);

        Transaction transaction = new Transaction();
        transaction.setAccountNumber(accountNumber);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setCreatedAt(LocalDateTime.now());

        TransactionDTO transactionDTO = new TransactionDTO(transactionRepository.save(transaction));
        notificationClient.sendAlert(accountNumber,type,amount);
        return transactionDTO;
    }

    public List<TransactionDTO> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber)
                .stream()
                .map(TransactionDTO::new)
                .collect(Collectors.toList());
    }
}
