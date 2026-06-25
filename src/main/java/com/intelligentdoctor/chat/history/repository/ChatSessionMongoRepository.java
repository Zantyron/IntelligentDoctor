package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.ChatSessionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatSessionMongoRepository extends MongoRepository<ChatSessionDocument, String> {
    Optional<ChatSessionDocument> findBySessionId(String sessionId);
    Optional<ChatSessionDocument> findByHospitalIdAndSessionId(String hospitalId, String sessionId);

    List<ChatSessionDocument> findByHospitalIdOrderByUpdatedAtDesc(String hospitalId);

    void deleteBySessionId(String sessionId);
    void deleteByHospitalIdAndSessionId(String hospitalId, String sessionId);

    void deleteBySessionIdIn(Iterable<String> sessionIds);
    void deleteByHospitalIdAndSessionIdIn(String hospitalId, Iterable<String> sessionIds);
}
