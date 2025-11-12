package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Mono;
import ru.practicum.NotFoundException;
import ru.practicum.kafka.WalletEventProducer;
import ru.practicum.redis.WalletCacheDto;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class GatewayIntegrationTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private WalletServiceClient walletServiceClient;

    @MockBean
    private WalletEventProducer eventProducer;

    @Container
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", redis::getFirstMappedPort);
    }

    @Test
    void processOperation_shouldWorkWithRealRedis() {
        UUID walletId = UUID.fromString("6fa4b687-5063-4da5-9930-faf423953ba3");
        String requestJson = """
                {
                    "walletId": "6fa4b687-5063-4da5-9930-faf423953ba3",
                    "operationType": "DEPOSIT", 
                    "amount": 100
                }
                """;

        WalletCacheDto walletFromService = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(1000L)
                .currency("RUB")
                .build();

        when(walletServiceClient.getWalletInfo(walletId))
                .thenReturn(Mono.just(walletFromService));

        doNothing().when(eventProducer).sendEvent(any());

        webTestClient.post()
                .uri("/api/v1/wallet")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestJson)
                .exchange()
                .expectStatus().isAccepted()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SUCCESS");
    }

    @Test
    void getBalance_shouldReturnNotFoundForNonExistentWallet() {
        UUID walletId = UUID.randomUUID();
        when(walletServiceClient.getWalletInfo(walletId))
                .thenReturn(Mono.error(new NotFoundException("Wallet not found")));

        webTestClient.get()
                .uri("/api/v1/wallets/{walletId}", walletId)
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void getBalance_shouldReturnOk_whenWalletExists() {
        UUID walletId = UUID.randomUUID();
        WalletCacheDto walletFromService = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(500L)
                .currency("RUB")
                .build();

        when(walletServiceClient.getWalletInfo(walletId))
                .thenReturn(Mono.just(walletFromService));

        webTestClient.get()
                .uri("/api/v1/wallets/{walletId}", walletId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(500)
                .jsonPath("$.currency").isEqualTo("RUB");
    }
}