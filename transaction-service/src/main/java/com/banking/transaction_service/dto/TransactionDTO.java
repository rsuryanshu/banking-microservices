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
    private String accountNumber;
    private BigDecimal amount;
    private String type;
    private String createdAt;

    public TransactionDTO(Transaction transaction) {
        this.id = transaction.getId();
        this.accountNumber = transaction.getAccountNumber();
        this.amount = transaction.getAmount();
        this.type = transaction.getType();
        this.createdAt = transaction.getCreatedAt().toString();
    }
}
