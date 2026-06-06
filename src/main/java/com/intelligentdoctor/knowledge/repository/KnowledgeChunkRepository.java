package com.intelligentdoctor.knowledge.repository;

import com.intelligentdoctor.knowledge.entity.KnowledgeChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunkEntity, String> {
    List<KnowledgeChunkEntity> findByHospitalId(String hospitalId);
    void deleteByHospitalId(String hospitalId);
}
