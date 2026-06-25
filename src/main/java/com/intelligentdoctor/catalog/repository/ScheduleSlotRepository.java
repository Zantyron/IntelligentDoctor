package com.intelligentdoctor.catalog.repository;

import com.intelligentdoctor.catalog.entity.ScheduleSlotEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface ScheduleSlotRepository extends JpaRepository<ScheduleSlotEntity, String> {
    List<ScheduleSlotEntity> findByHospitalIdAndDepartmentIdAndSlotDateGreaterThanEqual(String hospitalId, String departmentId, LocalDate slotDate);
    List<ScheduleSlotEntity> findByHospitalIdAndDoctorIdAndSlotDateGreaterThanEqual(String hospitalId, String doctorId, LocalDate slotDate);
    Optional<ScheduleSlotEntity> findByHospitalIdAndDoctorIdAndSlotDateAndPeriod(String hospitalId, String doctorId, LocalDate slotDate, String period);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ScheduleSlotEntity> findWithLockById(String id);
}
