package com.banking.account_service.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaTopicConfig {

    @Bean
    public NewTopic transactionInitiatedTopic() {
        return new NewTopic("transaction-initiated", 1, (short) 1);
    }

    @Bean
    public NewTopic transactionCompletedTopic() {
        return new NewTopic("transaction-completed", 1, (short) 1);
    }

    @Bean
    public NewTopic balanceUpdatedTopic() {
        return new NewTopic("balance-updated", 1, (short) 1);
    }
}
