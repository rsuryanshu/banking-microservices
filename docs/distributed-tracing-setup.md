# Distributed Tracing with Micrometer, Brave and Zipkin

## Overview

In a microservices architecture, a single request flows through multiple services. Without distributed tracing, debugging failures or performance issues requires manually searching logs across multiple services — time consuming and error prone.

Distributed tracing assigns a unique `traceId` to every request that flows through all services automatically. Each service operation creates a `spanId` — a child of the same trace. All spans are collected in Zipkin where you can visualize the complete request journey.

---

## Core Concepts

**TraceId** — unique ID for the entire request journey. Same across all services involved in one request.

**SpanId** — unique ID for each individual operation within a service. Different per service but linked to the same traceId.

**Brave** — the tracing instrumentation library that creates and propagates spans.

**Micrometer Tracing** — the abstraction layer between your code and Brave. You code against Micrometer API, Brave handles the actual tracing.

**Zipkin** — the visualization tool that collects all spans and displays them as a waterfall diagram.

---

## Prerequisites — Install and Run Zipkin

Download and run Zipkin standalone JAR:

```bash
curl -sSL https://zipkin.io/quickstart.sh | bash -s
java -jar zipkin.jar
```

Zipkin UI available at `http://localhost:9411`. Keep this running before starting your services.

---

## Part 1 — HTTP and Feign Tracing

### Step 1 — Add dependency

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-zipkin</artifactId>
</dependency>
```

This single starter includes everything needed — `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`, and the HTTP sender to push traces to Zipkin.

**Why this starter** — Spring Boot 4.x bundles Brave and Zipkin reporter together in `spring-boot-starter-zipkin`. Using separate dependencies caused auto-configuration issues in Spring Boot 4.x.

### Step 2 — Add configuration

```yaml
management:
  tracing:
    sampling:
      probability: 1.0
    propagation:
      type: b3
    export:
      zipkin:
        endpoint: http://localhost:9411/api/v2/spans
```

**`probability: 1.0`** — trace 100% of requests. Use `0.1` (10%) in production to reduce overhead.

**`propagation.type: b3`** — use B3 format for traceId propagation. There are two propagation formats:
- **B3** — Zipkin's original format. Sends traceId in headers like `X-B3-TraceId`, `X-B3-SpanId`, `X-B3-Sampled`. This is what Brave uses natively and what Zipkin expects.
- **W3C TraceContext** — newer standard format. Uses `traceparent` header. Used by OpenTelemetry.

Since we are using Brave and Zipkin, B3 is the correct format. Without explicitly setting it, Spring Boot 4.x might default to W3C format which causes traceId propagation to fail between services.

**`endpoint`** — where traces are sent. Zipkin listens here for incoming span data.

### Step 3 — Add traceId to logs

```yaml
logging:
  pattern:
    correlation: "[${spring.application.name:},%X{traceId:-},%X{spanId:-}] "
  include-application-name: false
```

`%X{traceId:-}` and `%X{spanId:-}` read from MDC (Mapped Diagnostic Context) which Micrometer Tracing populates automatically.

Now every log line shows:
```
2026-04-30 16:03:45  INFO [transaction-service,abc123,111] Processing transaction
2026-04-30 16:03:45  INFO [account-service,abc123,222]    Updating balance
```

Same `abc123` traceId across both services.

### Step 4 — Feign propagation

Add Feign Micrometer dependency:

```xml
<dependency>
    <groupId>io.github.openfeign</groupId>
    <artifactId>feign-micrometer</artifactId>
</dependency>
```

This automatically injects B3 tracing headers (`X-B3-TraceId`, `X-B3-SpanId`) into every outgoing Feign request. The receiving service reads these headers and continues the same trace — no extra code needed.

### How HTTP tracing works

```
Client → Gateway (new traceId created)
    → transaction-service (traceId propagated via B3 headers)
        → Feign call to account-service (traceId propagated automatically)
            → account-service (continues same trace)
        → Feign call to notification-service (traceId propagated automatically)
            → notification-service (continues same trace)
```

All spans collected in Zipkin under one traceId showing the complete waterfall.

---

## Part 2 — Kafka Tracing

HTTP tracing works automatically because B3 headers are passed in HTTP headers. Kafka is async — there are no HTTP headers. Without extra setup, each service creates its own new trace when consuming a Kafka message, breaking the correlation.

### Step 1 — Add Brave Kafka instrumentation dependency

```xml
<dependency>
    <groupId>io.zipkin.brave</groupId>
    <artifactId>brave-instrumentation-kafka-clients</artifactId>
</dependency>
```

This library intercepts Kafka producer and consumer at the client level — automatically injects traceId into Kafka message headers when producing and extracts it when consuming.

### Step 2 — Enable Kafka observation in application.yml

```yaml
spring:
  kafka:
    listener:
      observation-enabled: true
    template:
      observation-enabled: true
```

**`observation-enabled: true`** — tells Spring Kafka to create Micrometer observations (spans) for every message produced and consumed. Without this, Kafka activity is invisible to Zipkin even with Brave instrumentation.

### Step 3 — Wrap ProducerFactory with Brave tracing

```java
@Autowired
private Tracing tracing;

@Bean
public ProducerFactory<String, Object> producerFactory() {
    Map<String, Object> config = new HashMap<>();
    config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JacksonJsonSerializer.class);

    DefaultKafkaProducerFactory<String, Object> factory =
            new DefaultKafkaProducerFactory<>(config);

    factory.addPostProcessor(producer ->
            KafkaTracing.create(tracing).producer(producer));

    return factory;
}
```

`KafkaTracing.create(tracing).producer(producer)` — wraps the Kafka producer with Brave instrumentation. Every `send()` call now automatically injects traceId into Kafka message headers.

### Step 4 — Wrap ConsumerFactory with Brave tracing

```java
@Autowired
private Tracing tracing;

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

    DefaultKafkaConsumerFactory<String, Object> factory =
            new DefaultKafkaConsumerFactory<>(config, new StringDeserializer(), deserializer);

    factory.addPostProcessor(consumer ->
            KafkaTracing.create(tracing).consumer(consumer));

    return factory;
}
```

`KafkaTracing.create(tracing).consumer(consumer)` — wraps the Kafka consumer with Brave instrumentation. Every message consumed automatically extracts traceId from Kafka message headers and continues the existing trace.

### Step 5 — Enable observation in listener container factory

```java
@Bean
public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, Object> factory =
            new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    factory.getContainerProperties().setObservationEnabled(true);
    return factory;
}
```

`setObservationEnabled(true)` — creates a span for each `@KafkaListener` method invocation. Without this, the consumer factory wrapping alone is not enough — listener container also needs observation enabled to report spans to Zipkin.

### How Kafka tracing works

```
transaction-service → kafkaTemplate.send("transaction-initiated", event)
    Brave injects traceId into Kafka message headers
        ↓
    Kafka broker stores message with traceId header
        ↓
account-service → @KafkaListener consumes message
    Brave extracts traceId from headers
    Continues the same trace
        ↓
account-service → kafkaTemplate.send("balance-updated", response)
    Brave injects same traceId into response message headers
        ↓
transaction-service → @KafkaListener consumes response
    Brave extracts traceId
    Continues the same trace
```

All four operations appear in Zipkin under the same traceId — complete end-to-end visibility including async Kafka messaging.

---

## What Zipkin Shows

**HTTP only flow:**
```
Gateway (12ms)
  └── transaction-service (85ms)
        ├── circuit-breaker → account-service (20ms)
        └── circuit-breaker → notification-service (15ms)
```

**Kafka Saga flow:**
```
transaction-service → publish TransactionInitiatedEvent
    └── account-service → consume TransactionInitiatedEvent
            └── account-service → publish BalanceUpdatedEvent
                    └── transaction-service → consume BalanceUpdatedEvent
```

---

## Common Issues

**TraceId empty in logs** — `spring-boot-starter-zipkin` not on classpath or sampling probability not set.

**Different traceId per service** — `feign-micrometer` dependency missing or `propagation.type: b3` not set — Feign not propagating B3 headers correctly.

**Kafka spans not in Zipkin** — `observation-enabled: true` missing in yml or `setObservationEnabled(true)` missing in container factory.

**Zipkin not receiving traces** — wrong endpoint in yml or Zipkin not running on port 9411.
