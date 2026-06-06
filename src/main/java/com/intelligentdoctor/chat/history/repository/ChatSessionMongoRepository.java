package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.ChatSessionDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChatSessionMongoRepository extends MongoRepository<ChatSessionDocument, String> {
    Optional<ChatSessionDocument> findBySessionId(String sessionId);
}
