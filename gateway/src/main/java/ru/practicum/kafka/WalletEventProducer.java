package ru.practicum.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class WalletEventProducer {
    private static final String WALLET_EVENT_TOPIC = "wallet_event";

    private final KafkaTemplate<String, KafkaWalletEvent> kafkaTemplate;

    public void sendEvent(KafkaWalletEvent kafkaWalletEvent) {
        String key = kafkaWalletEvent.getWalletId().toString();

        kafkaTemplate.send(WALLET_EVENT_TOPIC, key, kafkaWalletEvent)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Send event failed", error);
                    } else {
                        log.info("Send event success. Track id {}, partition {}, offset {}",
                                key, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
                    }
                });
    }

}
