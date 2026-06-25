package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {
    List<ChatMessageDocument> findBySessionIdOrderByCreatedAtAsc(String sessionId);
    List<ChatMessageDocument> findByHospitalIdAndSessionIdOrderByCreatedAtAsc(String hospitalId, String sessionId);

    void deleteBySessionId(String sessionId);
    void deleteByHospitalIdAndSessionId(String hospitalId, String sessionId);

    void deleteBySessionIdIn(Iterable<String> sessionIds);
    void deleteByHospitalIdAndSessionIdIn(String hospitalId, Iterable<String> sessionIds);
}
