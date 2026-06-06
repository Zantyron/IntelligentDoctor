package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.HospitalEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface HospitalRepository extends JpaRepository<HospitalEntity, String> {
    Optional<HospitalEntity> findByHospitalCode(String hospitalCode);
}
