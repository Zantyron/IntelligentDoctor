package com.intelligentdoctor.catalog.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "department", indexes = {
        @Index(name = "idx_department_hospital", columnList = "hospitalId"),
        @Index(name = "idx_department_code", columnList = "hospitalId,departmentCode", unique = true)
})
public class DepartmentEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String departmentCode;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(length = 500)
    private String description;

    @Column(length = 120)
    private String category;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getDepartmentCode() {
        return departmentCode;
    }

    public void setDepartmentCode(String departmentCode) {
        this.departmentCode = departmentCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
