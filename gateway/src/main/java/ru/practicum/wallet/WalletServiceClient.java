package ru.practicum.wallet;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import ru.practicum.NotFoundException;
import ru.practicum.base.BaseClient;
import ru.practicum.redis.WalletCacheDto;

import java.util.UUID;

@Service
@Slf4j
public class WalletServiceClient extends BaseClient {

    private static final String API_PREFIX = "/api/v1";

    public WalletServiceClient(@Value("${wallet-service.url}") String baseUrl) {
        super(baseUrl, API_PREFIX);
    }

    public Mono<WalletCacheDto> getWalletInfo(UUID walletId) {
        log.info("Get wallet info from wallet-service: {}", walletId);
        return webClient.get()
                .uri("/wallets/" + walletId)
                .retrieve()
                .onStatus(status -> status == HttpStatus.NOT_FOUND,
                        response -> Mono.error(new NotFoundException("Wallet not found: " + walletId)))
                .bodyToMono(WalletCacheDto.class)
                .doOnError(e -> {
                    if (!(e instanceof NotFoundException)) {
                        log.error("Error getting wallet cache for {}", walletId);
                    }
                });
    }
}
