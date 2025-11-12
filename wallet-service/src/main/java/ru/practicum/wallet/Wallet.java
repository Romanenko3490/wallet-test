package ru.practicum.wallet;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.annotation.Version;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
@ToString
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class Wallet {

    @Id
    @EqualsAndHashCode.Include
    private UUID id;

    private Long balance;
    private String currency;

    @Version
    private Long version;

    private Instant createdAt;
    private Instant updatedAt;
}
