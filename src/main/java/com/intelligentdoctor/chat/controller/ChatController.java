package com.intelligentdoctor.chat.controller;

import com.intelligentdoctor.chat.dto.ChatMessageView;
import com.intelligentdoctor.chat.dto.ChatSessionView;
import com.intelligentdoctor.chat.dto.ChatStreamRequest;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.chat.service.ChatOrchestratorService;
import com.intelligentdoctor.common.ApiResponse;
import com.intelligentdoctor.registration.service.RegistrationService;
import com.intelligentdoctor.tenant.TenantContext;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatOrchestratorService chatOrchestratorService;
    private final ChatHistoryService chatHistoryService;
    private final RegistrationService registrationService;

    public ChatController(ChatOrchestratorService chatOrchestratorService,
                          ChatHistoryService chatHistoryService,
                          RegistrationService registrationService) {
        this.chatOrchestratorService = chatOrchestratorService;
        this.chatHistoryService = chatHistoryService;
        this.registrationService = registrationService;
    }

    @PostMapping(path = "/diagnosis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnosis(@Valid @RequestBody ChatStreamRequest request) {
        return chatOrchestratorService.stream(ChatMode.DIAGNOSIS, request);
    }

    @PostMapping(path = "/registration/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter registration(@Valid @RequestBody ChatStreamRequest request) {
        return chatOrchestratorService.stream(ChatMode.REGISTRATION, request);
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionView>> sessions() {
        return ApiResponse.success(chatHistoryService.listSessions(TenantContext.requireHospitalId()));
    }

    @GetMapping("/messages")
    public ApiResponse<List<ChatMessageView>> messages(@RequestParam String sessionId) {
        return ApiResponse.success(chatHistoryService.listMessages(TenantContext.requireHospitalId(), sessionId));
    }

    @DeleteMapping("/messages")
    public ApiResponse<Void> deleteMessage(@RequestParam String messageId) {
        chatHistoryService.deleteMessage(TenantContext.requireHospitalId(), messageId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/sessions")
    public ApiResponse<Void> deleteSession(@RequestParam String sessionId) {
        String hospitalId = TenantContext.requireHospitalId();
        chatHistoryService.deleteSession(hospitalId, sessionId);
        registrationService.deleteUnconfirmedDraftsBySession(hospitalId, sessionId);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/sessions/all")
    public ApiResponse<Void> deleteAllSessions() {
        String hospitalId = TenantContext.requireHospitalId();
        for (ChatSessionView session : chatHistoryService.listSessions(hospitalId)) {
            registrationService.deleteUnconfirmedDraftsBySession(hospitalId, session.sessionId());
        }
        chatHistoryService.deleteAllSessions(hospitalId);
        return ApiResponse.success(null);
    }
}
