package com.intelligentdoctor.chat.history.repository;

import com.intelligentdoctor.chat.history.document.ChatMessageDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatMessageMongoRepository extends MongoRepository<ChatMessageDocument, String> {
}
