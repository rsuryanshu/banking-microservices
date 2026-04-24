# Kafka Integration Guide for Spring Boot Microservices

This guide covers complete Kafka integration in a Spring Boot microservices project including producer, consumer, event-driven communication, and the Saga pattern.

---

## Why Kafka in Microservices

In a microservices architecture, services need to communicate with each other. There are two ways:

**Synchronous (Feign/REST)** — service A calls service B directly and waits for a response. If service B is down, service A fails too. This creates tight coupling and cascading failures.

**Asynchronous (Kafka)** — service A publishes an event to Kafka and continues. Service B reads the event when it's ready. If service B is down, the event waits in Kafka until service B comes back up. This creates loose coupling and resilience.

Kafka is also the foundation of the **Saga pattern** — a way to handle distributed transactions across multiple services without a single global transaction.

---

## Prerequisites

### Start Kafka on Windows (Kafka 4.x — no Zookeeper needed)

Kafka 4.x uses KRaft mode which removes the Zookeeper dependency. Before first use, storage must be formatted with a unique cluster ID.

**Step 1 — Generate a UUID (one time only)**
```bash
.\bin\windows\kafka-storage.bat random-uuid
```
Copy the UUID printed in the output.

**Step 2 — Format storage (one time only)**
```bash
.\bin\windows\kafka-storage.bat format --standalone -t YOUR-UUID -c .\config\server.properties
```
This initializes the metadata storage directory and creates a `meta.properties` file. Without this, Kafka throws `No readable meta.properties files found`.

**Step 3 — Start Kafka (every time)**
```bash
.\bin\windows\kafka-server-start.bat .\config\server.properties
```
Keep this terminal open — closing it stops Kafka.

**Verify Kafka is running**
```bash
.\bin\windows\kafka-topics.bat --list --bootstrap-server localhost:9092
```
If it returns without error, Kafka is running on port 9092.

---

## Step 1 — Add Dependency

Add `spring-kafka` in every service that produces or consumes messages. Do not add it in services that don't need Kafka (like eureka-server, api-gateway, notification-service) — unnecessary dependencies bloat the classpath.

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

`spring-kafka` bundles everything needed — `KafkaTemplate` for producing, `@KafkaListener` for consuming, `KafkaAdmin` for topic management, and JSON serializers/deserializers.

---

## Step 2 — application.yml Configuration

Add Kafka config in `application.yml` of each service that uses Kafka.

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9092
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      properties:
        spring.json.add.type.headers: true
    consumer:
      group-id: account-service
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "com.banking.common_config.events"
        spring.json.use.type.headers: true
```

**Why each property:**

`bootstrap-servers` — the address of the Kafka broker. Services connect here to publish and consume messages.

`key-serializer / key-deserializer` — Kafka messages have a key and a value. The key is usually a String (like account number or transaction ID) used for partitioning.

`value-serializer / value-deserializer` — the message value is your event object. `JsonSerializer` converts your Java object to JSON when publishing. `JsonDeserializer` converts JSON back to Java object when consuming.

`spring.json.add.type.headers: true` — when publishing, Spring Kafka adds a `__TypeId__` header to every message containing the fully qualified class name of the event. This is critical when multiple event types are published to the same topic — without it, the consumer doesn't know which class to deserialize into.

`spring.json.use.type.headers: true` — tells the consumer to use the `__TypeId__` header to determine which class to deserialize into. Works together with `add.type.headers` on the producer side.

`spring.json.trusted.packages` — for security, Spring Kafka only deserializes classes from trusted packages. You must explicitly list the package containing your event classes. If this is wrong (even a typo), deserialization will fail silently.

`group-id` — identifies the consumer group. Kafka tracks which messages each consumer group has already read (the offset). Two services with different group IDs both receive the same message. Two instances of the same service with the same group ID share the message load (one instance gets each message).

`auto-offset-reset: earliest` — when a consumer starts for the first time (no committed offset yet), it reads from the beginning of the topic. Without this, a new consumer might miss messages published before it started.

---

## Step 3 — Create Event POJOs in Common Module

Event classes are shared between producer and consumer services. Keep them in a shared `common` module so both services use the exact same class.

```java
// TransactionInitiatedEvent.java
package com.banking.common_config.events;

import lombok.*;
import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class TransactionInitiatedEvent {
    private String transactionId;    // unique ID to correlate events in the Saga
    private String accountNumber;
    private BigDecimal amount;
    private String type;             // CREDIT or DEBIT
}
```

```java
// BalanceUpdatedEvent.java
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
    private String failureReason;    // populated if success is false
}
```

**Why POJOs and not interfaces or enums** — Kafka serializes messages as JSON. A plain Java object (POJO) with getters/setters serializes and deserializes cleanly. The `@NoArgsConstructor` is required by Jackson for deserialization — without it, Jackson can't create the object.

**Why `transactionId`** — in a Saga, multiple events are published for the same business transaction. The `transactionId` is the correlation ID that links all events together. When `BalanceUpdatedEvent` arrives, the service uses `transactionId` to find the original transaction and update its status.

---

## Step 4 — Create Kafka Topics

Topics are like channels — producers publish to a topic, consumers subscribe to a topic. Create them in one service only — Kafka won't create duplicates if a topic already exists.

```java
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
    public NewTopic balanceUpdatedTopic() {
        return new NewTopic("balance-updated", 1, (short) 1);
    }
}
```

**Parameters explained:**
- `"transaction-initiated"` — topic name
- `1` — number of partitions. More partitions = more parallelism. For learning, 1 is fine.
- `(short) 1` — replication factor. How many brokers keep a copy of this topic. For single broker setup use 1.

---

## Step 5 — KafkaProducerConfig

Spring Boot auto-configuration should create `KafkaTemplate` from `application.yml` properties. However in practice with Spring Boot 4.x and custom serializers, auto-configuration sometimes fails to create the bean. Explicit config guarantees the bean is always created.

```java
package com.banking.transaction_service.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

**Why `<String, Object>`** — the key is always a String. The value is `Object` because you might publish different event types from the same service. Using `Object` keeps it flexible.

**Why `ProducerFactory`** — `KafkaTemplate` needs a `ProducerFactory` to create producer instances. `DefaultKafkaProducerFactory` creates producers lazily (on first use) and reuses them.

---

## Step 6 — KafkaConsumerConfig

`@KafkaListener` requires a `kafkaListenerContainerFactory` bean to work. Without explicit config, this bean is not created and the application fails to start with `No bean named kafkaListenerContainerFactory`.

```java
package com.banking.account_service.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableKafka
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        JsonDeserializer<Object> deserializer = new JsonDeserializer<>(Object.class);
        deserializer.addTrustedPackages("com.banking.common_config.events");
        deserializer.setUseTypeHeaders(true);

        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);

        return new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }
}
```

**Why `@EnableKafka`** — activates Kafka listener infrastructure. Without it, `@KafkaListener` annotations are ignored and no consumers are registered.

**Why `ConcurrentKafkaListenerContainerFactory`** — this is the factory that creates listener containers for each `@KafkaListener` method. `Concurrent` means it can run multiple listener threads in parallel.

**Why `setUseTypeHeaders(true)` on deserializer** — tells the deserializer to read the `__TypeId__` header and deserialize into the correct class. This is what makes multiple event types work on the same consumer.

---

## Step 7 — Publish Events

Inject `KafkaTemplate` and call `send`:

```java
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void initiateTransaction(String accountNumber, BigDecimal amount, String type) {
        String transactionId = UUID.randomUUID().toString();

        // save with PENDING status first
        Transaction transaction = new Transaction();
        transaction.setTransactionId(transactionId);
        transaction.setStatus("PENDING");
        transactionRepository.save(transaction);

        // publish event
        TransactionInitiatedEvent event = new TransactionInitiatedEvent(
                transactionId, accountNumber, amount, type);
        kafkaTemplate.send("transaction-initiated", event);
    }
}
```

**Why save as PENDING first** — in the Saga pattern, the transaction record must exist before the event is published. When the consumer responds with `BalanceUpdatedEvent`, the service looks up the transaction by `transactionId` and updates its status. If the record doesn't exist yet, the update fails.

**Why `kafkaTemplate.send()` is fire and forget** — `send()` is asynchronous. It publishes the event and returns immediately without waiting for the consumer to process it. This is what makes the system non-blocking.

---

## Step 8 — Consume Events

```java
@KafkaListener(topics = "transaction-initiated", groupId = "account-service")
public void handleTransactionInitiated(TransactionInitiatedEvent event) {
    BalanceUpdatedEvent response = new BalanceUpdatedEvent();
    response.setTransactionId(event.getTransactionId());

    try {
        // process event
        Account account = accountRepository.findByAccountNumber(event.getAccountNumber());
        // update balance...
        response.setSuccess(true);
    } catch (BankingException e) {
        response.setSuccess(false);
        response.setFailureReason(e.getMessage());
    }

    // publish response event
    kafkaTemplate.send("balance-updated", response);
}
```

**Why catch exceptions inside the listener** — if an uncaught exception escapes the listener, Kafka will retry the message repeatedly. By catching exceptions and publishing a failure response instead, you handle errors gracefully and maintain the Saga flow.

**Why publish a response event** — the consumer (account-service) publishes a `BalanceUpdatedEvent` back to Kafka. The original publisher (transaction-service) listens for this event and updates the transaction status. This is the choreography Saga pattern — no central coordinator, services react to events.

---

## Saga Pattern Flow

```
Client
  |
  | POST /transaction/initiate
  |
transaction-service
  | 1. Save transaction as PENDING
  | 2. Publish TransactionInitiatedEvent
  |
  |-----> Kafka (transaction-initiated topic)
                    |
                    | 3. account-service consumes event
                    |
              account-service
                    | 4. Validate account and balance
                    | 5. Update balance if valid
                    | 6. Publish BalanceUpdatedEvent (success=true/false)
                    |
                    |-----> Kafka (balance-updated topic)
                                      |
                                      | 7. transaction-service consumes event
                                      |
                              transaction-service
                                      | 8. Update transaction status
                                      |    COMPLETED if success=true
                                      |    FAILED if success=false
```

**Why this is better than direct Feign call:**

With Feign — if account-service is down, transaction-service fails immediately. The transaction is never saved.

With Kafka Saga — if account-service is down, the event waits in Kafka. When account-service comes back up, it processes the event and the transaction completes. The system is eventually consistent.

---

## Common Mistakes

**Wrong package in trusted.packages** — must exactly match the package of your event classes including correct spelling. Even one wrong character causes silent deserialization failures.

**Missing group-id in application.yml** — `KafkaConsumerConfig` reads `${spring.kafka.consumer.group-id}`. If missing, startup fails.

**Not adding type headers on producer** — without `spring.json.add.type.headers: true`, the consumer can't determine which class to deserialize into when multiple event types exist.

**Starting services before Kafka** — always start Kafka first. Services try to connect to Kafka on startup and fail if it's not running.

**Downloading source package instead of binary** — Kafka source package (`-src`) contains Java source code and has no `.bat` files. Always download the binary release from kafka.apache.org.

**Throwing exceptions inside @KafkaListener** — uncaught exceptions cause infinite retries. Always catch exceptions inside listeners and handle them gracefully.
