package com.intelligentdoctor.admin.repository;

import com.intelligentdoctor.admin.entity.ImportJobEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportJobRepository extends JpaRepository<ImportJobEntity, String> {
    List<ImportJobEntity> findByHospitalIdOrderByCreatedAtDesc(String hospitalId);
}
