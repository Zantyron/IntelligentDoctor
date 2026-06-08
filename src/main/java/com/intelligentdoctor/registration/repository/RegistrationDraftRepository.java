package com.intelligentdoctor.registration.repository;

import com.intelligentdoctor.registration.entity.RegistrationDraftEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface RegistrationDraftRepository extends JpaRepository<RegistrationDraftEntity, String> {
    List<RegistrationDraftEntity> findBySessionIdOrderByCreatedAtDesc(String sessionId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RegistrationDraftEntity> findWithLockById(String id);

    void deleteBySessionIdAndStatusNot(String sessionId, String status);
}
