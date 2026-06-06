package com.intelligentdoctor.knowledge;

import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.knowledge.entity.KnowledgeChunkEntity;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class KnowledgeSearchServiceTests {

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Autowired
    private KnowledgeSearchService knowledgeSearchService;

    @Autowired
    private JsonUtils jsonUtils;

    @Test
    void rebuildShouldIndexKnowledgeChunksForRagSearch() {
        KnowledgeChunkEntity chunk = new KnowledgeChunkEntity();
        chunk.setHospitalId("rag-test-hospital");
        chunk.setSourceType("test");
        chunk.setSourceName("心内科导诊规则");
        chunk.setChunkKey("rag-test-cardio");
        chunk.setChunkText("胸闷、胸痛、心慌患者优先推荐心内科，持续胸痛请前往急诊胸痛中心。");
        chunk.setMetadataJson(jsonUtils.toJson(Map.of("sourceName", "心内科导诊规则")));
        chunk.setExternalVectorId("rag-test-cardio");
        knowledgeChunkRepository.save(chunk);

        knowledgeSearchService.rebuild("rag-test-hospital");

        assertThat(knowledgeSearchService.search("rag-test-hospital", "胸闷 心慌", 3))
                .anySatisfy(snippet -> {
                    assertThat(snippet.sourceName()).isEqualTo("心内科导诊规则");
                    assertThat(snippet.text()).contains("心内科");
                });
    }
}
