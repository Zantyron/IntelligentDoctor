package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.DepartmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DepartmentRepository extends JpaRepository<DepartmentEntity, String> {
    List<DepartmentEntity> findByHospitalIdOrderBySortOrderAscNameAsc(String hospitalId);
    Optional<DepartmentEntity> findByHospitalIdAndDepartmentCode(String hospitalId, String departmentCode);
}
