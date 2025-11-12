package ru.practicum.wallet;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;
import ru.practicum.LowBalanceException;
import ru.practicum.NotFoundException;
import ru.practicum.enums.OperationStatus;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.kafka.KafkaWalletEvent;
import ru.practicum.kafka.WalletEventProducer;
import ru.practicum.redis.WalletCacheDto;

import static ru.practicum.wallet.ResponseFactory.createDeniedResponse;
import static ru.practicum.wallet.ResponseFactory.createResponse;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Validated
public class WalletController {
    private final WalletEventProducer eventProducer;
    private final WalletCacheService cacheService;
    private final WalletServiceClient walletServiceClient;

    @PostMapping("/wallet")
    public Mono<ResponseEntity<OperationResponseDto>> processOperation(
            @RequestBody @Valid NewWalletOpsRequest request
    ) {
        log.info("Processing wallet operation: {}", request);

        return cacheService.getWallet(request.getWalletId())
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
                        return cacheService.invalidateWallet(request.getWalletId())
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


    public Mono<ResponseEntity<OperationResponseDto>> processWithCachedWallet(
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

                    return cacheService.cacheWallet(walletInfo)
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

}
