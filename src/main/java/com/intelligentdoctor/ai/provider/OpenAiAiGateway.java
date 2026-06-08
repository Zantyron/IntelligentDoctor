package com.intelligentdoctor.ai.provider;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.ai.dto.FunctionSuggestion;
import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.dto.RecommendationCard;
import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.model.ChatMode;
import com.intelligentdoctor.common.JsonUtils;
import com.intelligentdoctor.config.AppProperties;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Component
@ConditionalOnProperty(prefix = "app.ai", name = "provider", havingValue = "openai", matchIfMissing = true)
public class OpenAiAiGateway implements AiGateway {

    private final AppProperties properties;
    private final JsonUtils jsonUtils;
    private final ChatModel chatModel;
    private final HttpClient httpClient;

    public OpenAiAiGateway(AppProperties properties, JsonUtils jsonUtils) {
        this.properties = properties;
        this.jsonUtils = jsonUtils;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(20))
                .build();
        this.chatModel = OpenAiChatModel.builder()
                .baseUrl(properties.getAi().getBaseUrl())
                .apiKey(properties.getAi().resolvedApiKey())
                .modelName(properties.getAi().getChatModel())
                .temperature(0.2)
                .maxRetries(2)
                .timeout(Duration.ofSeconds(60))
                .build();
    }

    @Override
    public TriageAnalysis analyze(ChatMode mode, List<ChatMessageInput> messages) {
        ensureConfigured();
        String latestUserText = latestUserText(messages);
        String instruction = """
                你是医院智能导诊系统的医学分诊分析器。请只做导诊和挂号建议，不要给出确诊或处方。
                你必须根据患者输入抽取结构化信息，并严格返回 JSON，不要返回 Markdown。
                JSON 字段：
                {
                  "symptomSummary": "用一句中文概括主诉和关键伴随症状",
                  "possibleConditions": ["可能方向，不超过4个"],
                  "urgencyLevel": "LOW|MEDIUM|HIGH",
                  "suggestedDepartments": ["推荐科室，不超过3个"],
                  "cautionNotes": ["风险提醒或线下就医建议"],
                  "extractedSlots": {"duration":"持续时间或未说明","temperature":"体温或未说明","redFlags":"危险信号或未说明"}
                }
                判断原则：
                1. 胸痛、呼吸困难、意识障碍、大出血、剧烈头痛、抽搐等优先标为 HIGH，并建议急诊。
                2. 信息不足时在 cautionNotes 中提出需要追问的信息。
                3. 挂号模式下 suggestedDepartments 要更具体，必要时包含急诊科。
                当前模式：%s。
                """.formatted(mode == ChatMode.DIAGNOSIS ? "诊断模式" : "挂号模式");

        List<ChatMessage> prompt = new ArrayList<>();
        prompt.add(SystemMessage.from(instruction));
        prompt.addAll(toLangChainMessages(messages));
        try {
            ChatResponse response = chatModel.chat(prompt);
            String content = response.aiMessage().text();
            AnalysisJson parsed = jsonUtils.fromJson(extractJson(content), AnalysisJson.class);
            return new TriageAnalysis(
                    firstText(parsed.symptomSummary(), latestUserText),
                    nonEmpty(parsed.possibleConditions(), List.of("需要结合更多病史进一步判断")),
                    firstText(parsed.urgencyLevel(), "MEDIUM").toUpperCase(),
                    nonEmpty(parsed.suggestedDepartments(), List.of("全科医学科")),
                    parsed.cautionNotes() == null ? List.of() : parsed.cautionNotes(),
                    parsed.extractedSlots() == null ? Map.of() : parsed.extractedSlots()
            );
        } catch (Exception ex) {
            return fallbackAnalysis(mode, latestUserText, ex);
        }
    }

    @Override
    public ChatStreamResult composeReply(ChatMode mode,
                                         TriageAnalysis analysis,
                                         List<KnowledgeSnippet> snippets,
                                         AiPromptContext promptContext,
                                         List<ChatMessageInput> messages) {
        return composeReplyInternal(mode, analysis, snippets, promptContext, messages, null);
    }

    @Override
    public ChatStreamResult composeReplyStreaming(ChatMode mode,
                                                  TriageAnalysis analysis,
                                                  List<KnowledgeSnippet> snippets,
                                                  AiPromptContext promptContext,
                                                  List<ChatMessageInput> messages,
                                                  Consumer<String> tokenConsumer) {
        return composeReplyInternal(mode, analysis, snippets, promptContext, messages, tokenConsumer);
    }

    private ChatStreamResult composeReplyInternal(ChatMode mode,
                                                  TriageAnalysis analysis,
                                                  List<KnowledgeSnippet> snippets,
                                                  AiPromptContext promptContext,
                                                  List<ChatMessageInput> messages,
                                                  Consumer<String> tokenConsumer) {
        ensureConfigured();
        List<String> evidence = snippets.stream()
                .map(snippet -> "%s（相似度 %.3f）：%s".formatted(snippet.sourceName(), snippet.score(), snippet.text()))
                .toList();

        String composePrompt = """
                %s

                %s

                %s

                %s

                已完成的结构化分诊分析：
                %s

                请面向患者输出中文回复：
                - 诊断模式：包含“病情归纳、可能方向、风险提醒、建议下一步、需要补充的问题”。
                - 挂号模式：必须包含“推荐科室/医生 + 推荐依据”，没有明确医生时说明将优先匹配有排班医生。
                - 如信息不足，先给出澄清追问，但仍提供谨慎的初步建议。
                - 失败或证据不足时明确说明信息有限，并建议线下就医或补充信息。
                - 不要声称已经确诊，不要开药方。
                """.formatted(
                promptContext.systemPrompt(),
                promptContext.businessPrompt(),
                promptContext.ragPrompt(),
                promptContext.toolPrompt(),
                jsonUtils.toJson(Map.of(
                        "symptomSummary", analysis.symptomSummary(),
                        "possibleConditions", analysis.possibleConditions(),
                        "urgencyLevel", analysis.urgencyLevel(),
                        "suggestedDepartments", analysis.suggestedDepartments(),
                        "cautionNotes", analysis.cautionNotes(),
                        "extractedSlots", analysis.extractedSlots()
                )));

        List<ChatMessage> prompt = new ArrayList<>();
        prompt.add(SystemMessage.from(composePrompt));
        prompt.addAll(toLangChainMessages(messages));
        String reply;
        Exception modelError = null;
        try {
            reply = tokenConsumer == null
                    ? chatModel.chat(prompt).aiMessage().text()
                    : streamChatCompletion(composePrompt, messages, tokenConsumer);
        } catch (Exception ex) {
            modelError = ex;
            try {
                reply = chatModel.chat(prompt).aiMessage().text();
                if (tokenConsumer != null) {
                    tokenConsumer.accept(reply);
                }
            } catch (Exception fallbackEx) {
                modelError = fallbackEx;
                reply = fallbackIfBlank("", mode);
                if (tokenConsumer != null) {
                    tokenConsumer.accept(reply);
                }
            }
        }

        List<RecommendationCard> cards = mode == ChatMode.REGISTRATION
                ? buildRecommendationCards(analysis, snippets)
                : List.of();
        List<FunctionSuggestion> functions = mode == ChatMode.REGISTRATION
                ? List.of(new FunctionSuggestion(
                "createRegistrationDraft",
                "创建待确认挂号草稿",
                Map.of(
                        "symptomSummary", analysis.symptomSummary(),
                        "suggestedDepartments", analysis.suggestedDepartments()
                ),
                false
        ))
                : List.of();

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("mode", mode.name());
        metadata.put("urgencyLevel", analysis.urgencyLevel());
        metadata.put("model", properties.getAi().getChatModel());
        metadata.put("evidenceCount", snippets.size());
        if (modelError != null) {
            metadata.put("modelError", modelError.getClass().getSimpleName() + ": " + modelError.getMessage());
        }

        return new ChatStreamResult(
                fallbackIfBlank(reply, mode),
                analysis.symptomSummary(),
                analysis.possibleConditions(),
                cards,
                evidence,
                functions,
                metadata
        );
    }

    private String streamChatCompletion(String systemPrompt,
                                        List<ChatMessageInput> messages,
                                        Consumer<String> tokenConsumer) throws IOException, InterruptedException {
        List<Map<String, String>> requestMessages = new ArrayList<>();
        requestMessages.add(Map.of("role", "system", "content", systemPrompt));
        for (ChatMessageInput message : messages) {
            String role = "assistant".equalsIgnoreCase(message.role()) ? "assistant" : "user";
            requestMessages.add(Map.of("role", role, "content", message.content()));
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("model", properties.getAi().getChatModel());
        payload.put("temperature", 0.2);
        payload.put("stream", true);
        payload.put("messages", requestMessages);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(normalizeBaseUrl(properties.getAi().getBaseUrl()) + "/chat/completions"))
                .timeout(Duration.ofSeconds(90))
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .header("Authorization", "Bearer " + properties.getAi().resolvedApiKey())
                .POST(HttpRequest.BodyPublishers.ofString(jsonUtils.toJson(payload)))
                .build();

        HttpResponse<java.io.InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Streaming chat completion failed: HTTP " + response.statusCode());
        }

        StringBuilder reply = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!line.startsWith("data:")) {
                    continue;
                }
                String data = line.substring(5).trim();
                if (data.isBlank() || "[DONE]".equals(data)) {
                    continue;
                }
                String token = extractStreamToken(data);
                if (!token.isEmpty()) {
                    reply.append(token);
                    tokenConsumer.accept(token);
                }
            }
        }
        return reply.toString();
    }

    private String extractStreamToken(String data) {
        Map<String, Object> event = jsonUtils.toMap(data);
        Object choicesValue = event.get("choices");
        if (!(choicesValue instanceof List<?> choices) || choices.isEmpty()) {
            return "";
        }
        Object firstChoice = choices.get(0);
        if (!(firstChoice instanceof Map<?, ?> choice)) {
            return "";
        }
        Object deltaValue = choice.get("delta");
        if (!(deltaValue instanceof Map<?, ?> delta)) {
            return "";
        }
        Object content = delta.get("content");
        return content == null ? "" : String.valueOf(content);
    }

    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.openai.com/v1";
        }
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private List<RecommendationCard> buildRecommendationCards(TriageAnalysis analysis, List<KnowledgeSnippet> snippets) {
        String reason = snippets.isEmpty()
                ? "依据患者主诉和模型分诊结果推荐，当前未召回到医院知识片段。"
                : "依据患者主诉、模型分诊结果和医院知识库召回片段推荐。";
        List<RecommendationCard> cards = new ArrayList<>();
        int order = 0;
        for (String department : new LinkedHashSet<>(analysis.suggestedDepartments())) {
            cards.add(new RecommendationCard(
                    "department",
                    "dept-" + order++,
                    department,
                    "推荐科室",
                    "适合优先匹配该科室下有号源的医生。",
                    reason
            ));
        }
        return cards;
    }

    private List<ChatMessage> toLangChainMessages(List<ChatMessageInput> messages) {
        return messages.stream()
                .map(message -> {
                    if ("assistant".equalsIgnoreCase(message.role())) {
                        return AiMessage.from(message.content());
                    }
                    return UserMessage.from(message.content());
                })
                .map(ChatMessage.class::cast)
                .toList();
    }

    private String extractJson(String content) {
        String text = firstText(content, "{}").trim();
        if (text.startsWith("```")) {
            text = text.replaceFirst("^```(?:json)?", "").replaceFirst("```$", "").trim();
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String latestUserText(List<ChatMessageInput> messages) {
        return messages.stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .reduce((first, second) -> second)
                .map(ChatMessageInput::content)
                .orElse("");
    }

    private TriageAnalysis fallbackAnalysis(ChatMode mode, String latestUserText, Exception ex) {
        String text = firstText(latestUserText, "患者病情描述信息不足");
        String lower = text.toLowerCase();
        List<String> departments = new ArrayList<>();
        List<String> conditions = new ArrayList<>();
        List<String> cautions = new ArrayList<>();
        if (containsAny(lower, "胸闷", "胸痛", "心慌", "呼吸困难")) {
            departments.add("心内科");
            conditions.add("心血管相关不适");
            cautions.add("如胸痛、呼吸困难、出汗或症状加重，请优先急诊。");
        }
        if (containsAny(lower, "发热", "咳嗽", "咽痛", "鼻塞")) {
            departments.add("呼吸内科");
            conditions.add("呼吸系统感染或相关不适");
        }
        if (containsAny(lower, "头痛", "头晕", "恶心")) {
            departments.add("神经内科");
            conditions.add("神经系统相关不适");
        }
        if (departments.isEmpty()) {
            departments.add(mode == ChatMode.REGISTRATION ? "全科医学科" : "全科医学科");
        }
        if (conditions.isEmpty()) {
            conditions.add("需要进一步结合病史判断");
        }
        cautions.add("真实模型调用暂时不可用，当前为安全兜底分析：" + ex.getClass().getSimpleName());
        return new TriageAnalysis(
                text,
                conditions,
                containsAny(lower, "剧烈", "呼吸困难", "昏迷", "抽搐", "大出血") ? "HIGH" : "MEDIUM",
                departments,
                cautions,
                Map.of("modelStatus", "fallback", "reason", ex.getClass().getSimpleName())
        );
    }

    private void ensureConfigured() {
        if (properties.getAi().resolvedApiKey() == null || properties.getAi().resolvedApiKey().isBlank()) {
            throw new IllegalStateException("OpenAI API Key 未配置，无法调用真实模型链路。");
        }
    }

    private String fallbackIfBlank(String reply, ChatMode mode) {
        if (reply != null && !reply.isBlank()) {
            return reply;
        }
        return mode == ChatMode.REGISTRATION
                ? "抱歉，模型暂时没有生成有效内容。建议补充年龄、性别、主要症状、持续时间和既往病史后重新尝试；如症状明显加重，请优先线下就医。"
                : "抱歉，模型暂时没有生成有效内容。建议补充症状细节、持续时间、体温和伴随表现；如出现急危重症信号，请立即线下就医。";
    }

    private String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> nonEmpty(List<String> values, List<String> fallback) {
        return values == null || values.isEmpty() ? fallback : values;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record AnalysisJson(
            String symptomSummary,
            List<String> possibleConditions,
            String urgencyLevel,
            List<String> suggestedDepartments,
            List<String> cautionNotes,
            Map<String, String> extractedSlots
    ) {
    }
}
