package ru.practicum.redis;

import lombok.Builder;
import lombok.Value;

import java.util.UUID;

@Value
@Builder
public class WalletCacheDto {
    UUID walletId;
    Long balance;
}
