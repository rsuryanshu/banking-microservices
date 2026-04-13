package com.banking.account_service.service;

import com.banking.account_service.dto.AccountDTO;
import com.banking.account_service.entity.Account;
import com.banking.account_service.repository.AccountRepository;
import com.banking.common_config.aop.LogExecutionTime;
import com.banking.common_config.exception.BankingException;
import com.banking.common_config.exception.BankingExceptionLevel;
import com.banking.common_config.exception.BankingExceptionType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

@Service
public class AccountService {

    @Autowired
    private AccountRepository accountRepository;

    public AccountDTO findById(Long id) {
        Account account = accountRepository.findById(id).orElseThrow(() -> new BankingException(BankingExceptionType.ACCOUNT_NOT_FOUND, "Bank Account not found"));
        return new AccountDTO(account);
    }

    public AccountDTO save(Account account) {
        Account accountNumber = accountRepository.findByAccountNumber(account.getAccountNumber());
        if (accountNumber != null) {
            throw new BankingException(BankingExceptionType.DUPLICATE_ACCOUNT, "Account already exists");
        }
        Account saved = accountRepository.save(account);
        return new AccountDTO(saved);
    }

    @LogExecutionTime
    public AccountDTO findByAccountNumber(String accountNumber) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new BankingException(BankingExceptionType.ACCOUNT_NOT_FOUND, "Bank Account not found");
        }
        return new AccountDTO(account);
    }

    public AccountDTO updateBalance(String accountNumber, BigDecimal amount, String type) {
        Account account = accountRepository.findByAccountNumber(accountNumber);
        if (account == null) {
            throw new BankingException(BankingExceptionType.ACCOUNT_NOT_FOUND, "Bank Account not found");
        }

        if ("DEBIT".equals(type) && account.getBalance().compareTo(amount) < 0) {
            throw new BankingException(BankingExceptionType.INSUFFICIENT_FUNDS,"Insufficient funds. Available Balance is " + account.getBalance());
        }

        BigDecimal delta = "CREDIT".equals(type) ? amount : amount.negate();
        account.setBalance(account.getBalance().add(delta));
        accountRepository.save(account);
        return new AccountDTO(account);
    }
}
