package ru.practicum.wallet;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.LowBalanceException;
import ru.practicum.NotFoundException;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.kafka.KafkaWalletEvent;
import ru.practicum.redis.WalletCacheDto;
import ru.practicum.transaction.Transaction;
import ru.practicum.transaction.TransactionRepository;

import java.time.Instant;
import java.util.UUID;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class WalletService {

    private final WalletRepository walletRepository;
    private final TransactionRepository transactionRepository;

    private final WalletMapper walletMapper;

    @Transactional(readOnly = true)
    public WalletCacheDto getWalletInfo(UUID walletId) {
        log.info("Getting wallet info for {}", walletId);
        Wallet wallet = walletRepository.findById(walletId).orElseThrow(
                () -> new NotFoundException("Wallet with id: " + walletId + " not found")
        );
        log.info("Found wallet {}", wallet);
        return walletMapper.walletToCacheDto(wallet);
    }

    @Retryable(value = ObjectOptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100))
    public void updateBalanceFromEvent(KafkaWalletEvent event) {
        log.info("Event in processing {}: {}", event);
        Wallet wallet = walletRepository.findById(event.getWalletId()).orElseThrow(
                () -> new NotFoundException("Wallet with id: " + event.getWalletId() + " not found")
        );

        if (transactionRepository.existsByOperationTrackId(event.getOperationTrackId())) {
            log.info("Duplicated operation track id {} already exists", event.getOperationTrackId());
            return;
        }

        if (event.getOperationType() == WalletOperationType.WITHDRAW &&
                wallet.getBalance() < event.getAmount()) {
            throw new LowBalanceException("Not enough balance for processing");
        }

        Long previousBalance = wallet.getBalance();

        Long newBalance = event.getOperationType() == WalletOperationType.DEPOSIT
                ? previousBalance + event.getAmount()
                : previousBalance - event.getAmount();

        wallet.setBalance(newBalance);
        walletRepository.save(wallet);

        Transaction newTransaction = Transaction.builder()
                .id(UUID.randomUUID())
                .wallet(wallet)
                .walletOperationType(event.getOperationType())
                .amount(event.getAmount())
                .previousBalance(previousBalance)
                .newBalance(newBalance)
                .operationTrackId(event.getOperationTrackId())
                .createdAt(Instant.now())
                .build();

        transactionRepository.save(newTransaction);
        log.info("Transaction successfully saved with id: {}", newTransaction.getId());
        log.info("Balance updated. Wallet: {}, New Balance: {}", event.getWalletId(), newBalance);
    }


}
