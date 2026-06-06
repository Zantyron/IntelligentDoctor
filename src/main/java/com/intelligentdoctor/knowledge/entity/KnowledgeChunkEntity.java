package com.intelligentdoctor.knowledge.entity;

import com.intelligentdoctor.infrastructure.persistence.jpa.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "knowledge_chunk", indexes = {
        @Index(name = "idx_chunk_hospital", columnList = "hospitalId"),
        @Index(name = "idx_chunk_source", columnList = "sourceType,sourceName")
})
public class KnowledgeChunkEntity extends BaseEntity {

    @Column(nullable = false, length = 64)
    private String hospitalId;

    @Column(nullable = false, length = 64)
    private String sourceType;

    @Column(nullable = false, length = 128)
    private String sourceName;

    @Column(nullable = false, length = 64)
    private String chunkKey;

    @Lob
    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(length = 128)
    private String externalVectorId;

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getSourceName() {
        return sourceName;
    }

    public void setSourceName(String sourceName) {
        this.sourceName = sourceName;
    }

    public String getChunkKey() {
        return chunkKey;
    }

    public void setChunkKey(String chunkKey) {
        this.chunkKey = chunkKey;
    }

    public String getChunkText() {
        return chunkText;
    }

    public void setChunkText(String chunkText) {
        this.chunkText = chunkText;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public String getExternalVectorId() {
        return externalVectorId;
    }

    public void setExternalVectorId(String externalVectorId) {
        this.externalVectorId = externalVectorId;
    }
}
