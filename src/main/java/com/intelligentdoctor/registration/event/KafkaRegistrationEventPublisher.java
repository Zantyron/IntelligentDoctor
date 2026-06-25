package com.intelligentdoctor.registration.event;

import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.registration.entity.RegistrationOrderEntity;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@ConditionalOnProperty(prefix = "app.registration", name = "event-provider", havingValue = "kafka")
public class KafkaRegistrationEventPublisher implements RegistrationEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final AppProperties properties;
    private final JsonUtils jsonUtils;

    public KafkaRegistrationEventPublisher(KafkaTemplate<String, String> kafkaTemplate,
                                           AppProperties properties,
                                           JsonUtils jsonUtils) {
        this.kafkaTemplate = kafkaTemplate;
        this.properties = properties;
        this.jsonUtils = jsonUtils;
    }

    @Override
    public Optional<RegistrationOrderEntity> publish(RegistrationReservedEvent event) {
        kafkaTemplate.send(properties.getRegistration().getKafkaTopic(), event.token(), jsonUtils.toJson(event)).join();
        return Optional.empty();
    }

    @Override
    public String providerName() {
        return "kafka";
    }
}
