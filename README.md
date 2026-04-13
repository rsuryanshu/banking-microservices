# Banking Microservices

A microservices-based banking application built with Java 21, Spring Boot 4.0.5 and Spring Cloud 2025.1.1.

---

## Services

| Service | Port | Description |
|---|---|---|
| eureka-server | 8761 | Service discovery |
| api-gateway | 8080 | Single entry point, JWT validation, routing |
| auth-service | 8084 | User registration, login, JWT generation |
| account-service | 8081 | Bank account management |
| transaction-service | 8082 | Debit/credit transactions |
| notification-service | 8083 | Transaction alerts |
| common-config | - | Shared library |

---

## Tech Stack

- Java 21
- Spring Boot 4.0.5
- Spring Cloud 2025.1.1
- Spring Security + JWT (JJWT 0.12.6)
- Spring Cloud Gateway (WebFlux)
- Netflix Eureka
- OpenFeign + Resilience4j
- MySQL
- Lombok

---

## Architecture

```
Client → API Gateway → Microservices → MySQL
              ↕
        Eureka Server
```

- API Gateway validates JWT on every incoming request
- Each service independently validates JWT for defense in depth
- Inter-service communication via Feign with automatic JWT propagation
- Circuit breaker on all Feign calls via Resilience4j
- Shared common-config module for all cross-cutting concerns

---

## Prerequisites

- Java 21
- MySQL
- Maven

---

## Database Setup

```sql
CREATE DATABASE auth_db;
CREATE DATABASE accounts_db;
CREATE DATABASE transactions_db;
```

---

## Start Order

```
1. eureka-server
2. api-gateway
3. auth-service
4. account-service
5. transaction-service
6. notification-service
```

---

## API Endpoints

### Auth (no token required)

```
POST /users/auth/register
POST /users/auth/login
```

### Accounts (token required)

```
POST  /accounts/api/account
GET   /accounts/api/account/number/{accountNumber}
PUT   /accounts/api/account/{accountNumber}/balance
```

### Transactions (token required)

```
POST  /transactions/api/transaction/process?accountNumber=&amount=&type=
GET   /transactions/api/transaction/{accountNumber}
```

---

## Common Config Module

Shared library used by all services providing:

- Structured exception handling via `BankingException` and `BankingExceptionType`
- Consistent JSON error responses via `GlobalExceptionHandler`
- JWT token generation and validation via `JwtUtil`
- Security filter via `JwtAuthenticationFilter`
- Auto-configured security via `CommonSecurityConfig`
- Automatic JWT propagation in Feign calls via `FeignClientInterceptor`
- Execution time logging via `LoggingAspect`
