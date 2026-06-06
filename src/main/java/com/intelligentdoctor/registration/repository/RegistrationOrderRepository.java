package com.intelligentdoctor.registration.repository;

import com.intelligentdoctor.registration.entity.RegistrationOrderEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegistrationOrderRepository extends JpaRepository<RegistrationOrderEntity, String> {
    Optional<RegistrationOrderEntity> findByOrderNo(String orderNo);
    Optional<RegistrationOrderEntity> findByDraftId(String draftId);
    List<RegistrationOrderEntity> findByHospitalIdOrderByCreatedAtDesc(String hospitalId);
}
