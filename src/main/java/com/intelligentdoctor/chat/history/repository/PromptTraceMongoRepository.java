package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.PromptTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PromptTraceMongoRepository extends MongoRepository<PromptTraceDocument, String> {
    void deleteBySessionId(String sessionId);
    void deleteByHospitalIdAndSessionId(String hospitalId, String sessionId);

    void deleteBySessionIdIn(Iterable<String> sessionIds);
    void deleteByHospitalIdAndSessionIdIn(String hospitalId, Iterable<String> sessionIds);
}
