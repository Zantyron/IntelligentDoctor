package com.intelligentdoctor;

import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatStreamRequest;
import com.intelligentdoctor.registration.dto.CreateDraftCommand;
import com.intelligentdoctor.registration.service.RegistrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class IntelligentDoctorApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RegistrationService registrationService;

    @Test
    void contextLoads() {
        assertThat(registrationService).isNotNull();
    }

    @Test
    void systemProfileShouldBeAvailable() throws Exception {
        mockMvc.perform(get("/api/system/profile"))
                .andExpect(status().isOk());
    }

    @Test
    void chatEndpointShouldReturnSse() throws Exception {
        String payload = """
                {
                  "sessionId": "test-session",
                  "messages": [
                    { "role": "user", "content": "最近发烧咳嗽，想知道挂什么科" }
                  ],
                  "consentToStoreHistory": false
                }
                """;
        mockMvc.perform(post("/api/chat/diagnosis/stream")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void createDraftShouldWork() {
        var draft = registrationService.createDraft(new CreateDraftCommand(
                "hospital-demo",
                "draft-session",
                "发烧咳嗽三天",
                "dept-1",
                "clinic-1",
                "doctor-1",
                "slot-1",
                LocalDate.now().plusDays(1),
                "上午",
                null,
                null,
                null,
                null,
                null,
                null
        ));
        assertThat(draft.draftId()).isNotBlank();
        assertThat(draft.status()).isEqualTo("DRAFT");
    }
}
