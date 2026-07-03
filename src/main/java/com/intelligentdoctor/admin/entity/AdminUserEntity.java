package com.intelligentdoctor.admin.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "admin_user", indexes = {
        @Index(name = "idx_admin_user_hospital_username", columnList = "hospitalId,username", unique = true),
        @Index(name = "idx_admin_user_hospital_role", columnList = "hospitalId,role")
})
public class AdminUserEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String username;

    @Column(nullable = false, length = 256)
    private String passwordHash;

    @Column(nullable = false, length = 32)
    private String role = "HOSPITAL_ADMIN";

    @Column(nullable = false)
    private Boolean enabled = true;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
