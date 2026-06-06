package com.intelligentdoctor.admin.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "import_job")
public class ImportJobEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 128)
    private String fileName;

    @Column(nullable = false, length = 32)
    private String fileType;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(length = 500)
    private String storagePath;

    @Column(nullable = false)
    private Integer retryCount = 0;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summaryJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFileType() {
        return fileType;
    }

    public void setFileType(String fileType) {
        this.fileType = fileType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getStoragePath() {
        return storagePath;
    }

    public void setStoragePath(String storagePath) {
        this.storagePath = storagePath;
    }

    public Integer getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(Integer retryCount) {
        this.retryCount = retryCount;
    }

    public String getSummaryJson() {
        return summaryJson;
    }

    public void setSummaryJson(String summaryJson) {
        this.summaryJson = summaryJson;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
