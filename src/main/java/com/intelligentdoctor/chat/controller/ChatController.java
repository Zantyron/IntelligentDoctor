package com.intelligentdoctor.chat.controller;

import com.intelligentdoctor.chat.dto.ChatStreamRequest;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.chat.service.ChatOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatOrchestratorService chatOrchestratorService;

    public ChatController(ChatOrchestratorService chatOrchestratorService) {
        this.chatOrchestratorService = chatOrchestratorService;
    }

    @PostMapping(path = "/diagnosis/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter diagnosis(@Valid @RequestBody ChatStreamRequest request) {
        return chatOrchestratorService.stream(ChatMode.DIAGNOSIS, request);
    }

    @PostMapping(path = "/registration/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter registration(@Valid @RequestBody ChatStreamRequest request) {
        return chatOrchestratorService.stream(ChatMode.REGISTRATION, request);
    }
}
