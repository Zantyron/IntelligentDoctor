package com.intelligentdoctor.registration.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "registration_draft")
public class RegistrationDraftEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String symptomSummary;

    @Column(length = 64)
    private String departmentId;

    @Column(length = 64)
    private String clinicRoomId;

    @Column(length = 64)
    private String doctorId;

    @Column(length = 64)
    private String slotId;

    private LocalDate visitDate;

    @Column(length = 32)
    private String visitPeriod;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 64)
    private String patientName;

    @Column(length = 32)
    private String patientPhone;

    @Column(length = 32)
    private String idCard;

    @Column(length = 64)
    private String patientId;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSymptomSummary() {
        return symptomSummary;
    }

    public void setSymptomSummary(String symptomSummary) {
        this.symptomSummary = symptomSummary;
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

    public String getSlotId() {
        return slotId;
    }

    public void setSlotId(String slotId) {
        this.slotId = slotId;
    }

    public LocalDate getVisitDate() {
        return visitDate;
    }

    public void setVisitDate(LocalDate visitDate) {
        this.visitDate = visitDate;
    }

    public String getVisitPeriod() {
        return visitPeriod;
    }

    public void setVisitPeriod(String visitPeriod) {
        this.visitPeriod = visitPeriod;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientPhone() {
        return patientPhone;
    }

    public void setPatientPhone(String patientPhone) {
        this.patientPhone = patientPhone;
    }

    public String getIdCard() {
        return idCard;
    }

    public void setIdCard(String idCard) {
        this.idCard = idCard;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }
}
