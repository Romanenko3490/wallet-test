package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.kafka.KafkaWalletEvent;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Testcontainers
@SpringBootTest
@EmbeddedKafka(topics = "wallet_event", partitions = 1)
class WalletServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void fullFlow_shouldProcessEventAndUpdateBalance() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(1000L)
                .currency("RUB")
                .version(0L)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
        walletRepository.save(wallet);

        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.DEPOSIT)
                .amount(500L)
                .operationTrackId(UUID.randomUUID())
                .build();

        walletService.updateBalanceFromEvent(event);

        Wallet updatedWallet = walletRepository.findById(walletId).orElseThrow();
        assertEquals(1500L, updatedWallet.getBalance());
    }
}