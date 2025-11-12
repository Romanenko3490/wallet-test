package ru.practicum.wallet;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.practicum.redis.WalletCacheDto;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
@Slf4j
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @GetMapping("/wallets/{walletId}")
    public ResponseEntity<WalletCacheDto> getWallet(
            @PathVariable UUID walletId) {

        log.info("getWallet: {}", walletId);
        return ResponseEntity.ok().body(walletService.getWalletInfo(walletId));
    }


}
