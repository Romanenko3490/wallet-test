package ru.practicum.wallet;

import org.mapstruct.Mapper;
import ru.practicum.redis.WalletCacheDto;

@Mapper(componentModel = "spring")
public interface WalletDtoMapper {


    WalletBalanceDto toBalanceDto(WalletCacheDto cachedWallet);
}
