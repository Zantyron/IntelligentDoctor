package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.DoctorEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<DoctorEntity, String> {
    List<DoctorEntity> findByHospitalIdAndDepartmentId(String hospitalId, String departmentId);
    List<DoctorEntity> findByHospitalIdAndClinicRoomId(String hospitalId, String clinicRoomId);
    Optional<DoctorEntity> findByHospitalIdAndDoctorCode(String hospitalId, String doctorCode);
}
