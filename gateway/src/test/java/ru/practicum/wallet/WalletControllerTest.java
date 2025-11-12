package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import ru.practicum.enums.OperationStatus;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebFluxTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockBean
    private WalletCacheService walletCacheService;

    @Test
    void processOperation_shouldReturnAccepted() {
        // given
        String requestJson = """
                {
                    "walletId": "6fa4b687-5063-4da5-9930-faf423953ba3",
                    "operationType": "DEPOSIT", 
                    "amount": 100
                }
                """;

        OperationResponseDto responseDto = OperationResponseDto.builder()
                .status(OperationStatus.SUCCESS)
                .build();

        when(walletCacheService.processOperation(any(NewWalletOpsRequest.class)))
                .thenReturn(Mono.just(org.springframework.http.ResponseEntity.accepted().body(responseDto)));

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
    void getBalance_shouldReturnOk() {
        String walletIdStr = "f73c383c-51d2-4966-b0bc-c63faa7bf58d";
        UUID walletId = UUID.fromString(walletIdStr);

        WalletBalanceDto balanceDto = WalletBalanceDto.builder()
                .balance(1000L)
                .currency("RUB")
                .build();

        when(walletCacheService.getBalance(walletId))
                .thenReturn(Mono.just(org.springframework.http.ResponseEntity.ok(balanceDto)));

        webTestClient.get()
                .uri("/api/v1/wallets/{WALLET_UUID}", walletIdStr) // ← wallets вместо wallet!
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.balance").isEqualTo(1000)
                .jsonPath("$.currency").isEqualTo("RUB");
    }

    @Test
    void getBalance_shouldReturnNotFound() {
        UUID walletId = UUID.randomUUID();

        when(walletCacheService.getBalance(walletId))
                .thenReturn(Mono.just(org.springframework.http.ResponseEntity.notFound().build()));

        webTestClient.get()
                .uri("/api/v1/wallet/{walletId}", walletId)
                .exchange()
                .expectStatus().isNotFound();
    }
}