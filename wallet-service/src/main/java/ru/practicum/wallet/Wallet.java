package ru.practicum.wallet;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Version;

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

    @Version
    private Long version;
}
