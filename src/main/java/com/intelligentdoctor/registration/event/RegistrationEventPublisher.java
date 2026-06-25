package com.intelligentdoctor.registration.event;

import com.intelligentdoctor.registration.entity.RegistrationOrderEntity;

import java.util.Optional;

public interface RegistrationEventPublisher {

    Optional<RegistrationOrderEntity> publish(RegistrationReservedEvent event);

    String providerName();
}
