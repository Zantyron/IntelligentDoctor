package com.intelligentdoctor.catalog.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "schedule_slot", indexes = {
        @Index(name = "idx_slot_hospital", columnList = "hospitalId"),
        @Index(name = "idx_slot_doctor_date", columnList = "doctorId,slotDate"),
        @Index(name = "idx_slot_department_date", columnList = "departmentId,slotDate")
})
public class ScheduleSlotEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String departmentId;

    @Column(nullable = false, length = 64)
    private String clinicRoomId;

    @Column(nullable = false, length = 64)
    private String doctorId;

    @Column(nullable = false)
    private LocalDate slotDate;

    @Column(nullable = false, length = 32)
    private String period;

    @Column(nullable = false)
    private Integer stockTotal;

    @Column(nullable = false)
    private Integer stockAvailable;

    @Column(nullable = false)
    private Boolean hotSlot = false;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getDepartmentId() {
        return departmentId;
    }

    public void setDepartmentId(String departmentId) {
        this.departmentId = departmentId;
    }

    public String getClinicRoomId() {
        return clinicRoomId;
    }

    public void setClinicRoomId(String clinicRoomId) {
        this.clinicRoomId = clinicRoomId;
    }

    public String getDoctorId() {
        return doctorId;
    }

    public void setDoctorId(String doctorId) {
        this.doctorId = doctorId;
    }

    public LocalDate getSlotDate() {
        return slotDate;
    }

    public void setSlotDate(LocalDate slotDate) {
        this.slotDate = slotDate;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public Integer getStockTotal() {
        return stockTotal;
    }

    public void setStockTotal(Integer stockTotal) {
        this.stockTotal = stockTotal;
    }

    public Integer getStockAvailable() {
        return stockAvailable;
    }

    public void setStockAvailable(Integer stockAvailable) {
        this.stockAvailable = stockAvailable;
    }

    public Boolean getHotSlot() {
        return hotSlot;
    }

    public void setHotSlot(Boolean hotSlot) {
        this.hotSlot = hotSlot;
    }
}
