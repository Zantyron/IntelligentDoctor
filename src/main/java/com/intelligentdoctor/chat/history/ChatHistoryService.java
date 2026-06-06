package com.intelligentdoctor.chat.history;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class ChatHistoryService {

    private static final Logger log = LoggerFactory.getLogger(ChatHistoryService.class);

    private final ChatSessionMongoRepository sessionRepository;
    private final ChatMessageMongoRepository messageRepository;
    private final PromptTraceMongoRepository promptTraceRepository;
    private final ToolTraceMongoRepository toolTraceRepository;
    private final JsonUtils jsonUtils;

    public ChatHistoryService(ChatSessionMongoRepository sessionRepository,
                              ChatMessageMongoRepository messageRepository,
                              PromptTraceMongoRepository promptTraceRepository,
                              ToolTraceMongoRepository toolTraceRepository,
                              JsonUtils jsonUtils) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.promptTraceRepository = promptTraceRepository;
        this.toolTraceRepository = toolTraceRepository;
        this.jsonUtils = jsonUtils;
    }

    public void storeChat(String sessionId,
                          String hospitalId,
                          ChatMode mode,
                          boolean consent,
                          Iterable<ChatMessageInput> messages,
                          String assistantReply,
                          AiPromptContext promptContext) {
        try {
            ChatSessionDocument session = sessionRepository.findBySessionId(sessionId)
                    .orElseGet(ChatSessionDocument::new);
            LocalDateTime now = LocalDateTime.now();
            if (session.getCreatedAt() == null) {
                session.setCreatedAt(now);
            }
            session.setSessionId(sessionId);
            session.setHospitalId(hospitalId);
            session.setMode(mode.name());
            session.setConsentToStoreHistory(consent);
            session.setUpdatedAt(now);
            sessionRepository.save(session);

            for (ChatMessageInput input : messages) {
                ChatMessageDocument messageDocument = new ChatMessageDocument();
                messageDocument.setSessionId(sessionId);
                messageDocument.setRole(input.role());
                messageDocument.setContent(input.content());
                messageDocument.setCreatedAt(now);
                messageRepository.save(messageDocument);
            }

            ChatMessageDocument assistantMessage = new ChatMessageDocument();
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(assistantReply);
            assistantMessage.setCreatedAt(now);
            messageRepository.save(assistantMessage);

            PromptTraceDocument promptTrace = new PromptTraceDocument();
            promptTrace.setSessionId(sessionId);
            promptTrace.setMode(mode.name());
            promptTrace.setPromptContent(String.join("\n\n",
                    promptContext.systemPrompt(),
                    promptContext.businessPrompt(),
                    promptContext.ragPrompt(),
                    promptContext.toolPrompt()));
            promptTrace.setCreatedAt(now);
            promptTraceRepository.save(promptTrace);
        } catch (Exception ex) {
            log.warn("Failed to store chat history, fallback to logs only: {}", ex.getMessage());
        }
    }

    public void storeToolTrace(String sessionId, String toolName, Map<String, Object> arguments, Object result) {
        try {
            ToolTraceDocument toolTrace = new ToolTraceDocument();
            toolTrace.setSessionId(sessionId);
            toolTrace.setToolName(toolName);
            toolTrace.setArgumentsJson(jsonUtils.toJson(arguments));
            toolTrace.setResultJson(jsonUtils.toJson(result));
            toolTrace.setCreatedAt(LocalDateTime.now());
            toolTraceRepository.save(toolTrace);
        } catch (Exception ex) {
            log.warn("Failed to store tool trace, fallback to logs only: {}", ex.getMessage());
        }
    }
}
