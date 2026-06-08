package com.intelligentdoctor.registration.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "registration_order", indexes = {
        @Index(name = "idx_order_hospital", columnList = "hospitalId"),
        @Index(name = "idx_order_order_no", columnList = "orderNo", unique = true),
        @Index(name = "idx_order_draft", columnList = "draftId", unique = true),
        @Index(name = "idx_order_slot", columnList = "slotId")
})
public class RegistrationOrderEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String orderNo;

    @Column(nullable = false, length = 64)
    private String draftId;

    @Column(nullable = false, length = 64)
    private String sessionId;

    @Column(nullable = false, length = 64)
    private String departmentId;

    @Column(nullable = false, length = 64)
    private String clinicRoomId;

    @Column(nullable = false, length = 64)
    private String doctorId;

    @Column(nullable = false, length = 64)
    private String slotId;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false, length = 32)
    private String visitPeriod;

    @Column(nullable = false, length = 64)
    private String patientName;

    @Column(nullable = false, length = 32)
    private String patientPhone;

    @Column(nullable = false, length = 32)
    private String idCard;

    @Column(nullable = false, length = 16)
    private String gender;

    @Column(nullable = false)
    private Integer age;

    @Column(nullable = false, length = 32)
    private String status;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String symptomSummary;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getDraftId() {
        return draftId;
    }

    public void setDraftId(String draftId) {
        this.draftId = draftId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
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

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSymptomSummary() {
        return symptomSummary;
    }

    public void setSymptomSummary(String symptomSummary) {
        this.symptomSummary = symptomSummary;
    }
}
