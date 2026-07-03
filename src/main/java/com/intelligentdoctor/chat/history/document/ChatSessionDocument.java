package com.intelligentdoctor.chat.history.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document("chat_session")
public class ChatSessionDocument {

    @Id
    private String id;
    private String sessionId;
    private String hospitalId;
    private String terminalUsername;
    private String mode;
    private boolean consentToStoreHistory;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getHospitalId() {
        return hospitalId;
    }

    public void setHospitalId(String hospitalId) {
        this.hospitalId = hospitalId;
    }

    public String getTerminalUsername() {
        return terminalUsername;
    }

    public void setTerminalUsername(String terminalUsername) {
        this.terminalUsername = terminalUsername;
    }

    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public boolean isConsentToStoreHistory() {
        return consentToStoreHistory;
    }

    public void setConsentToStoreHistory(boolean consentToStoreHistory) {
        this.consentToStoreHistory = consentToStoreHistory;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
