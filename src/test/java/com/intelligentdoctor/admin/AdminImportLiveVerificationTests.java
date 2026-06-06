package com.intelligentdoctor.admin;

import com.intelligentdoctor.admin.entity.ImportJobEntity;
import com.intelligentdoctor.admin.repository.ImportJobRepository;
import com.intelligentdoctor.admin.service.AdminImportService;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("dev")
@EnabledIfSystemProperty(named = "live.pinecone", matches = "true",
        disabledReason = "Live verification requires real MySQL, Embedding and Pinecone credentials.")
class AdminImportLiveVerificationTests {

    @Autowired
    private AdminImportService adminImportService;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Test
    void markdownImportShouldWriteMysqlAndPinecone() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hospital-knowledge.md",
                "text/markdown",
                Files.readAllBytes(Path.of("sample-data", "hospital-knowledge.md"))
        );

        var submitted = adminImportService.createImportJob("hospital-demo", file);
        adminImportService.processImportForTest(submitted.id());

        ImportJobEntity job = importJobRepository.findById(submitted.id()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
        assertThat(job.getSummaryJson()).contains("\"vectorProvider\":\"pinecone\"");
        assertThat(knowledgeChunkRepository.findByHospitalId("hospital-demo")).isNotEmpty();
    }
}
