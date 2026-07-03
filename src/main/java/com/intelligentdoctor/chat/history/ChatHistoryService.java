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
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.auth.AdminPrincipal;
import com.intelligentdoctor.auth.TerminalSecurityContext;
import com.intelligentdoctor.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final AppProperties properties;

    @Autowired
    public ChatHistoryService(ChatSessionMongoRepository sessionRepository,
                              ChatMessageMongoRepository messageRepository,
                              PromptTraceMongoRepository promptTraceRepository,
                              ToolTraceMongoRepository toolTraceRepository,
                              JsonUtils jsonUtils,
                              AppProperties properties) {
        this.sessionRepository = sessionRepository;
        this.messageRepository = messageRepository;
        this.promptTraceRepository = promptTraceRepository;
        this.toolTraceRepository = toolTraceRepository;
        this.jsonUtils = jsonUtils;
        this.properties = properties;
    }

    ChatHistoryService(ChatSessionMongoRepository sessionRepository,
                       ChatMessageMongoRepository messageRepository,
                       PromptTraceMongoRepository promptTraceRepository,
                       ToolTraceMongoRepository toolTraceRepository,
                       JsonUtils jsonUtils) {
        this(sessionRepository, messageRepository, promptTraceRepository, toolTraceRepository, jsonUtils, new AppProperties());
    }

    public List<ChatMessageInput> mergeWithStoredHistory(String sessionId, List<ChatMessageInput> incomingMessages) {
        List<ChatMessageInput> merged = new ArrayList<>();
        try {
            merged.addAll(messageRepository.findByHospitalIdAndSessionIdOrderByCreatedAtAsc(currentHospitalId(), sessionId).stream()
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
                            session.getTerminalUsername(),
                            session.getMode(),
                            sessionTitle(hospitalId, session.getSessionId()),
                            session.getCreatedAt(),
                            session.getUpdatedAt()
                    ))
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to list chat sessions: {}", ex.getMessage());
            return List.of();
        }
    }

    public List<ChatMessageView> listMessages(String hospitalId, String sessionId) {
        try {
            return messageRepository.findByHospitalIdAndSessionIdOrderByCreatedAtAsc(hospitalId, sessionId).stream()
                    .map(this::toView)
                    .toList();
        } catch (Exception ex) {
            log.warn("Failed to list chat messages: {}", ex.getMessage());
            return List.of();
        }
    }

    @Transactional
    public void deleteMessage(String hospitalId, String messageId) {
        messageRepository.deleteByHospitalIdAndId(hospitalId, messageId);
    }

    @Transactional
    public void deleteSession(String hospitalId, String sessionId) {
        messageRepository.deleteByHospitalIdAndSessionId(hospitalId, sessionId);
        promptTraceRepository.deleteByHospitalIdAndSessionId(hospitalId, sessionId);
        toolTraceRepository.deleteByHospitalIdAndSessionId(hospitalId, sessionId);
        sessionRepository.deleteByHospitalIdAndSessionId(hospitalId, sessionId);
    }

    @Transactional
    public void deleteAllSessions(String hospitalId) {
        List<String> sessionIds = sessionRepository.findByHospitalIdOrderByUpdatedAtDesc(hospitalId).stream()
                .map(ChatSessionDocument::getSessionId)
                .toList();
        if (sessionIds.isEmpty()) {
            return;
        }
        messageRepository.deleteByHospitalIdAndSessionIdIn(hospitalId, sessionIds);
        promptTraceRepository.deleteByHospitalIdAndSessionIdIn(hospitalId, sessionIds);
        toolTraceRepository.deleteByHospitalIdAndSessionIdIn(hospitalId, sessionIds);
        sessionRepository.deleteByHospitalIdAndSessionIdIn(hospitalId, sessionIds);
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
            ChatSessionDocument session = sessionRepository.findByHospitalIdAndSessionId(hospitalId, sessionId)
                    .orElseGet(ChatSessionDocument::new);
            LocalDateTime now = LocalDateTime.now();
            if (session.getCreatedAt() == null) {
                session.setCreatedAt(now);
            }
            session.setSessionId(sessionId);
            session.setHospitalId(hospitalId);
            AdminPrincipal terminal = TerminalSecurityContext.get();
            if (terminal != null) {
                session.setTerminalUsername(terminal.username());
            }
            session.setMode(mode.name());
            session.setConsentToStoreHistory(consent);
            session.setUpdatedAt(now);
            sessionRepository.save(session);

            List<ChatMessageDocument> messageDocuments = new ArrayList<>();
            for (ChatMessageInput input : messages) {
                ChatMessageDocument messageDocument = new ChatMessageDocument();
                messageDocument.setHospitalId(hospitalId);
                messageDocument.setSessionId(sessionId);
                messageDocument.setRole(input.role());
                messageDocument.setContent(input.content());
                messageDocument.setCreatedAt(now);
                messageDocuments.add(messageDocument);
            }

            ChatMessageDocument assistantMessage = new ChatMessageDocument();
            assistantMessage.setHospitalId(hospitalId);
            assistantMessage.setSessionId(sessionId);
            assistantMessage.setRole("assistant");
            assistantMessage.setContent(assistantReply);
            assistantMessage.setCreatedAt(now);
            messageDocuments.add(assistantMessage);
            messageRepository.saveAll(messageDocuments);

            if (promptContext != null) {
                PromptTraceDocument promptTrace = new PromptTraceDocument();
                promptTrace.setHospitalId(hospitalId);
                promptTrace.setSessionId(sessionId);
                promptTrace.setMode(mode.name());
                promptTrace.setPromptContent(String.join("\n\n",
                        promptContext.systemPrompt(),
                        promptContext.businessPrompt(),
                        promptContext.ragPrompt(),
                        promptContext.toolPrompt()));
                promptTrace.setCreatedAt(now);
                promptTraceRepository.save(promptTrace);
            }
        } catch (Exception ex) {
            log.warn("Failed to store chat history, fallback to logs only: {}", ex.getMessage());
        }
    }

    public void storeToolTrace(String sessionId, String toolName, Map<String, Object> arguments, Object result) {
        try {
            ToolTraceDocument toolTrace = new ToolTraceDocument();
            toolTrace.setHospitalId(currentHospitalId());
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

    private String sessionTitle(String hospitalId, String sessionId) {
        return messageRepository.findByHospitalIdAndSessionIdOrderByCreatedAtAsc(hospitalId, sessionId).stream()
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

    private String currentHospitalId() {
        String hospitalId = TenantContext.getHospitalId();
        return hospitalId == null || hospitalId.isBlank() ? properties.getDefaultHospitalId() : hospitalId;
    }
}
