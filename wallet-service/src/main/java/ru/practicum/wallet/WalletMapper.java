package ru.practicum.wallet;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.practicum.redis.WalletCacheDto;

@Mapper(componentModel = "spring")
public interface WalletMapper {

    @Mapping(source = "id", target = "walletId")
    @Mapping(source = "balance", target = "balance")
    WalletCacheDto walletToCacheDto(Wallet wallet);
}
