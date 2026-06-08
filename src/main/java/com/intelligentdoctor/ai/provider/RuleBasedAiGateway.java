package com.intelligentdoctor.ai.provider;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.ai.dto.FunctionSuggestion;
import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.dto.RecommendationCard;
import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.model.ChatMode;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Component
@ConditionalOnExpression("'${app.ai.provider:openai}' != 'openai'")
public class RuleBasedAiGateway implements AiGateway {

    @Override
    public TriageAnalysis analyze(ChatMode mode, List<ChatMessageInput> messages) {
        String text = latestUserText(messages);
        String normalized = text.toLowerCase(Locale.ROOT);
        Set<String> conditions = new LinkedHashSet<>();
        Set<String> departments = new LinkedHashSet<>();
        List<String> cautions = new ArrayList<>();
        Map<String, String> slots = new HashMap<>();

        if (containsAny(normalized, "发热", "高热", "咳嗽", "咽痛", "鼻塞")) {
            conditions.add("上呼吸道感染或呼吸系统问题");
            departments.add("呼吸内科");
        }
        if (containsAny(normalized, "头痛", "头晕", "偏头痛", "失眠")) {
            conditions.add("神经系统相关不适");
            departments.add("神经内科");
        }
        if (containsAny(normalized, "胸闷", "胸痛", "心慌", "高血压")) {
            conditions.add("心血管相关不适");
            departments.add("心内科");
            cautions.add("胸痛胸闷可能存在急症风险，如症状持续或加重请优先急诊就医。");
        }
        if (containsAny(normalized, "肚子", "腹痛", "恶心", "呕吐", "腹泻")) {
            conditions.add("消化系统相关不适");
            departments.add("消化内科");
        }
        if (containsAny(normalized, "皮肤", "皮肤病", "皮疹", "过敏", "瘙痒", "湿疹", "痘", "荨麻疹")) {
            conditions.add("过敏或皮肤问题");
            departments.add("皮肤科");
        }
        if (containsAny(normalized, "鼻塞", "耳痛", "咽痛", "喉咙", "嗓子")) {
            conditions.add("耳鼻喉相关不适");
            departments.add("耳鼻喉科");
        }
        if (containsAny(normalized, "关节", "扭伤", "腰痛", "颈椎", "骨折")) {
            conditions.add("骨科或运动损伤相关问题");
            departments.add("骨科");
        }
        if (containsAny(normalized, "儿童", "小孩", "宝宝")) {
            departments.add("儿科");
        }

        if (conditions.isEmpty()) {
            conditions.add("需要进一步判断的常见症状");
        }
        if (departments.isEmpty()) {
            departments.add("全科医学科");
        }

        String urgency = containsAny(normalized, "剧烈", "呼吸困难", "昏迷", "抽搐", "大出血")
                ? "HIGH" : "MEDIUM";
        if ("HIGH".equals(urgency)) {
            cautions.add("描述中出现急危重症信号，请优先前往急诊。");
        }
        slots.put("duration", containsAny(normalized, "三天", "3天", "两天", "2天", "一周") ? "已描述病程" : "未说明");
        slots.put("temperature", normalized.contains("38") || normalized.contains("39") ? "存在体温信息" : "未说明");

        return new TriageAnalysis(text, new ArrayList<>(conditions), urgency, new ArrayList<>(departments), cautions, slots);
    }

    @Override
    public ChatStreamResult composeReply(ChatMode mode,
                                         TriageAnalysis analysis,
                                         List<KnowledgeSnippet> snippets,
                                         AiPromptContext promptContext,
                                         List<ChatMessageInput> messages) {
        List<String> evidence = snippets.stream()
                .map(snippet -> snippet.sourceName() + ": " + snippet.text())
                .toList();
        List<RecommendationCard> cards = new ArrayList<>();
        List<FunctionSuggestion> functions = new ArrayList<>();
        StringBuilder reply = new StringBuilder();

        reply.append("以下内容仅作导诊建议，不替代医生面诊。\n\n");
        reply.append("病情归纳: ").append(analysis.symptomSummary()).append("\n");
        reply.append("可能方向: ").append(String.join("、", analysis.possibleConditions())).append("\n");
        reply.append("紧急程度: ").append(urgencyText(analysis.urgencyLevel())).append("\n");

        if (mode == ChatMode.DIAGNOSIS) {
            reply.append("建议科室: ").append(String.join("、", analysis.suggestedDepartments())).append("\n");
            if (!analysis.cautionNotes().isEmpty()) {
                reply.append("风险提示: ").append(String.join("；", analysis.cautionNotes())).append("\n");
            }
            reply.append("建议补充: 年龄、性别、症状持续时间、体温、既往病史以及是否明显加重。");
        } else {
            reply.append("推荐科室/医生: ").append(String.join("、", analysis.suggestedDepartments()))
                    .append("；系统将优先匹配该科室下有排班和号源的医生。\n");
            reply.append("推荐依据:\n");
            if (snippets.isEmpty()) {
                reply.append("- 当前基于症状描述和通用导诊规则推荐，未召回明确医院知识片段。\n");
            } else {
                snippets.stream().limit(3).forEach(snippet -> reply.append("- ").append(snippet.text()).append("\n"));
            }
            reply.append("下一步: 补充姓名、手机号、身份证号、性别和年龄后即可确认挂号。\n");
            int order = 0;
            for (String department : analysis.suggestedDepartments()) {
                cards.add(new RecommendationCard(
                        "department",
                        "dept-" + order++,
                        department,
                        "智能导诊推荐科室",
                        "适合当前症状描述，可继续筛选诊室和医生。",
                        "根据症状关键词、模型分析结果与医院知识匹配得出"
                ));
            }
            functions.add(new FunctionSuggestion(
                    "createRegistrationDraft",
                    "创建待确认挂号草稿",
                    Map.of("symptomSummary", analysis.symptomSummary(), "suggestedDepartments", analysis.suggestedDepartments()),
                    false
            ));
        }

        return new ChatStreamResult(
                reply.toString().trim(),
                analysis.symptomSummary(),
                analysis.possibleConditions(),
                cards,
                evidence,
                functions,
                Map.of("mode", mode.name(), "urgencyLevel", analysis.urgencyLevel(), "promptSummary", promptContext.businessPrompt())
        );
    }

    private String urgencyText(String urgencyLevel) {
        return switch (urgencyLevel) {
            case "HIGH" -> "较高，建议尽快线下就医或急诊。";
            case "MEDIUM" -> "中等，建议尽快预约相关科室。";
            default -> "一般，建议继续观察并补充信息。";
        };
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String latestUserText(List<ChatMessageInput> messages) {
        return messages.stream()
                .filter(message -> "user".equalsIgnoreCase(message.role()))
                .reduce((first, second) -> second)
                .map(ChatMessageInput::content)
                .orElse("");
    }
}
