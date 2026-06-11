package com.intelligentdoctor.chat.service;

import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.ai.tools.AgentToolService;
import com.intelligentdoctor.catalog.entity.DepartmentEntity;
import com.intelligentdoctor.catalog.service.CatalogQueryService;
import com.intelligentdoctor.chat.agent.TriageAgentRequest;
import com.intelligentdoctor.chat.agent.TriageAgentRuntimeSelector;
import com.intelligentdoctor.chat.dto.ChatStreamRequest;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.common.SsePayload;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.registration.dto.CreateDraftCommand;
import com.intelligentdoctor.registration.dto.RegistrationDraftView;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;

@Service
public class ChatOrchestratorService {

    private static final ZoneId HOSPITAL_ZONE = ZoneId.of("Asia/Shanghai");

    private final AppProperties properties;
    private final CatalogQueryService catalogQueryService;
    private final AgentToolService agentToolService;
    private final ChatHistoryService chatHistoryService;
    private final TriageAgentRuntimeSelector runtimeSelector;
    private final Executor executor;

    public ChatOrchestratorService(AppProperties properties,
                                   CatalogQueryService catalogQueryService,
                                   AgentToolService agentToolService,
                                   ChatHistoryService chatHistoryService,
                                   TriageAgentRuntimeSelector runtimeSelector,
                                   Executor executor) {
        this.properties = properties;
        this.catalogQueryService = catalogQueryService;
        this.agentToolService = agentToolService;
        this.chatHistoryService = chatHistoryService;
        this.runtimeSelector = runtimeSelector;
        this.executor = executor;
    }

    public SseEmitter stream(ChatMode mode, ChatStreamRequest request) {
        SseEmitter emitter = new SseEmitter(properties.getStream().getTimeoutMillis());
        emitter.onTimeout(emitter::complete);
        emitter.onError(error -> emitter.complete());
        executor.execute(() -> {
            try {
                String hospitalId = resolveHospitalId(request.hospitalId());
                ZonedDateTime requestTime = ZonedDateTime.now(HOSPITAL_ZONE);
                emitPayload(emitter, "meta", "开始生成导诊结果", Map.of(
                        "mode", mode.name(),
                        "hospitalId", hospitalId,
                        "agentRuntime", properties.getAgent().getRuntime(),
                        "requestTime", requestTime.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)
                ));

                StringBuilder streamedReply = new StringBuilder();
                ChatStreamResult result = runtimeSelector.select().run(new TriageAgentRequest(
                        mode,
                        request.sessionId(),
                        hospitalId,
                        request.consentToStoreHistory(),
                        requestTime,
                        request.messages()
                ), token -> {
                    try {
                        streamedReply.append(token);
                        emitPayload(emitter, "chunk", token, Map.of());
                    } catch (IOException ex) {
                        throw new IllegalStateException("Failed to stream AI token", ex);
                    }
                });

                List<Map<String, Object>> appointmentOptions = mode == ChatMode.REGISTRATION
                        ? buildAppointmentOptions(request.sessionId(), hospitalId, result, requestTime)
                        : List.of();
                RegistrationDraftView autoDraft = mode == ChatMode.REGISTRATION
                        ? createDraftFromRecommendation(request.sessionId(), hospitalId, result, appointmentOptions)
                        : null;
                ChatStreamResult enriched = enrichResult(result, autoDraft, appointmentOptions);

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
                        request.messages(), enriched.reply(), null);
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

    private ChatStreamResult enrichResult(ChatStreamResult result,
                                          RegistrationDraftView draftView,
                                          List<Map<String, Object>> appointmentOptions) {
        Map<String, Object> metadata = new HashMap<>(result.metadata());
        metadata.put("appointmentOptions", appointmentOptions);
        if (draftView == null) {
            return new ChatStreamResult(
                    result.reply(),
                    result.summary(),
                    result.possibleConditions(),
                    result.recommendations(),
                    result.evidence(),
                    result.functionSuggestions(),
                    metadata
            );
        }
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

    private List<Map<String, Object>> buildAppointmentOptions(String sessionId,
                                                              String hospitalId,
                                                              ChatStreamResult result,
                                                              ZonedDateTime requestTime) {
        TriageAnalysis analysis = toAnalysis(result);
        List<Map<String, Object>> options = new ArrayList<>();
        for (String name : new LinkedHashSet<>(analysis.suggestedDepartments())) {
            DepartmentEntity department = catalogQueryService.resolveDepartmentByName(hospitalId, name);
            if (department == null) {
                continue;
            }
            var clinics = agentToolService.searchClinics(sessionId, hospitalId, department.getId());
            for (var clinic : clinics) {
                var doctors = agentToolService.searchDoctors(sessionId, hospitalId, department.getId(), clinic.id());
                for (var doctor : doctors) {
                    var schedules = agentToolService.querySchedules(sessionId, hospitalId, department.getId(), doctor.id());
                    for (var schedule : schedules) {
                        if (schedule.stockAvailable() <= 0 || !isBookableAfterNow(schedule.slotDate(), schedule.period(), requestTime)) {
                            continue;
                        }
                        Map<String, Object> option = new HashMap<>();
                        option.put("departmentId", department.getId());
                        option.put("departmentName", department.getName());
                        option.put("clinicRoomId", clinic.id());
                        option.put("clinicName", clinic.name());
                        option.put("clinicLocation", clinic.location());
                        option.put("doctorId", doctor.id());
                        option.put("doctorName", doctor.name());
                        option.put("doctorTitle", doctor.title());
                        option.put("specialty", doctor.specialty());
                        option.put("consultationFee", doctor.consultationFee());
                        option.put("slotId", schedule.id());
                        option.put("slotDate", schedule.slotDate());
                        option.put("period", schedule.period());
                        option.put("stockAvailable", schedule.stockAvailable());
                        option.put("hotExpert", doctor.hotExpert());
                        option.put("hotSlot", schedule.hotSlot());
                        options.add(option);
                    }
                }
            }
        }
        return options.stream()
                .sorted(Comparator
                        .comparing((Map<String, Object> option) -> (LocalDate) option.get("slotDate"))
                        .thenComparingInt(option -> periodRank(String.valueOf(option.get("period")))))
                .limit(12)
                .toList();
    }

    private boolean isBookableAfterNow(LocalDate slotDate, String period, ZonedDateTime requestTime) {
        LocalDate today = requestTime.toLocalDate();
        if (slotDate.isAfter(today)) {
            return true;
        }
        if (slotDate.isBefore(today)) {
            return false;
        }
        // Period data is coarse, so keep the current period until its likely end time.
        return periodEnd(period).isAfter(requestTime.toLocalTime());
    }

    private LocalTime periodEnd(String period) {
        String value = period == null ? "" : period;
        if (value.contains("上午") || value.toLowerCase().contains("morning")) {
            return LocalTime.NOON;
        }
        if (value.contains("下午") || value.toLowerCase().contains("afternoon")) {
            return LocalTime.of(18, 0);
        }
        if (value.contains("晚上") || value.contains("夜") || value.toLowerCase().contains("evening")) {
            return LocalTime.of(23, 59);
        }
        return LocalTime.MAX;
    }

    private int periodRank(String period) {
        String value = period == null ? "" : period;
        if (value.contains("上午") || value.toLowerCase().contains("morning")) {
            return 1;
        }
        if (value.contains("下午") || value.toLowerCase().contains("afternoon")) {
            return 2;
        }
        if (value.contains("晚上") || value.contains("夜") || value.toLowerCase().contains("evening")) {
            return 3;
        }
        return 9;
    }

    private RegistrationDraftView createDraftFromRecommendation(String sessionId,
                                                               String hospitalId,
                                                               ChatStreamResult result,
                                                               List<Map<String, Object>> appointmentOptions) {
        if (!appointmentOptions.isEmpty()) {
            Map<String, Object> option = appointmentOptions.get(0);
            return agentToolService.createRegistrationDraft(sessionId, new CreateDraftCommand(
                    hospitalId,
                    sessionId,
                    result.summary(),
                    String.valueOf(option.get("departmentId")),
                    String.valueOf(option.get("clinicRoomId")),
                    String.valueOf(option.get("doctorId")),
                    String.valueOf(option.get("slotId")),
                    (LocalDate) option.get("slotDate"),
                    String.valueOf(option.get("period")),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null
            ));
        }
        TriageAnalysis analysis = toAnalysis(result);
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
        var schedules = agentToolService.querySchedules(sessionId, hospitalId, department.getId(), doctors.get(0).id()).stream()
                .filter(schedule -> schedule.stockAvailable() > 0)
                .filter(schedule -> isBookableAfterNow(schedule.slotDate(), schedule.period(), ZonedDateTime.now(HOSPITAL_ZONE)))
                .sorted(Comparator
                        .comparing(com.intelligentdoctor.catalog.dto.ScheduleSlotView::slotDate)
                        .thenComparingInt(schedule -> periodRank(schedule.period())))
                .toList();
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

    private TriageAnalysis toAnalysis(ChatStreamResult result) {
        List<String> departments = result.recommendations().stream()
                .filter(recommendation -> "department".equalsIgnoreCase(recommendation.type()))
                .map(recommendation -> recommendation.title())
                .toList();
        return new TriageAnalysis(
                result.summary(),
                result.possibleConditions(),
                String.valueOf(result.metadata().getOrDefault("urgencyLevel", "MEDIUM")),
                departments,
                List.of(),
                Map.of()
        );
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
