package com.intelligentdoctor.registration.event;

public interface RegistrationEventPublisher {

    void publish(RegistrationReservedEvent event);

    String providerName();
}
