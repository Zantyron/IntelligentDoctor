package com.intelligentdoctor.registration.event;

import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.registration.service.RegistrationOrderPersistenceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.registration", name = "event-provider", havingValue = "kafka")
public class KafkaRegistrationEventConsumer {

    private final RegistrationOrderPersistenceService persistenceService;
    private final JsonUtils jsonUtils;

    public KafkaRegistrationEventConsumer(RegistrationOrderPersistenceService persistenceService,
                                          JsonUtils jsonUtils,
                                          AppProperties properties) {
        this.persistenceService = persistenceService;
        this.jsonUtils = jsonUtils;
    }

    @KafkaListener(topics = "${app.registration.kafka-topic}")
    public void consume(String payload) {
        RegistrationReservedEvent event = jsonUtils.fromJson(payload, RegistrationReservedEvent.class);
        persistenceService.persist(event);
    }
}
