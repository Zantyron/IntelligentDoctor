package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlotEntity, String> {
    List<ScheduleSlotEntity> findByHospitalIdAndDepartmentIdAndSlotDateGreaterThanEqual(String hospitalId, String departmentId, LocalDate slotDate);
    List<ScheduleSlotEntity> findByHospitalIdAndDoctorIdAndSlotDateGreaterThanEqual(String hospitalId, String doctorId, LocalDate slotDate);
    Optional<ScheduleSlotEntity> findByHospitalIdAndDoctorIdAndSlotDateAndPeriod(String hospitalId, String doctorId, LocalDate slotDate, String period);
}
