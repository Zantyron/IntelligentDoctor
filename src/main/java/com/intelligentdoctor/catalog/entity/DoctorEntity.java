package com.intelligentdoctor.catalog.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctor", indexes = {
        @Index(name = "idx_doctor_hospital", columnList = "hospitalId"),
        @Index(name = "idx_doctor_department", columnList = "departmentId"),
        @Index(name = "idx_doctor_clinic", columnList = "clinicRoomId")
})
public class DoctorEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String departmentId;

    @Column(length = 64)
    private String clinicRoomId;

    @Column(nullable = false, length = 64)
    private String doctorCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 64)
    private String title;

    @Column(length = 120)
    private String specialty;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String introduction;

    @Column(length = 255)
    private String avatarUrl;

    @Column(nullable = false)
    private Boolean hotExpert = false;

    @Column(nullable = false)
    private Integer consultationFee = 0;

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

    public String getDoctorCode() {
        return doctorCode;
    }

    public void setDoctorCode(String doctorCode) {
        this.doctorCode = doctorCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getIntroduction() {
        return introduction;
    }

    public void setIntroduction(String introduction) {
        this.introduction = introduction;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Boolean getHotExpert() {
        return hotExpert;
    }

    public void setHotExpert(Boolean hotExpert) {
        this.hotExpert = hotExpert;
    }

    public Integer getConsultationFee() {
        return consultationFee;
    }

    public void setConsultationFee(Integer consultationFee) {
        this.consultationFee = consultationFee;
    }
}
