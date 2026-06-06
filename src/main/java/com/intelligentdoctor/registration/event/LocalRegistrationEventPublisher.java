package com.intelligentdoctor.registration.event;

import com.intelligentdoctor.registration.service.RegistrationOrderPersistenceService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "app.registration", name = "event-provider", havingValue = "local")
public class LocalRegistrationEventPublisher implements RegistrationEventPublisher {

    private final RegistrationOrderPersistenceService persistenceService;

    public LocalRegistrationEventPublisher(RegistrationOrderPersistenceService persistenceService) {
        this.persistenceService = persistenceService;
    }

    @Override
    public void publish(RegistrationReservedEvent event) {
        persistenceService.persist(event);
    }

    @Override
    public String providerName() {
        return "local";
    }
}
