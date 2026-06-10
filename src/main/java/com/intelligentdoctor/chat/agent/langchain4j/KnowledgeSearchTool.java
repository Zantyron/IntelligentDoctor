package com.intelligentdoctor.chat.agent.langchain4j;

import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.chat.history.ChatHistoryService;
import com.intelligentdoctor.knowledge.service.KnowledgeSearchService;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class KnowledgeSearchTool {

    private final KnowledgeSearchService knowledgeSearchService;
    private final ChatHistoryService chatHistoryService;

    public KnowledgeSearchTool(KnowledgeSearchService knowledgeSearchService,
                               ChatHistoryService chatHistoryService) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.chatHistoryService = chatHistoryService;
    }

    @Tool("检索医院知识库，返回与症状、科室、医生或挂号规则相关的 RAG 证据")
    public List<KnowledgeSnippet> searchMedicalKnowledge(
            @P("会话 ID") String sessionId,
            @P("医院 ID") String hospitalId,
            @P("用户症状、科室或挂号规则问题") String query) {
        List<KnowledgeSnippet> snippets = knowledgeSearchService.search(hospitalId, query, 5);
        chatHistoryService.storeToolTrace(sessionId, "langchain4jKnowledgeSearch", Map.of(
                "hospitalId", hospitalId,
                "query", query,
                "runtime", "langchain4j"
        ), Map.of(
                "snippets", snippets,
                "trace", knowledgeSearchService.explainSearch(query, snippets)
        ));
        return snippets;
    }
}
