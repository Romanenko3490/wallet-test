package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import ru.practicum.NotFoundException;
import ru.practicum.redis.WalletCacheDto;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(WalletController.class)
class WalletControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WalletService walletService;

    @Test
    void getWallet_shouldReturnWallet() throws Exception {
        UUID walletId = UUID.randomUUID();
        WalletCacheDto cacheDto = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(1000L)
                .currency("RUB")
                .build();

        when(walletService.getWalletInfo(walletId)).thenReturn(cacheDto);

        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.walletId").value(walletId.toString()))
                .andExpect(jsonPath("$.balance").value(1000L))
                .andExpect(jsonPath("$.currency").value("RUB"));
    }

    @Test
    void getWallet_shouldReturnNotFound() throws Exception {
        UUID walletId = UUID.randomUUID();
        when(walletService.getWalletInfo(walletId))
                .thenThrow(new NotFoundException("Wallet with id: " + walletId + " not found"));

        mockMvc.perform(get("/api/v1/wallets/{walletId}", walletId))
                .andExpect(status().isNotFound())
                .andExpect(content().string("Wallet with id: " + walletId + " not found"));
    }
}