package ru.practicum.wallet;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class WalletBalanceDto {
    Long balance;
    String currency;
}
