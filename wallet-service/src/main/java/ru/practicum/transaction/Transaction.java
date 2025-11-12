package ru.practicum.transaction;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import ru.practicum.enums.WalletOperationType;
import ru.practicum.wallet.Wallet;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Transaction {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id")
    private Wallet wallet;

    @Enumerated(EnumType.STRING)
    @Column(name = "operation_type")
    private WalletOperationType walletOperationType;

    private Long amount;
    private Long previousBalance;
    private Long newBalance;

    private UUID operationTrackId;

    private Instant createdAt;
}
