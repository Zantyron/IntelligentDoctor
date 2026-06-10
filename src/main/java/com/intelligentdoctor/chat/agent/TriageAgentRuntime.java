package com.intelligentdoctor.chat.agent;

import com.intelligentdoctor.chat.dto.ChatStreamResult;

import java.util.function.Consumer;

public interface TriageAgentRuntime {

    boolean supports(String runtime);

    ChatStreamResult run(TriageAgentRequest request, Consumer<String> tokenConsumer);
}
