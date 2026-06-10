package com.intelligentdoctor.chat.agent;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.ai.prompt.PromptTemplateService;
import com.intelligentdoctor.ai.provider.AiGateway;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
public class NativeTriageAgentRuntime implements TriageAgentRuntime {

    private final AiGateway aiGateway;
    private final PromptTemplateService promptTemplateService;
    private final KnowledgeSearchService knowledgeSearchService;
    private final ChatHistoryService chatHistoryService;

    public NativeTriageAgentRuntime(AiGateway aiGateway,
                                    PromptTemplateService promptTemplateService,
                                    KnowledgeSearchService knowledgeSearchService,
                                    ChatHistoryService chatHistoryService) {
        this.aiGateway = aiGateway;
        this.promptTemplateService = promptTemplateService;
        this.knowledgeSearchService = knowledgeSearchService;
        this.chatHistoryService = chatHistoryService;
    }

    @Override
    public boolean supports(String runtime) {
        return runtime == null || runtime.isBlank() || "native".equalsIgnoreCase(runtime);
    }

    @Override
    public ChatStreamResult run(TriageAgentRequest request, Consumer<String> tokenConsumer) {
        List<ChatMessageInput> modelMessages = chatHistoryService.mergeWithStoredHistory(
                request.sessionId(), request.messages());
        TriageAnalysis analysis = aiGateway.analyze(request.mode(), modelMessages);
        List<KnowledgeSnippet> snippets = knowledgeSearchService.search(request.hospitalId(), analysis.symptomSummary(), 5);
        if (request.consentToStoreHistory()) {
            chatHistoryService.storeToolTrace(request.sessionId(), "ragSearch", Map.of(
                    "hospitalId", request.hospitalId(),
                    "mode", request.mode().name(),
                    "query", analysis.symptomSummary(),
                    "limit", 5,
                    "pipeline", "queryProcessing -> vector coarse retrieval -> lexical rerank -> prompt augmentation"
            ), Map.of(
                    "snippets", snippets,
                    "trace", knowledgeSearchService.explainSearch(analysis.symptomSummary(), snippets)
            ));
        }

        List<String> evidence = snippets.stream().map(KnowledgeSnippet::text).toList();
        AiPromptContext promptContext = promptTemplateService.build(request.mode(), evidence);
        return aiGateway.composeReplyStreaming(
                request.mode(),
                analysis,
                snippets,
                promptContext,
                modelMessages,
                tokenConsumer
        );
    }
}
