package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import ru.practicum.NotFoundException;
import ru.practicum.enums.OperationStatus;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.kafka.KafkaWalletEvent;
import ru.practicum.kafka.WalletEventProducer;
import ru.practicum.redis.WalletCacheDto;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletCacheServiceTest {

    @Mock
    private ReactiveRedisTemplate<String, WalletCacheDto> reactiveRedisTemplate;

    @Mock
    private ReactiveValueOperations<String, WalletCacheDto> valueOperations;

    @Mock
    private WalletServiceClient walletServiceClient;

    @Mock
    private WalletEventProducer eventProducer;

    @InjectMocks
    private WalletCacheService walletCacheService;

    @Test
    void processOperation_shouldSendEvent_whenCacheHitAndBalanceSufficient() {
        UUID walletId = UUID.randomUUID();
        NewWalletOpsRequest request = NewWalletOpsRequest.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.WITHDRAW)
                .amount(100L)
                .build();

        WalletCacheDto cachedWallet = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(1000L)
                .currency("RUB")
                .build();

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just(cachedWallet));

        when(valueOperations.delete(anyString())).thenReturn(Mono.just(true));

        doNothing().when(eventProducer).sendEvent(any(KafkaWalletEvent.class));

        Mono<ResponseEntity<OperationResponseDto>> result = walletCacheService.processOperation(request);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.ACCEPTED &&
                                response.getBody().getStatus() == OperationStatus.SUCCESS
                )
                .verifyComplete();

        verify(eventProducer).sendEvent(any(KafkaWalletEvent.class));
        verify(valueOperations).delete(anyString()); // Проверяем что инвалидация вызвалась
    }

    @Test
    void processOperation_shouldReturnUnprocessableEntity_whenInsufficientBalance() {
        UUID walletId = UUID.randomUUID();
        NewWalletOpsRequest request = NewWalletOpsRequest.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.WITHDRAW)
                .amount(1000L)
                .build();

        WalletCacheDto cachedWallet = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(100L) // Меньше чем нужно
                .currency("RUB")
                .build();

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.just(cachedWallet));

        Mono<ResponseEntity<OperationResponseDto>> result = walletCacheService.processOperation(request);

        StepVerifier.create(result)
                .expectNextMatches(response ->
                        response.getStatusCode() == HttpStatus.UNPROCESSABLE_ENTITY
                )
                .verifyComplete();

        verify(eventProducer, never()).sendEvent(any());
    }

    @Test
    void processOperation_shouldFetchFromService_whenCacheMiss() {
        UUID walletId = UUID.randomUUID();
        NewWalletOpsRequest request = NewWalletOpsRequest.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.DEPOSIT)
                .amount(100L)
                .build();

        WalletCacheDto walletFromService = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(500L)
                .currency("RUB")
                .build();

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty()); // Cache miss
        when(walletServiceClient.getWalletInfo(walletId)).thenReturn(Mono.just(walletFromService));
        when(valueOperations.set(anyString(), any(), any())).thenReturn(Mono.just(true));

        when(valueOperations.delete(anyString())).thenReturn(Mono.just(true));

        doNothing().when(eventProducer).sendEvent(any(KafkaWalletEvent.class));

        Mono<ResponseEntity<OperationResponseDto>> result = walletCacheService.processOperation(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode().is2xxSuccessful())
                .verifyComplete();

        verify(walletServiceClient).getWalletInfo(walletId);
        verify(eventProducer).sendEvent(any(KafkaWalletEvent.class));
    }

    @Test
    void processOperation_shouldReturnNotFound_whenWalletNotExists() {
        UUID walletId = UUID.randomUUID();
        NewWalletOpsRequest request = NewWalletOpsRequest.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.DEPOSIT)
                .amount(100L)
                .build();

        when(reactiveRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(Mono.empty());
        when(walletServiceClient.getWalletInfo(walletId))
                .thenReturn(Mono.error(new NotFoundException("Wallet not found")));

        Mono<ResponseEntity<OperationResponseDto>> result = walletCacheService.processOperation(request);

        StepVerifier.create(result)
                .expectNextMatches(response -> response.getStatusCode() == HttpStatus.NOT_FOUND)
                .verifyComplete();
    }
}