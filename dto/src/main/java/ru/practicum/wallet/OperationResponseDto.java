package ru.practicum.wallet;

import lombok.Builder;
import lombok.Value;
import ru.practicum.enums.OperationStatus;

import java.util.UUID;

@Value
@Builder
public class OperationResponseDto {
    UUID walletID;
    Long amount;
    OperationStatus status;
}
