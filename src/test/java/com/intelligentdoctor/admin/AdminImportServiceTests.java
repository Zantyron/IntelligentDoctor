package com.intelligentdoctor.admin;

import com.intelligentdoctor.admin.entity.ImportJobEntity;
import com.intelligentdoctor.admin.repository.ImportJobRepository;
import com.intelligentdoctor.admin.service.AdminImportService;
import com.intelligentdoctor.catalog.repository.ClinicRoomRepository;
import com.intelligentdoctor.catalog.repository.DepartmentRepository;
import com.intelligentdoctor.catalog.repository.DoctorRepository;
import com.intelligentdoctor.catalog.repository.HospitalRepository;
import com.intelligentdoctor.catalog.repository.RegistrationRuleRepository;
import com.intelligentdoctor.catalog.repository.ScheduleSlotRepository;
import com.intelligentdoctor.knowledge.repository.KnowledgeChunkRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminImportServiceTests {

    private static final String HOSPITAL_ID = "hospital-demo";

    @Autowired
    private AdminImportService adminImportService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ImportJobRepository importJobRepository;

    @Autowired
    private HospitalRepository hospitalRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private ClinicRoomRepository clinicRoomRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private ScheduleSlotRepository scheduleSlotRepository;

    @Autowired
    private RegistrationRuleRepository registrationRuleRepository;

    @Autowired
    private KnowledgeChunkRepository knowledgeChunkRepository;

    @Test
    void uploadEndpointShouldCreateImportJobAndExposeStatus() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hospital-knowledge.md",
                "text/markdown",
                Files.readAllBytes(Path.of("sample-data", "hospital-knowledge.md"))
        );

        String response = mockMvc.perform(multipart("/api/admin/imports")
                        .file(file)
                        .header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andReturn()
                .getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        String jobId = response.replaceAll("(?s).*\"id\":\"([^\"]+)\".*", "$1");
        await().untilAsserted(() -> assertThat(importJobRepository.findById(jobId).orElseThrow().getStatus())
                .isIn("COMPLETED", "FAILED"));

        mockMvc.perform(get("/api/admin/imports").header("Authorization", adminAuthHeader()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").exists());
    }

    @Test
    void adminEndpointsShouldRejectMissingCredentials() throws Exception {
        mockMvc.perform(get("/api/admin/imports"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void csvImportShouldPersistMasterDataAndRebuildVectors() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hospital-import.csv",
                "text/csv",
                Files.readAllBytes(Path.of("sample-data", "hospital-import.csv"))
        );

        var submitted = adminImportService.createImportJob(HOSPITAL_ID, file);
        adminImportService.processImportForTest(submitted.id());

        ImportJobEntity job = importJobRepository.findById(submitted.id()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
        assertThat(job.getSummaryJson())
                .contains("\"hospitals\":1", "\"departments\":3", "\"doctors\":3", "\"schedules\":4", "\"rules\":2");

        assertThat(hospitalRepository.findByHospitalCode(HOSPITAL_ID)).isPresent();
        assertThat(departmentRepository.findByHospitalIdAndDepartmentCode(HOSPITAL_ID, "CARD")).isPresent();
        assertThat(clinicRoomRepository.findByHospitalIdAndClinicCode(HOSPITAL_ID, "CARD-VIP")).isPresent();
        assertThat(doctorRepository.findByHospitalIdAndDoctorCode(HOSPITAL_ID, "DOC003")).isPresent();
        assertThat(scheduleSlotRepository.findByHospitalIdAndDoctorIdAndSlotDateGreaterThanEqual(
                HOSPITAL_ID,
                doctorRepository.findByHospitalIdAndDoctorCode(HOSPITAL_ID, "DOC003").orElseThrow().getId(),
                LocalDate.of(2026, 6, 6)
        )).isNotEmpty();
        assertThat(registrationRuleRepository.findByHospitalIdAndDepartmentId(
                HOSPITAL_ID,
                departmentRepository.findByHospitalIdAndDepartmentCode(HOSPITAL_ID, "CARD").orElseThrow().getId()
        )).isNotEmpty();
        assertThat(knowledgeChunkRepository.findByHospitalId(HOSPITAL_ID)).isNotEmpty();
    }

    @Test
    void markdownImportShouldChunkKnowledgeAndExposeVectorSearch() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "hospital-knowledge.md",
                "text/markdown",
                Files.readAllBytes(Path.of("sample-data", "hospital-knowledge.md"))
        );

        var submitted = adminImportService.createImportJob(HOSPITAL_ID, file);
        adminImportService.processImportForTest(submitted.id());

        ImportJobEntity job = importJobRepository.findById(submitted.id()).orElseThrow();
        assertThat(job.getStatus()).isEqualTo("COMPLETED");
        assertThat(job.getSummaryJson()).contains("\"chunks\":");
        assertThat(knowledgeChunkRepository.findByHospitalId(HOSPITAL_ID)).isNotEmpty();
    }

    @Test
    void failedImportShouldKeepErrorForStatusPageAndAllowRetry() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "broken.csv",
                "text/csv",
                "type\nunknown\n".getBytes(StandardCharsets.UTF_8)
        );

        var submitted = adminImportService.createImportJob(HOSPITAL_ID, file);
        adminImportService.processImportForTest(submitted.id());

        ImportJobEntity failed = importJobRepository.findById(submitted.id()).orElseThrow();
        assertThat(failed.getStatus()).isEqualTo("FAILED");
        assertThat(failed.getErrorMessage()).isNotBlank();

        var retry = adminImportService.retryImport(submitted.id());
        assertThat(retry.status()).isEqualTo("PENDING");
        assertThat(retry.retryCount()).isEqualTo(1);
    }

    private String adminAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString("admin:admin".getBytes(StandardCharsets.UTF_8));
    }
}
