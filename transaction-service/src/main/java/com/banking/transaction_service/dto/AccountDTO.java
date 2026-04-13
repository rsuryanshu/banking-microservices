package com.banking.transaction_service.dto;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class AccountDTO {
    private Long id;
    private String accountNumber;
    private String ownerName;
    private Double balance;
}