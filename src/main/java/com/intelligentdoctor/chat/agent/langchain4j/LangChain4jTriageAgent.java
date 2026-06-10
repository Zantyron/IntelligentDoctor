package com.intelligentdoctor.chat.agent.langchain4j;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.TokenStream;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LangChain4jTriageAgent {

    @SystemMessage("""
            你是智能导诊 Agent，只能提供导诊、分诊、挂号建议和工具调用决策，不能确诊、开药或替代医生面诊。
            你需要结合医院知识库证据、业务工具结果和历史上下文回答。出现胸痛、呼吸困难、意识障碍、大出血、
            剧烈头痛、抽搐等急危重信号时，必须建议立即线下就医或急诊。
            """)
    @UserMessage("""
            当前模式：{{mode}}
            医院 ID：{{hospitalId}}

            历史会话：
            {{conversation}}

            RAG 证据：
            {{evidence}}

            已知结构化分诊结果：
            {{analysis}}

            用户最新问题：
            {{question}}

            请输出中文回答：
            1. 诊断模式包含病情归纳、可能方向、风险提醒、下一步建议和需要补充的问题。
            2. 挂号模式包含推荐科室/医生、推荐依据和下一步行动。
            3. 如需业务信息，请通过可用工具查询科室、诊室、医生、排班、挂号规则或创建挂号草稿。
            4. 不要声称已经确诊，不要开处方。
            """)
    TokenStream chat(@MemoryId String memoryId,
                     @V("mode") String mode,
                     @V("hospitalId") String hospitalId,
                     @V("conversation") String conversation,
                     @V("evidence") String evidence,
                     @V("analysis") String analysis,
                     @V("question") String question);
}
