package ru.practicum.kafka;

import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import ru.practicum.enums.WalletOperationType;

import java.time.Instant;
import java.util.UUID;

@Value
@Builder
@Jacksonized
public class KafkaWalletEvent {
    UUID walletId;
    WalletOperationType operationType;
    Long amount;

    @Builder.Default
    Instant timestamp = Instant.now();

    UUID operationTrackId;
}
