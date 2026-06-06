package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.RegistrationRuleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegistrationRuleRepository extends JpaRepository<RegistrationRuleEntity, String> {
    List<RegistrationRuleEntity> findByHospitalIdAndDepartmentId(String hospitalId, String departmentId);
}
