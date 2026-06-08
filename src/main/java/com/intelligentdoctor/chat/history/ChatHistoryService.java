package com.intelligentdoctor.chat.history;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatMessageView;
import com.intelligentdoctor.chat.dto.ChatSessionView;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

    public List<ChatMessageInput> mergeWithStoredHistory(String sessionId, List<ChatMessageInput> incomingMessages) {
        List<ChatMessageInput> merged = new ArrayList<>();
        try {
            merged.addAll(messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                    .map(message -> new ChatMessageInput(message.getRole(), message.getContent()))
                    .toList());
        } catch (Exception ex) {
            log.warn("Failed to load chat history for context: {}", ex.getMessage());
        }
        merged.addAll(incomingMessages);
        return trimForModel(merged, 16);
    }

    public List<ChatSessionView> listSessions(String hospitalId) {
        try {
            return sessionRepository.findByHospitalIdOrderByUpdatedAtDesc(hospitalId).stream()
                    .map(session -> new ChatSessionView(
                            session.getSessionId(),
                            session.getHospitalId(),
                            session.getMode(),
                            sessionTitle(session.getSessionId()),
                            session.getCreatedAt(),
                            session.getUpdatedAt()
                    ))
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to list chat sessions: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<ChatMessageView> listMessages(String sessionId) {
        try {
            return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                    .map(this::toView)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to list chat messages: {}", ex.getMessage());
            return List.of();
        }
    }

    @Transactional
    public void deleteMessage(String messageId) {
        messageRepository.deleteById(messageId);
    }

    @Transactional
    public void deleteSession(String sessionId) {
        messageRepository.deleteBySessionId(sessionId);
        promptTraceRepository.deleteBySessionId(sessionId);
        toolTraceRepository.deleteBySessionId(sessionId);
        sessionRepository.deleteBySessionId(sessionId);
    }

    @Transactional
    public void deleteAllSessions(String hospitalId) {
        List<String> sessionIds = sessionRepository.findByHospitalIdOrderByUpdatedAtDesc(hospitalId).stream()
                .map(ChatSessionDocument::getSessionId)
                .toList();
        if (sessionIds.isEmpty()) {
            return;
        }
        messageRepository.deleteBySessionIdIn(sessionIds);
        promptTraceRepository.deleteBySessionIdIn(sessionIds);
        toolTraceRepository.deleteBySessionIdIn(sessionIds);
        sessionRepository.deleteBySessionIdIn(sessionIds);
    }

    public void storeChat(String sessionId,
                          String hospitalId,
                          ChatMode mode,
                          boolean consent,
                          Iterable<ChatMessageInput> messages,
                          String assistantReply,
                          AiPromptContext promptContext) {
        if (!consent) {
            return;
        }
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

            List<ChatMessageDocument> messageDocuments = new ArrayList<>();
            for (ChatMessageInput input : messages) {
                ChatMessageDocument messageDocument = new ChatMessageDocument();
                messageDocument.setSessionId(sessionId);
                messageDocument.setRole(input.role());
                messageDocument.setContent(input.content());
                messageDocument.setCreatedAt(now);
                messageDocuments.add(messageDocument);
            }

            ChatMessageDocument assistantMessage = new ChatMessageDocument();
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(assistantReply);
            assistantMessage.setCreatedAt(now);
            messageDocuments.add(assistantMessage);
            messageRepository.saveAll(messageDocuments);

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

    private List<ChatMessageInput> trimForModel(List<ChatMessageInput> messages, int maxMessages) {
        return messages.stream()
                .skip(Math.max(0, messages.size() - maxMessages))
                .toList();
    }

    private String sessionTitle(String sessionId) {
        return messageRepository.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .filter(message -> "user".equalsIgnoreCase(message.getRole()))
                .min(Comparator.comparing(ChatMessageDocument::getCreatedAt))
                .map(ChatMessageDocument::getContent)
                .map(content -> content.length() > 24 ? content.substring(0, 24) + "..." : content)
                .orElse("新对话");
    }

    private ChatMessageView toView(ChatMessageDocument message) {
        return new ChatMessageView(
                message.getId(),
                message.getSessionId(),
                message.getRole(),
                message.getContent(),
                message.getCreatedAt()
        );
    }
}
