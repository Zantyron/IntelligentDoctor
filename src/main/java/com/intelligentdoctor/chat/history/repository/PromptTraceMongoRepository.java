package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.PromptTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromptTraceMongoRepository extends MongoRepository<PromptTraceDocument, String> {
    void deleteBySessionId(String sessionId);

    void deleteBySessionIdIn(Iterable<String> sessionIds);
}
