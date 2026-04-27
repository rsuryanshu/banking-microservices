package com.banking.transaction_service.service;

import com.banking.common_config.events.BalanceUpdatedEvent;
import com.banking.common_config.events.TransactionInitiatedEvent;
import com.banking.common_config.exception.BankingException;
import com.banking.common_config.exception.BankingExceptionLevel;
import com.banking.common_config.exception.BankingExceptionType;
import com.banking.transaction_service.dto.TransactionDTO;
import com.banking.transaction_service.entity.Transaction;
import com.banking.transaction_service.feign.AccountClient;
import com.banking.transaction_service.feign.NotificationClient;
import com.banking.transaction_service.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final AccountClient accountClient;
    private final NotificationClient notificationClient;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransactionDTO processTransaction(String accountNumber, BigDecimal amount, String type) {
        accountClient.updateBalance(accountNumber, amount, type);

        String transactionId = UUID.randomUUID().toString();

        Transaction transaction = new Transaction();
        transaction.setAccountNumber(accountNumber);
        transaction.setTransactionId(transactionId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());

        TransactionDTO transactionDTO = new TransactionDTO(transactionRepository.save(transaction));
        notificationClient.sendAlert(accountNumber, type, amount);
        return transactionDTO;
    }

    public TransactionDTO initiateTransaction(String accountNumber, BigDecimal amount, String type) {
        String transactionId = UUID.randomUUID().toString();

        Transaction transaction = new Transaction();
        transaction.setAccountNumber(accountNumber);
        transaction.setTransactionId(transactionId);
        transaction.setAmount(amount);
        transaction.setType(type);
        transaction.setStatus("PENDING");
        transaction.setCreatedAt(LocalDateTime.now());

        TransactionDTO transactionDTO = new TransactionDTO(transactionRepository.save(transaction));

        try {
            TransactionInitiatedEvent event = new TransactionInitiatedEvent(transactionId, accountNumber,
                    amount, type);
            kafkaTemplate.send("transaction-initiated", event);
        } catch (Exception e) {
            transaction.setStatus("FAILED");
            transaction.setFailureReason("Messaging service unavailable");
            transactionRepository.save(transaction);
            transactionDTO.setStatus(transaction.getStatus());
            transactionDTO.setFailureReason(transaction.getFailureReason());
            log.error("Failed to publish transaction event: {}", e.getMessage());
        }
        return transactionDTO;
    }

    @KafkaListener(topics = "balance-updated", groupId = "transaction-service")
    public void handleBalanceUpdated(BalanceUpdatedEvent event) {
        Transaction transaction = transactionRepository
                .findByTransactionId(event.getTransactionId());

        if (transaction == null) {
            throw new BankingException(BankingExceptionLevel.ERROR, BankingExceptionType.ELEMENT_NOT_FOUND,
                    "Transaction with id " + event.getTransactionId() + " not found");
        }

        if (event.isSuccess()) {
            transaction.setStatus("COMPLETED");
            log.info("Transaction {} completed", event.getTransactionId());
            kafkaTemplate.send("transaction-completed", event);
        } else {
            transaction.setStatus("FAILED");
            transaction.setFailureReason(event.getFailureReason());
            log.warn("Transaction {} failed: {}", event.getTransactionId(), event.getFailureReason());
        }

        transactionRepository.save(transaction);
    }

    public List<TransactionDTO> getTransactionsByAccount(String accountNumber) {
        return transactionRepository.findByAccountNumber(accountNumber)
                .stream()
                .map(TransactionDTO::new)
                .collect(Collectors.toList());
    }
}
