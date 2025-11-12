package ru.practicum.wallet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.TestPropertySource;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@TestPropertySource(properties = {
        "spring.liquibase.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class WalletRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private WalletRepository walletRepository;

    @Test
    void shouldSaveAndRetrieveWallet() {
        UUID walletId = UUID.randomUUID();
        Wallet wallet = Wallet.builder()
                .id(walletId)
                .balance(1000L)
                .currency("RUB")
                .build();

        Wallet saved = walletRepository.save(wallet);
        entityManager.flush();
        entityManager.clear();

        Wallet found = walletRepository.findById(walletId).orElse(null);

        assertThat(found).isNotNull();
        assertThat(found.getId()).isEqualTo(walletId);
        assertThat(found.getBalance()).isEqualTo(1000L);
        assertThat(found.getCurrency()).isEqualTo("RUB");
    }
}