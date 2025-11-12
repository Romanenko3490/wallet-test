# Wallet Service Gateway

Этот проект представляет собой микросервисную архитектуру для обработки операций с электронными кошельками. Он состоит из шлюза (Gateway), сервиса кошельков (Wallet Service), базы данных (PostgreSQL), брокера сообщений (Kafka) и кэша (Redis).

## Архитектура

Система построена на принципах микросервисов и асинхронной обработки с использованием Kafka.

```mermaid



graph LR
    A[Client App] -->|HTTP| B(Gateway)
    B -->|Check/Get| C(Redis Cache)
    B -->|HTTP Get| D[Wallet Service API]
    D -->|Fetch/Cache| C
    B -->|Kafka Event| F[Kafka Broker]
    F -->|wallet_event| E[Wallet Service Consumer]
    D -->|DB Access| G[(PostgreSQL)]
    E -->|Update/Save| G

    style A fill:#87CEEB
    style B fill:#98FB98
    style C fill:#FFD700
    style D fill:#98FB98
    style E fill:#98FB98
    style F fill:#FFA07A
    style G fill:#DDA0DD




```

## Поток данных

1.  **Клиент** отправляет HTTP-запрос на `/api/v1/wallet` в **Gateway** с указанием ID кошелька, типа операции (DEPOSIT/WITHDRAW) и суммы.
2.  **Gateway** принимает запрос и проверяет **Redis**, есть ли в кэше информация о балансе для указанного кошелька.
3.  **Если баланс найден в кэше:**
    *   Gateway проверяет, достаточно ли средств для операции `WITHDRAW`.
    *   Если проверка проходит, Gateway отправляет событие `KafkaWalletEvent` в топик `wallet_event` через **Kafka**. Событие содержит ID кошелька, тип операции, сумму и уникальный `operationTrackId`.
    *   Gateway возвращает ответ клиенту (например, `202 ACCEPTED`).
4.  **Если баланс НЕ найден в кэше:**
    *   Gateway делает HTTP-запрос к **Wallet Service** по адресу `/api/v1/wallets/{walletId}`.
    *   **Wallet Service** получает запрос, извлекает информацию о кошельке из **PostgreSQL** и возвращает её в виде `WalletCacheDto`.
    *   Gateway получив данные, сохраняет их в **Redis** с TTL.
    *   Затем Gateway повторяет проверку (для `WITHDRAW`) и отправку события в Kafka, как описано в шаге 3.
5.  **Kafka** доставляет событие `KafkaWalletEvent` одному из потребителей в группе `wallet-service`.
6.  **Wallet Service (Consumer)** получает событие.
7.  **Wallet Service** проверяет, не обрабатывалась ли ранее операция с таким `operationTrackId` (для идемпотентности).
8.  **Wallet Service** снова проверяет баланс и делает транзакционное обновление баланса в **PostgreSQL** с использованием оптимистичной блокировки.
9.  **Wallet Service** сохраняет запись о транзакции в **PostgreSQL**.
10. (В идеальной реализации) **Wallet Service** отправляет команду на инвалидацию кэша в **Redis** для соответствующего кошелька, чтобы гарантировать консистентность при следующем запросе.

## Стек технологий

*   **Java 17+**
*   **Spring Boot**
*   **Spring WebFlux (Reactive)**
*   **Spring Data JPA**
*   **Spring Kafka**
*   **Spring Data Redis (Reactive)**
*   **PostgreSQL**
*   **Kafka**
*   **Redis**
*   **Docker / Docker Compose**
*   **Liquibase**
*   **MapStruct**
*   **Lombok**

## Запуск

Для запуска приложения выполните:

```bash
   docker-compose up --build

```

Приложения будут доступны на:

Gateway: http://localhost:8080
Kafka UI: http://localhost:8082
PostgreSQL (Wallet DB): jdbc:postgresql://localhost:15432/walletdb
Redis: redis://localhost:6379


Структура проекта
gateway/: Исходный код сервиса-шлюза.
wallet-service/: Исходный код основного сервиса кошелька.
docker-compose.yml: Определение сервисов для Docker Compose.
README.md: Этот файл.


Возможные улучшения
Интеграция инвалидации кэша в WalletService после обработки события Kafka.
Использование секретов для хранения конфиденциальных данных (пароли, токены).
Добавление аутентификации и авторизации.
Мониторинг и логирование (например, с использованием ELK или Prometheus/Grafana).
Более надежная обработка ошибок и сбоев (circuit breaker, dead letter topic для Kafka).