package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Retryable;
import ru.practicum.LowBalanceException;
import ru.practicum.NotFoundException;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.kafka.KafkaWalletEvent;
import ru.practicum.redis.WalletCacheDto;
import ru.practicum.transaction.TransactionRepository;

import java.lang.reflect.Method;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    @Mock
    private WalletRepository walletRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private WalletMapper walletMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    void updateBalanceFromEvent_shouldUpdateBalanceForDeposit() {
        UUID walletId = UUID.randomUUID();
        UUID operationTrackId = UUID.randomUUID();

        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.DEPOSIT)
                .amount(100L)
                .operationTrackId(operationTrackId)
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(500L)
                .currency("RUB")
                .version(1L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByOperationTrackId(operationTrackId)).thenReturn(false);

        walletService.updateBalanceFromEvent(event);

        verify(walletRepository).save(wallet);
        assertEquals(600L, wallet.getBalance()); // 500 + 100

        verify(transactionRepository).save(argThat(transaction ->
                transaction.getWallet().equals(wallet) &&
                        transaction.getWalletOperationType() == WalletOperationType.DEPOSIT &&
                        transaction.getAmount().equals(100L) &&
                        transaction.getPreviousBalance().equals(500L) &&
                        transaction.getNewBalance().equals(600L) &&
                        transaction.getOperationTrackId().equals(operationTrackId)
        ));
    }

    @Test
    void updateBalanceFromEvent_shouldUpdateBalanceForWithdraw() {
        UUID walletId = UUID.randomUUID();
        UUID operationTrackId = UUID.randomUUID();

        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.WITHDRAW)
                .amount(100L)
                .operationTrackId(operationTrackId)
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(500L)
                .currency("RUB")
                .version(1L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByOperationTrackId(operationTrackId)).thenReturn(false);

        walletService.updateBalanceFromEvent(event);

        verify(walletRepository).save(wallet);
        assertEquals(400L, wallet.getBalance()); // 500 - 100

        verify(transactionRepository).save(argThat(transaction ->
                transaction.getNewBalance().equals(400L)
        ));
    }

    @Test
    void updateBalanceFromEvent_shouldThrowLowBalanceException() {
        UUID walletId = UUID.randomUUID();
        UUID operationTrackId = UUID.randomUUID();

        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.WITHDRAW)
                .amount(1000L) // Больше чем баланс
                .operationTrackId(operationTrackId)
                .build();

        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(500L) // Меньше чем списание
                .currency("RUB")
                .version(1L)
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(transactionRepository.existsByOperationTrackId(operationTrackId)).thenReturn(false);

        assertThrows(LowBalanceException.class, () -> {
            walletService.updateBalanceFromEvent(event);
        });

        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateBalanceFromEvent_shouldSkipDuplicateOperation() {
        UUID walletId = UUID.randomUUID();
        UUID operationTrackId = UUID.randomUUID();

        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.DEPOSIT)
                .amount(100L)
                .operationTrackId(operationTrackId)
                .build();

        when(transactionRepository.existsByOperationTrackId(operationTrackId)).thenReturn(true);

        walletService.updateBalanceFromEvent(event);

        verify(walletRepository, never()).findById(any());
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateBalanceFromEvent_shouldThrowNotFoundException() {
        UUID walletId = UUID.randomUUID();
        UUID operationTrackId = UUID.randomUUID();

        KafkaWalletEvent event = KafkaWalletEvent.builder()
                .walletId(walletId)
                .operationType(WalletOperationType.DEPOSIT)
                .amount(100L)
                .operationTrackId(operationTrackId)
                .build();

        when(transactionRepository.existsByOperationTrackId(operationTrackId)).thenReturn(false);
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            walletService.updateBalanceFromEvent(event);
        });

        verify(transactionRepository).existsByOperationTrackId(operationTrackId);
        verify(walletRepository).findById(walletId);
        verify(walletRepository, never()).save(any());
        verify(transactionRepository, never()).save(any());
    }

    @Test
    void updateBalanceFromEvent_shouldBeAnnotatedWithRetryable() throws NoSuchMethodException {
        Method method = WalletService.class.getMethod("updateBalanceFromEvent", KafkaWalletEvent.class);

        Retryable retryable = method.getAnnotation(Retryable.class);

        assertNotNull(retryable);
        assertEquals(ObjectOptimisticLockingFailureException.class, retryable.value()[0]);
        assertEquals(3, retryable.maxAttempts());
        assertEquals(100, retryable.backoff().delay());
    }

    @Test
    void getWalletInfo_shouldReturnWalletCacheDto() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(1000L)
                .currency("RUB")
                .build();

        WalletCacheDto cacheDto = WalletCacheDto.builder()
                .walletId(walletId)
                .balance(1000L)
                .currency("RUB")
                .build();

        when(walletRepository.findById(walletId)).thenReturn(Optional.of(wallet));
        when(walletMapper.walletToCacheDto(wallet)).thenReturn(cacheDto);

        WalletCacheDto result = walletService.getWalletInfo(walletId);

        assertNotNull(result);
        assertEquals(walletId, result.getWalletId());
        assertEquals(1000L, result.getBalance());
        assertEquals("RUB", result.getCurrency());

        verify(walletRepository).findById(walletId);
        verify(walletMapper).walletToCacheDto(wallet);
    }

    @Test
    void getWalletInfo_shouldThrowNotFoundException() {
        UUID walletId = UUID.randomUUID();
        when(walletRepository.findById(walletId)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> {
            walletService.getWalletInfo(walletId);
        });

        verify(walletMapper, never()).walletToCacheDto(any());
    }
}