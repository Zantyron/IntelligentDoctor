package com.intelligentdoctor.ai.provider;

import com.intelligentdoctor.ai.dto.AiPromptContext;
import com.intelligentdoctor.ai.dto.KnowledgeSnippet;
import com.intelligentdoctor.ai.dto.TriageAnalysis;
import com.intelligentdoctor.chat.dto.ChatMessageInput;
import com.intelligentdoctor.chat.dto.ChatStreamResult;
import com.intelligentdoctor.chat.model.ChatMode;

import java.util.List;
import java.util.function.Consumer;

public interface AiGateway {

    TriageAnalysis analyze(ChatMode mode, List<ChatMessageInput> messages);

    ChatStreamResult composeReply(
            ChatMode mode,
            TriageAnalysis analysis,
            List<KnowledgeSnippet> snippets,
            AiPromptContext promptContext,
            List<ChatMessageInput> messages
    );

    default ChatStreamResult composeReplyStreaming(
            ChatMode mode,
            TriageAnalysis analysis,
            List<KnowledgeSnippet> snippets,
            AiPromptContext promptContext,
            List<ChatMessageInput> messages,
            Consumer<String> tokenConsumer
    ) {
        ChatStreamResult result = composeReply(mode, analysis, snippets, promptContext, messages);
        tokenConsumer.accept(result.reply());
        return result;
    }
}
