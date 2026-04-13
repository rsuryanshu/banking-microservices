package com.banking.common_config.exception;

import lombok.Getter;

@Getter
public enum BankingExceptionLevel {
    ERROR("error"),
    WARNING("warning"),
    INFO("info");

    private final String description;

    BankingExceptionLevel(String description) {
        this.description = description;
    }
}