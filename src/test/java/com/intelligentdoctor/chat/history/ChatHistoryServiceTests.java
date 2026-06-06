package com.intelligentdoctor.chat.history;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.history.document.ChatMessageDocument;
import com.intelligentdoctor.chat.history.document.ChatSessionDocument;
import com.intelligentdoctor.chat.history.document.PromptTraceDocument;
import com.intelligentdoctor.chat.history.document.ToolTraceDocument;
import com.intelligentdoctor.chat.history.repository.ChatMessageMongoRepository;
import com.intelligentdoctor.chat.history.repository.ChatSessionMongoRepository;
import com.intelligentdoctor.chat.history.repository.PromptTraceMongoRepository;
import com.intelligentdoctor.chat.history.repository.ToolTraceMongoRepository;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.common.JsonUtils;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChatHistoryServiceTests {

    @Test
    void storeChatPersistsSessionMessagesAndPromptTraceEvenWithoutConsent() {
        ChatSessionMongoRepository sessionRepository = mock(ChatSessionMongoRepository.class);
        ChatMessageMongoRepository messageRepository = mock(ChatMessageMongoRepository.class);
        PromptTraceMongoRepository promptTraceRepository = mock(PromptTraceMongoRepository.class);
        ToolTraceMongoRepository toolTraceRepository = mock(ToolTraceMongoRepository.class);
        ChatHistoryService service = new ChatHistoryService(
                sessionRepository,
                messageRepository,
                promptTraceRepository,
                toolTraceRepository,
                new JsonUtils(new ObjectMapper())
        );
        when(sessionRepository.findBySessionId("session-1")).thenReturn(Optional.empty());

        service.storeChat(
                "session-1",
                "hospital-demo",
                ChatMode.DIAGNOSIS,
                false,
                List.of(new ChatMessageInput("user", "发热咳嗽三天")),
                "建议呼吸内科就诊",
                new AiPromptContext(
                        ChatMode.DIAGNOSIS,
                        "system prompt",
                        "business prompt",
                        "rag prompt",
                        "tool prompt",
                        List.of("evidence")
                )
        );

        ArgumentCaptor<ChatSessionDocument> sessionCaptor = ArgumentCaptor.forClass(ChatSessionDocument.class);
        ArgumentCaptor<PromptTraceDocument> promptCaptor = ArgumentCaptor.forClass(PromptTraceDocument.class);
        verify(sessionRepository).save(sessionCaptor.capture());
        verify(messageRepository, times(2)).save(any(ChatMessageDocument.class));
        verify(promptTraceRepository).save(promptCaptor.capture());

        assertThat(sessionCaptor.getValue().getSessionId()).isEqualTo("session-1");
        assertThat(sessionCaptor.getValue().isConsentToStoreHistory()).isFalse();
        assertThat(promptCaptor.getValue().getPromptContent())
                .contains("system prompt", "business prompt", "rag prompt", "tool prompt");
    }

    @Test
    void storeToolTracePersistsArgumentsAndResultJson() {
        ChatSessionMongoRepository sessionRepository = mock(ChatSessionMongoRepository.class);
        ChatMessageMongoRepository messageRepository = mock(ChatMessageMongoRepository.class);
        PromptTraceMongoRepository promptTraceRepository = mock(PromptTraceMongoRepository.class);
        ToolTraceMongoRepository toolTraceRepository = mock(ToolTraceMongoRepository.class);
        ChatHistoryService service = new ChatHistoryService(
                sessionRepository,
                messageRepository,
                promptTraceRepository,
                toolTraceRepository,
                new JsonUtils(new ObjectMapper())
        );

        service.storeToolTrace("session-1", "ragSearch", Map.of("query", "胸闷"), List.of("心内科知识"));

        ArgumentCaptor<ToolTraceDocument> traceCaptor = ArgumentCaptor.forClass(ToolTraceDocument.class);
        verify(toolTraceRepository).save(traceCaptor.capture());
        assertThat(traceCaptor.getValue().getSessionId()).isEqualTo("session-1");
        assertThat(traceCaptor.getValue().getToolName()).isEqualTo("ragSearch");
        assertThat(traceCaptor.getValue().getArgumentsJson()).contains("query", "胸闷");
        assertThat(traceCaptor.getValue().getResultJson()).contains("心内科知识");
    }
}
