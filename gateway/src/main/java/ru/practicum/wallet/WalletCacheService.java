package ru.practicum.wallet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.redis.WalletCacheDto;

import java.time.Duration;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
public class WalletCacheService {
    private final ReactiveRedisTemplate<String, WalletCacheDto> reactiveRedisTemplate;

    private static final String WALLET_KEY_PREFIX = "wallet:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(5);

    public Mono<WalletCacheDto> getWallet(UUID walletId) {
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

    public Mono<Void> cacheWallet(WalletCacheDto wallet) {
        log.info("Cache wallet in cache service: {}", wallet);
        String key = WALLET_KEY_PREFIX + wallet.getWalletId();
        return reactiveRedisTemplate.opsForValue()
                .set(key, wallet, CACHE_TTL)
                .doOnSuccess(v -> log.info("Cached wallet: {}", wallet.getWalletId()))
                .doOnError(e -> log.info("Caching error for wallet: {}", wallet.getWalletId(), e))
                .then();
    }

//    public Mono<Void> updateBalance(UUID walletId, WalletOperationType operationType, Long amount) {
//        return getWallet(walletId)
//                .flatMap(cached -> {
//                    if (cached != null) {
//                        Long newBalance = operationType == WalletOperationType.DEPOSIT
//                                ? cached.getBalance() + amount
//                                : cached.getBalance() - amount;
//
//                        WalletCacheDto update = WalletCacheDto.builder()
//                                .walletId(cached.getWalletId())
//                                .balance(newBalance)
//                                .build();
//
//                        return cacheWallet(update);
//                    }
//                    return Mono.empty();
//                })
//                .doOnError(e -> log.error("Error updating balance in cache: {}", walletId, e));
//    }

    public Mono<Boolean> invalidateWallet(UUID walletId) {
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
