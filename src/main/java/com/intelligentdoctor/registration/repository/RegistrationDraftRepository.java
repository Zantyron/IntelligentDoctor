package com.intelligentdoctor.registration.repository;

import com.intelligentdoctor.registration.entity.RegistrationDraftEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationDraftRepository extends JpaRepository<RegistrationDraftEntity, String> {
    List<RegistrationDraftEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);
}
