package com.intelligentdoctor.chat.agent.langchain4j;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.ai.dto.FunctionSuggestion;
import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.dto.RecommendationCard;
import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.ai.prompt.PromptTemplateService;
import com.intelligentdoctor.ai.provider.AiGateway;
import com.intelligentdoctor.ai.tools.AgentToolService;
import com.intelligentdoctor.chat.agent.TriageAgentRequest;
import com.intelligentdoctor.chat.agent.TriageAgentRuntime;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.config.AppProperties;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.tool.BeforeToolExecution;
import dev.langchain4j.service.tool.ToolExecution;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

@Component
public class LangChain4jTriageAgentRuntime implements TriageAgentRuntime {

    private final AppProperties properties;
    private final AiGateway aiGateway;
    private final PromptTemplateService promptTemplateService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ChatHistoryService chatHistoryService;
    private final JsonUtils jsonUtils;
    private final LangChain4jTriageAgent agent;

    public LangChain4jTriageAgentRuntime(AppProperties properties,
                                         AiGateway aiGateway,
                                         PromptTemplateService promptTemplateService,
                                         KnowledgeSearchService knowledgeSearchService,
                                         ChatHistoryService chatHistoryService,
                                         JsonUtils jsonUtils,
                                         AgentToolService agentToolService,
                                         KnowledgeSearchTool knowledgeSearchTool) {
        this.properties = properties;
        this.aiGateway = aiGateway;
        this.promptTemplateService = promptTemplateService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.chatHistoryService = chatHistoryService;
        this.jsonUtils = jsonUtils;
        this.agent = buildAgent(agentToolService, knowledgeSearchTool);
    }

    @Override
    public boolean supports(String runtime) {
        return "langchain4j".equalsIgnoreCase(runtime);
    }

    @Override
    public ChatStreamResult run(TriageAgentRequest request, Consumer<String> tokenConsumer) {
        List<ChatMessageInput> modelMessages = chatHistoryService.mergeWithStoredHistory(
                request.sessionId(), request.messages());
        TriageAnalysis analysis = aiGateway.analyze(request.mode(), modelMessages);
        List<KnowledgeSnippet> snippets = knowledgeSearchService.search(request.hospitalId(), analysis.symptomSummary(), 5);
        if (request.consentToStoreHistory()) {
            chatHistoryService.storeToolTrace(request.sessionId(), "langchain4jRagPrefetch", Map.of(
                    "hospitalId", request.hospitalId(),
                    "mode", request.mode().name(),
                    "query", analysis.symptomSummary(),
                    "runtime", "langchain4j"
            ), Map.of(
                    "snippets", snippets,
                    "trace", knowledgeSearchService.explainSearch(analysis.symptomSummary(), snippets)
            ));
        }

        List<String> evidence = snippets.stream().map(KnowledgeSnippet::text).toList();
        AiPromptContext promptContext = promptTemplateService.build(request.mode(), evidence, request.requestTime());
        StringBuilder reply = new StringBuilder();
        Throwable error = runAgentStream(request, modelMessages, analysis, promptContext, token -> {
            reply.append(token);
            tokenConsumer.accept(token);
        });

        if (reply.isEmpty()) {
            ChatStreamResult fallback = aiGateway.composeReplyStreaming(
                    request.mode(), analysis, snippets, promptContext, modelMessages, tokenConsumer);
            return addRuntimeMetadata(fallback, "langchain4j-fallback", error);
        }

        return new ChatStreamResult(
                reply.toString(),
                analysis.symptomSummary(),
                analysis.possibleConditions(),
                request.mode() == ChatMode.REGISTRATION ? buildRecommendationCards(analysis, snippets) : List.of(),
                evidenceWithScore(snippets),
                request.mode() == ChatMode.REGISTRATION ? functionSuggestions(analysis) : List.of(),
                metadata(request, snippets, error)
        );
    }

    private LangChain4jTriageAgent buildAgent(AgentToolService agentToolService, KnowledgeSearchTool knowledgeSearchTool) {
        OpenAiChatModel chatModel = OpenAiChatModel.builder()
                .baseUrl(properties.getAi().getBaseUrl())
                .apiKey(properties.getAi().resolvedApiKey())
                .modelName(properties.getAi().getChatModel())
                .temperature(0.2)
                .maxRetries(2)
                .timeout(Duration.ofSeconds(60))
                .build();
        OpenAiStreamingChatModel streamingChatModel = OpenAiStreamingChatModel.builder()
                .baseUrl(properties.getAi().getBaseUrl())
                .apiKey(properties.getAi().resolvedApiKey())
                .modelName(properties.getAi().getChatModel())
                .temperature(0.2)
                .timeout(Duration.ofSeconds(90))
                .build();

        return AiServices.builder(LangChain4jTriageAgent.class)
                .chatModel(chatModel)
                .streamingChatModel(streamingChatModel)
                .chatMemoryProvider(memoryId -> MessageWindowChatMemory.builder()
                        .id(memoryId)
                        .maxMessages(properties.getAgent().getMemoryMaxMessages())
                        .build())
                .tools(agentToolService, knowledgeSearchTool)
                .maxToolCallingRoundTrips(properties.getAgent().getMaxToolRoundTrips())
                .beforeToolExecution(this::traceBeforeToolExecution)
                .afterToolExecution(this::traceAfterToolExecution)
                .build();
    }

    private Throwable runAgentStream(TriageAgentRequest request,
                                     List<ChatMessageInput> modelMessages,
                                     TriageAnalysis analysis,
                                     AiPromptContext promptContext,
                                     Consumer<String> tokenConsumer) {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> errorRef = new AtomicReference<>();
        TokenStream stream = agent.chat(
                request.sessionId(),
                request.mode().name(),
                request.hospitalId(),
                conversation(modelMessages),
                promptContext.ragPrompt(),
                jsonUtils.toJson(Map.of(
                        "symptomSummary", analysis.symptomSummary(),
                        "possibleConditions", analysis.possibleConditions(),
                        "urgencyLevel", analysis.urgencyLevel(),
                        "suggestedDepartments", analysis.suggestedDepartments(),
                        "requestTime", requestTimeText(request),
                        "registrationTimeRule", "Only recommend appointment slots later than requestTime.",
                        "cautionNotes", analysis.cautionNotes(),
                        "extractedSlots", analysis.extractedSlots()
                )),
                latestUserText(modelMessages)
        );
        stream.onPartialResponse(tokenConsumer)
                .onToolExecuted(tool -> {
                })
                .onCompleteResponse(response -> done.countDown())
                .onError(error -> {
                    errorRef.set(error);
                    done.countDown();
                })
                .start();
        try {
            done.await();
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            errorRef.set(ex);
        }
        return errorRef.get();
    }

    private void traceBeforeToolExecution(BeforeToolExecution execution) {
        chatHistoryService.storeToolTrace("langchain4j-runtime", "langchain4j.beforeToolExecution", Map.of(
                "request", execution.request()
        ), Map.of("runtime", "langchain4j"));
    }

    private void traceAfterToolExecution(ToolExecution execution) {
        chatHistoryService.storeToolTrace("langchain4j-runtime", "langchain4j.afterToolExecution", Map.of(
                "request", execution.request()
        ), Map.of(
                "result", execution.result(),
                "runtime", "langchain4j"
        ));
    }

    private ChatStreamResult addRuntimeMetadata(ChatStreamResult result, String runtime, Throwable error) {
        Map<String, Object> metadata = new java.util.HashMap<>(result.metadata());
        metadata.put("agentRuntime", runtime);
        if (error != null) {
            metadata.put("langchain4jError", error.getClass().getSimpleName() + ": " + error.getMessage());
        }
        return new ChatStreamResult(result.reply(), result.summary(), result.possibleConditions(),
                result.recommendations(), result.evidence(), result.functionSuggestions(), metadata);
    }

    private Map<String, Object> metadata(TriageAgentRequest request, List<KnowledgeSnippet> snippets, Throwable error) {
        Map<String, Object> metadata = new java.util.HashMap<>();
        metadata.put("mode", request.mode().name());
        metadata.put("agentRuntime", "langchain4j");
        metadata.put("model", properties.getAi().getChatModel());
        metadata.put("evidenceCount", snippets.size());
        metadata.put("toolCalling", "@Tool AgentToolService + KnowledgeSearchTool");
        metadata.put("requestTime", requestTimeText(request));
        if (error != null) {
            metadata.put("langchain4jError", error.getClass().getSimpleName() + ": " + error.getMessage());
        }
        return metadata;
    }

    private String requestTimeText(TriageAgentRequest request) {
        if (request.requestTime() == null) {
            return "";
        }
        return request.requestTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " (" + request.requestTime().getZone() + ")";
    }

    private List<String> evidenceWithScore(List<KnowledgeSnippet> snippets) {
        return snippets.stream()
                .map(snippet -> "%s(%.3f): %s".formatted(snippet.sourceName(), snippet.score(), snippet.text()))
                .toList();
    }

    private List<RecommendationCard> buildRecommendationCards(TriageAnalysis analysis, List<KnowledgeSnippet> snippets) {
        String reason = snippets.isEmpty()
                ? "依据患者主诉和 LangChain4j Agent 分诊结果推荐。"
                : "依据患者主诉、LangChain4j Agent 工具调用和医院知识库证据推荐。";
        List<RecommendationCard> cards = new ArrayList<>();
        int order = 0;
        for (String department : new LinkedHashSet<>(analysis.suggestedDepartments())) {
            cards.add(new RecommendationCard(
                    "department",
                    "dept-" + order++,
                    department,
                    "推荐科室",
                    "优先匹配该科室下有排班和号源的医生。",
                    reason
            ));
        }
        return cards;
    }

    private List<FunctionSuggestion> functionSuggestions(TriageAnalysis analysis) {
        return List.of(new FunctionSuggestion(
                "langchain4jFunctionCalling",
                "LangChain4j @Tool 已封装科室、诊室、医生、排班、挂号规则、挂号草稿和知识库检索工具",
                Map.of(
                        "symptomSummary", analysis.symptomSummary(),
                        "suggestedDepartments", analysis.suggestedDepartments()
                ),
                false
        ));
    }

    private String conversation(List<ChatMessageInput> messages) {
        return messages.stream()
                .map(message -> message.role() + ": " + message.content())
                .reduce((left, right) -> left + "\n" + right)
                .orElse("");
    }

    private String latestUserText(List<ChatMessageInput> messages) {
        return messages.stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .reduce((first, second) -> second)
                .map(ChatMessageInput::content)
                .orElse("");
    }
}
