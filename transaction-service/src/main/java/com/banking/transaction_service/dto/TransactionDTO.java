package com.banking.transaction_service.dto;

import com.banking.transaction_service.entity.Transaction;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionDTO {
    private Long id;
    private String transactionId;
    private String accountNumber;
    private BigDecimal amount;
    private String type;
    private String status;
    private String failureReason;
    private String createdAt;

    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.accountNumber = transaction.getAccountNumber();
        this.transactionId = transaction.getTransactionId();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.status = transaction.getStatus();
        this.failureReason = transaction.getFailureReason();
        this.createdAt = transaction.getCreatedAt().toString();
    }
}
