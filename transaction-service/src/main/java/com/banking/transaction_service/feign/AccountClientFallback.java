package com.banking.transaction_service.feign;

import com.banking.common_config.exception.BankingException;
import com.banking.common_config.exception.BankingExceptionType;
import com.banking.transaction_service.dto.AccountDTO;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
public class AccountClientFallback implements AccountClient {

    @Override
    public AccountDTO getAccountById(Long id) {
        throw new BankingException(BankingExceptionType.SERVICE_UNAVAILABLE,
                "Account service is currently unavailable. Please try again later.");
    }

    @Override
    public AccountDTO updateBalance(String accountNumber, BigDecimal amount, String type) {
        throw new BankingException(BankingExceptionType.SERVICE_UNAVAILABLE,
                "Account service is currently unavailable. Please try again later.");
    }

    @Override
    public AccountDTO getAccountByNumber(String accountNumber) {
        throw new BankingException(BankingExceptionType.SERVICE_UNAVAILABLE,
                "Account service is currently unavailable. Please try again later.");
    }
}
