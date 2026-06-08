package com.intelligentdoctor.chat.service;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.ai.prompt.PromptTemplateService;
import com.intelligentdoctor.ai.provider.AiGateway;
import com.intelligentdoctor.ai.tools.AgentToolService;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.catalog.entity.DepartmentEntity;
import com.intelligentdoctor.catalog.service.CatalogQueryService;
import com.intelligentdoctor.chat.dto.ChatStreamRequest;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.common.SsePayload;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import com.intelligentdoctor.registration.dto.CreateDraftCommand;
import com.intelligentdoctor.registration.dto.RegistrationDraftView;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
public class ChatOrchestratorService {

    private final AppProperties properties;
    private final AiGateway aiGateway;
    private final PromptTemplateService promptTemplateService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final CatalogQueryService catalogQueryService;
    private final AgentToolService agentToolService;
    private final ChatHistoryService chatHistoryService;
    private final Executor executor;

    public ChatOrchestratorService(AppProperties properties,
                                   AiGateway aiGateway,
                                   PromptTemplateService promptTemplateService,
                                   KnowledgeSearchService knowledgeSearchService,
                                   CatalogQueryService catalogQueryService,
                                   AgentToolService agentToolService,
                                   ChatHistoryService chatHistoryService,
                                   Executor executor) {
        this.properties = properties;
        this.aiGateway = aiGateway;
        this.promptTemplateService = promptTemplateService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.catalogQueryService = catalogQueryService;
        this.agentToolService = agentToolService;
        this.chatHistoryService = chatHistoryService;
        this.executor = executor;
    }

    public SseEmitter stream(ChatMode mode, ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(properties.getStream().getTimeoutMillis());
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitter.complete());
        executor.execute(() -> {
            try {
                String hospitalId = resolveHospitalId(request.hospitalId());
                List<ChatMessageInput> modelMessages = chatHistoryService.mergeWithStoredHistory(
                        request.sessionId(), request.messages());
                TriageAnalysis analysis = aiGateway.analyze(mode, modelMessages);
                List<KnowledgeSnippet> snippets = knowledgeSearchService.search(hospitalId, analysis.symptomSummary(), 5);
                if (request.consentToStoreHistory()) {
                    chatHistoryService.storeToolTrace(request.sessionId(), "ragSearch", Map.of(
                            "hospitalId", hospitalId,
                            "mode", mode.name(),
                            "query", analysis.symptomSummary(),
                            "limit", 5,
                            "pipeline", "queryProcessing -> vector coarse retrieval -> lexical rerank -> prompt augmentation"
                    ), Map.of(
                            "snippets", snippets,
                            "trace", knowledgeSearchService.explainSearch(analysis.symptomSummary(), snippets)
                    ));
                }

                List<String> evidence = snippets.stream().map(KnowledgeSnippet::text).toList();
                AiPromptContext promptContext = promptTemplateService.build(mode, evidence);
                RegistrationDraftView autoDraft = mode == ChatMode.REGISTRATION
                        ? createDraftFromRecommendation(request.sessionId(), hospitalId, analysis)
                        : null;
                emitPayload(emitter, "meta", "开始生成导诊结果", Map.of("mode", mode.name(), "hospitalId", hospitalId));
                StringBuilder streamedReply = new StringBuilder();
                ChatStreamResult result = aiGateway.composeReplyStreaming(mode, analysis, snippets, promptContext, modelMessages, token -> {
                    try {
                        streamedReply.append(token);
                        emitPayload(emitter, "chunk", token, Map.of());
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed to stream AI token", ex);
                    }
                });
                ChatStreamResult enriched = enrichResult(result, autoDraft);

                streamText(emitter, remainingText(enriched.reply(), streamedReply));
                emitPayload(emitter, "result", "", Map.of(
                        "summary", enriched.summary(),
                        "possibleConditions", enriched.possibleConditions(),
                        "recommendations", enriched.recommendations(),
                        "evidence", enriched.evidence(),
                        "functionSuggestions", enriched.functionSuggestions(),
                        "metadata", enriched.metadata()
                ));
                chatHistoryService.storeChat(request.sessionId(), hospitalId, mode, request.consentToStoreHistory(),
                        request.messages(), enriched.reply(), promptContext);
                emitter.complete();
            } catch (Exception ex) {
                try {
                    emitPayload(emitter, "error", failureMessage(ex), Map.of());
                } catch (IOException ignored) {
                }
                emitter.complete();
            }
        });
        return emitter;
    }

    private ChatStreamResult enrichResult(ChatStreamResult result, RegistrationDraftView draftView) {
        if (draftView == null) {
            return result;
        }
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        Map<String, Object> draft = new HashMap<>();
        draft.put("draftId", draftView.draftId());
        draft.put("departmentId", draftView.departmentId());
        draft.put("clinicRoomId", draftView.clinicRoomId());
        draft.put("doctorId", draftView.doctorId());
        draft.put("slotId", draftView.slotId());
        draft.put("visitDate", draftView.visitDate());
        draft.put("visitPeriod", draftView.visitPeriod());
        draft.put("patientName", draftView.patientName() == null ? "" : draftView.patientName());
        draft.put("patientPhone", draftView.patientPhone() == null ? "" : draftView.patientPhone());
        draft.put("idCard", draftView.idCard() == null ? "" : draftView.idCard());
        draft.put("gender", draftView.gender() == null ? "" : draftView.gender());
        draft.put("age", draftView.age() == null ? "" : draftView.age());
        draft.put("status", draftView.status());
        metadata.put("draft", draft);
        return new ChatStreamResult(
                result.reply() + "\n\n已为你生成挂号草稿，草稿编号: " + draftView.draftId() + "。",
                result.summary(),
                result.possibleConditions(),
                result.recommendations(),
                result.evidence(),
                result.functionSuggestions(),
                metadata
        );
    }

    private RegistrationDraftView createDraftFromRecommendation(String sessionId, String hospitalId, TriageAnalysis analysis) {
        DepartmentEntity department = null;
        for (String name : analysis.suggestedDepartments()) {
            department = catalogQueryService.resolveDepartmentByName(hospitalId, name);
            if (department != null) {
                break;
            }
        }
        if (department == null) {
            return null;
        }
        var clinics = agentToolService.searchClinics(sessionId, hospitalId, department.getId());
        if (clinics.isEmpty()) {
            return null;
        }
        var doctors = agentToolService.searchDoctors(sessionId, hospitalId, department.getId(), clinics.get(0).id());
        if (doctors.isEmpty()) {
            return null;
        }
        var schedules = agentToolService.querySchedules(sessionId, hospitalId, department.getId(), doctors.get(0).id());
        if (schedules.isEmpty()) {
            return null;
        }
        return agentToolService.createRegistrationDraft(sessionId, new CreateDraftCommand(
                hospitalId,
                sessionId,
                analysis.symptomSummary(),
                department.getId(),
                clinics.get(0).id(),
                doctors.get(0).id(),
                schedules.get(0).id(),
                schedules.get(0).slotDate(),
                schedules.get(0).period(),
                null,
                null,
                null,
                null,
                null,
                null
        ));
    }

    private void streamText(SseEmitter emitter, String text) throws IOException, InterruptedException {
        if (text == null || text.isBlank()) {
            return;
        }
        int chunkSize = properties.getStream().getChunkSize();
        for (int start = 0; start < text.length(); start += chunkSize) {
            int end = Math.min(text.length(), start + chunkSize);
            emitPayload(emitter, "chunk", text.substring(start, end), Map.of());
            Thread.sleep(properties.getStream().getChunkDelayMillis());
        }
    }

    private String remainingText(String fullText, StringBuilder streamedReply) {
        if (fullText == null || fullText.isBlank()) {
            return "";
        }
        String streamed = streamedReply.toString();
        if (streamed.isBlank()) {
            return fullText;
        }
        if (fullText.startsWith(streamed)) {
            return fullText.substring(streamed.length());
        }
        return "";
    }

    private void emitPayload(SseEmitter emitter, String type, String content, Map<String, Object> metadata) throws IOException {
        emitter.send(SseEmitter.event().name(type).data(new SsePayload(type, content, metadata)));
    }

    private String resolveHospitalId(String hospitalId) {
        return hospitalId == null || hospitalId.isBlank() ? properties.getDefaultHospitalId() : hospitalId;
    }

    private String failureMessage(Exception ex) {
        return "AI 服务暂时不可用: " + ex.getMessage() + "。请稍后重试；如症状明显加重，请优先线下就医。";
    }
}
