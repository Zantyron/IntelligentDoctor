package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.ClinicRoomEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ClinicRoomRepository extends JpaRepository<ClinicRoomEntity, String> {
    List<ClinicRoomEntity> findByHospitalIdAndDepartmentId(String hospitalId, String departmentId);
    Optional<ClinicRoomEntity> findByHospitalIdAndClinicCode(String hospitalId, String clinicCode);
}
