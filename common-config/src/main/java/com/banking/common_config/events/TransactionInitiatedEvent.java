package com.banking.common_config.events;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInitiatedEvent {
    private String transactionId;    // unique ID for this saga
    private String accountNumber;
    private BigDecimal amount;
    private String type;             // CREDIT or DEBIT
}
