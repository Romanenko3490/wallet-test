package ru.practicum.kafka;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.wallet.WalletService;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalletEventConsumer {

    private final WalletService walletService;


    @KafkaListener(topics = "wallet_event", groupId = "wallet-service")
    public void processWalletOperation(KafkaWalletEvent event) {
        log.info("Received event: {}", event);
        walletService.updateBalanceFromEvent(event);
    }

}
