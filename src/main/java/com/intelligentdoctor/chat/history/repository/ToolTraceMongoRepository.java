package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.ToolTraceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ToolTraceMongoRepository extends MongoRepository<ToolTraceDocument, String> {
    void deleteBySessionId(String sessionId);
    void deleteByHospitalIdAndSessionId(String hospitalId, String sessionId);

    void deleteBySessionIdIn(Iterable<String> sessionIds);
    void deleteByHospitalIdAndSessionIdIn(String hospitalId, Iterable<String> sessionIds);
}
