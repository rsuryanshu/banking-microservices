package com.banking.account_service.dto;

import com.banking.account_service.entity.Account;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private String ownerName;
    private BigDecimal balance;

    public AccountDTO(Account account) {
        this.id = account.getId();
        this.accountNumber = account.getAccountNumber();
        this.ownerName = account.getOwnerName();
        this.balance = account.getBalance();
    }
}
