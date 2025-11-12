package ru.practicum.wallet;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.UUID;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/v1")
@Validated
public class WalletController {
    private final WalletCacheService cacheService;


    @PostMapping("/wallet")
    public Mono<ResponseEntity<OperationResponseDto>> processOperation(
            @RequestBody @Valid NewWalletOpsRequest request
    ) {
        return cacheService.processOperation(request);
    }

    @GetMapping("/wallets/{WALLET_UUID}")
    public Mono<ResponseEntity<WalletBalanceDto>> getBalance(
            @PathVariable UUID WALLET_UUID
    ) {
        return cacheService.getBalance(WALLET_UUID);
    }

}
