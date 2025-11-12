package ru.practicum.wallet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.LowBalanceException;
import ru.practicum.NotFoundException;
import ru.practicum.enums.OperationStatus;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.kafka.KafkaWalletEvent;
import ru.practicum.kafka.WalletEventProducer;
import ru.practicum.redis.WalletCacheDto;

import java.time.Duration;
import java.util.UUID;

import static ru.practicum.wallet.ResponseFactory.createDeniedResponse;
import static ru.practicum.wallet.ResponseFactory.createResponse;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletCacheService {
    private final ReactiveRedisTemplate<String, WalletCacheDto> reactiveRedisTemplate;
    private final WalletServiceClient walletServiceClient;
    private final WalletEventProducer eventProducer;

    private final WalletDtoMapper walletDtoMapper;

    private static final String WALLET_KEY_PREFIX = "wallet:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);


    public Mono<ResponseEntity<OperationResponseDto>> processOperation(NewWalletOpsRequest request) {
        log.info("Processing wallet operation: {}", request);

        return getWallet(request.getWalletId())
                .flatMap(cachedWallet -> {
                    log.info("Processed with cached wallet: {}", cachedWallet);
                    return processWithCachedWallet(request, cachedWallet);
                })
                .switchIfEmpty(Mono.fromCallable(() -> {
                    log.info("Processed without cached wallet (cache was empty)");
                    return fetchAndProcessWallet(request);
                }).flatMap(monoFunc -> monoFunc))
                .flatMap(response -> {
                    if (response != null && response.getStatusCode().is2xxSuccessful()) {
                        log.info("Invalidating cache after successful processing wallet: {}", request.getWalletId());
                        return invalidateWallet(request.getWalletId())
                                .doOnError(e -> log.info("Cache invalidate failed for wallet: {}",
                                        request.getWalletId(), e))
                                .thenReturn(response);
                    }
                    return Mono.just(response);
                })
                .onErrorResume(NotFoundException.class, e -> {
                    log.info("Wallet operation {} not found", request.getWalletId());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(LowBalanceException.class, e -> {
                    log.info("Wallet operation {} failed, low balance", request.getWalletId());
                    return Mono.just(ResponseEntity.badRequest()
                            .body(createResponse(request, OperationStatus.DENIED)));
                })

                .onErrorResume(e -> {
                    log.info("Error during processing wallet operation: {}", request.getWalletId(), e);
                    return Mono.just(ResponseEntity.badRequest()
                            .body(createDeniedResponse(request)));
                });
    }

    public Mono<ResponseEntity<WalletBalanceDto>> getBalance(UUID walletId) {
        log.info("Getting balance for wallet: {}", walletId);
        return getWallet(walletId)
                .map(cachedWallet -> ResponseEntity.ok(walletDtoMapper.toBalanceDto(cachedWallet)))
                .switchIfEmpty(Mono.defer(() ->
                        walletServiceClient.getWalletInfo(walletId)
                                .flatMap(walletInfo ->
                                        walletInfo == null
                                                ? Mono.just(ResponseEntity.notFound().build())
                                                : cacheWallet(walletInfo)
                                                .thenReturn(ResponseEntity.ok(walletDtoMapper.toBalanceDto(walletInfo)))
                                )
                ));
    }


    private Mono<ResponseEntity<OperationResponseDto>> processWithCachedWallet(
            NewWalletOpsRequest request, WalletCacheDto cachedWallet
    ) {
        if (request.getOperationType() == WalletOperationType.WITHDRAW &&
                cachedWallet.getBalance() < request.getAmount()) {
            return Mono.just(ResponseEntity.unprocessableEntity()
                    .body(createDeniedResponse(request)));
        }

        return Mono.fromRunnable(() -> sendKafkaEvent(request))
                .thenReturn(ResponseEntity.accepted()
                        .body(createResponse(request, OperationStatus.SUCCESS)));
    }

    private Mono<ResponseEntity<OperationResponseDto>> fetchAndProcessWallet(NewWalletOpsRequest request) {
        return walletServiceClient.getWalletInfo(request.getWalletId())
                .flatMap(walletInfo -> {
                    if (walletInfo == null) {
                        log.info("Wallet not found with id {}", request.getWalletId());
                        return Mono.just(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
                    }

                    return cacheWallet(walletInfo)
                            .then(processWithCachedWallet(request, walletInfo));
                });
    }

    private void sendKafkaEvent(NewWalletOpsRequest request) {
        log.info("Sending Kafka event to wallet: {}", request);
        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(request.getWalletId())
                .operationType(request.getOperationType())
                .amount(request.getAmount())
                .operationTrackId(request.getOperationTrackId())
                .build();
        log.info("Event build: {}", event);
        eventProducer.sendEvent(event);
    }

    private Mono<WalletCacheDto> getWallet(UUID walletId) {
        log.info("Get Wallet in cache service: {}", walletId);
        String key = WALLET_KEY_PREFIX + walletId;
        return reactiveRedisTemplate.opsForValue()
                .get(key)
                .doOnSuccess(value -> log.info("Success get from cache : {}", value))
                .doOnError(e -> log.info("Redis error for wallet: {}", walletId, e))
                .onErrorResume(e -> {
                    log.info("Redis unavailable, proceeding without cache for wallet: {}", walletId);
                    return Mono.empty();
                });
    }

    private Mono<Void> cacheWallet(WalletCacheDto wallet) {
        log.info("Cache wallet in cache service: {}", wallet);
        String key = WALLET_KEY_PREFIX + wallet.getWalletId();
        return reactiveRedisTemplate.opsForValue()
                .set(key, wallet, CACHE_TTL)
                .doOnSuccess(v -> log.info("Cached wallet: {}", wallet.getWalletId()))
                .doOnError(e -> log.info("Caching error for wallet: {}", wallet.getWalletId(), e))
                .then();
    }

    private Mono<Boolean> invalidateWallet(UUID walletId) {
        log.info("Invalidate wallet in cache service: {}", walletId);
        String key = WALLET_KEY_PREFIX + walletId;
        return reactiveRedisTemplate.opsForValue()
                .delete(key)
                .doOnSuccess(deleted -> {
                    if (Boolean.TRUE.equals(deleted)) {
                        log.info("Cache invalidate success for wallet: {}", walletId);
                    } else {
                        log.info("Cache invalidate failed for wallet: {}", walletId);
                    }
                })
                .doOnError(e -> log.info("Cache invalidate failed for wallet: {}", walletId, e));
    }

}
