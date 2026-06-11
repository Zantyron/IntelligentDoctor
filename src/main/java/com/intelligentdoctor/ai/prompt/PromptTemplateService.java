package com.intelligentdoctor.ai.prompt;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.chat.model.ChatMode;
import org.springframework.stereotype.Service;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class PromptTemplateService {

    public AiPromptContext build(ChatMode mode, List<String> evidence) {
        return build(mode, evidence, null);
    }

    public AiPromptContext build(ChatMode mode, List<String> evidence, ZonedDateTime requestTime) {
        String systemPrompt = """
                你是“智能导诊”系统中的 AI 导诊中枢。你只能提供导诊、分诊和挂号建议，不能给出确诊、处方或替代医生面诊。
                输出必须谨慎、可解释、面向中文患者；当出现急危重症信号时，明确建议立即线下就医或急诊。
                """;

        String businessPrompt = switch (mode) {
            case DIAGNOSIS -> """
                    当前模式为诊断模式。
                    请从用户描述中提取症状、持续时间、诱因、伴随表现和危险信号，输出可能方向、风险等级、线下就医建议和需要澄清追问的问题。
                    """;
            case REGISTRATION -> """
                    当前模式为智能挂号模式。
                    请结合医院知识库、科室职责、医生专长、排班和挂号规则，输出推荐科室/医生与推荐依据。信息不足时先追问关键挂号信息。
                    """;
        };

        String ragPrompt = evidence.isEmpty()
                ? "当前没有召回到医院知识片段。请说明信息有限，只基于患者描述和通用导诊原则给出谨慎建议。"
                : "可用医院知识库召回片段：\n- " + String.join("\n- ", evidence);

        String toolPrompt = """
                工具和业务规则：
                1. 挂号模式下，优先检索科室、诊室、医生、排班和挂号规则。
                2. 确认挂号前必须补齐姓名、手机号、身份证号、性别、年龄、就诊日期和时段。
                3. 信息缺失时先追问，不要盲目确认挂号。
                4. 输出中必须区分“建议”“推荐依据”“下一步行动”。
                """;

        return new AiPromptContext(mode, systemPrompt + timePrompt(requestTime), businessPrompt, ragPrompt, toolPrompt, evidence);
    }

    private String timePrompt(ZonedDateTime requestTime) {
        if (requestTime == null) {
            return "";
        }
        return "\nCurrent hospital business time: "
                + requestTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))
                + " (" + requestTime.getZone() + "). For registration answers, first use this exact date and time, "
                + "and only recommend appointment slots later than the current time. Do not recommend elapsed same-day periods.";
    }
}
