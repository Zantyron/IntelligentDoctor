package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {
    List<ChatMessageDocument> findBySessionIdOrderByCreatedAtAsc(String sessionId);

    void deleteBySessionId(String sessionId);

    void deleteBySessionIdIn(Iterable<String> sessionIds);
}
