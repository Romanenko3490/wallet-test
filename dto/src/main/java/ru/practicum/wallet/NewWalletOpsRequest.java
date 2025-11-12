package ru.practicum.wallet;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import ru.practicum.enums.WalletOperationType;

import java.util.UUID;

@Value
@Jacksonized
@Builder
public class NewWalletOpsRequest {

    @NotNull
    UUID walletId;

    @NotNull
    WalletOperationType operationType;

    @NotNull
    @Positive
    Long amount;

    @Builder.Default
    private UUID operationTrackId =  UUID.randomUUID();
}
