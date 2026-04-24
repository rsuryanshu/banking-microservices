package com.banking.common_config.events;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BalanceUpdatedEvent {
    private String transactionId;
    private String accountNumber;
    private BigDecimal amount;
    private String type;
    private boolean success;
    private String failureReason;
}
