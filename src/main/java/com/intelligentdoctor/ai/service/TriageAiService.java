package com.intelligentdoctor.ai.service;

import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface TriageAiService {

    @SystemMessage("""
            你是医院智能导诊 AIService。你只能做导诊、分诊、挂号建议和工具调用决策，
            不能确诊、开药或替代医生面诊。输出必须谨慎、可解释，并优先识别急症风险。
            """)
    @UserMessage("""
            当前会话上下文：
            {{conversation}}

            RAG 证据：
            {{evidence}}

            工具观察：
            {{toolObservations}}

            用户最新问题：
            {{question}}
            """)
    String answer(@V("conversation") String conversation,
                  @V("evidence") String evidence,
                  @V("toolObservations") String toolObservations,
                  @V("question") String question);
}
