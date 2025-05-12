package com.example.springdocs.model;

import java.io.Serializable; // Good practice for model/entity classes

/**
 * Represents a document record.
 */
public class DocumentRecord implements Serializable {
    private static final long serialVersionUID = 1L; // Recommended for Serializable classes

    private String documentId;
    private String title;
    private String ownerUserId; // ID of the user who owns/can access this doc
    private String content; // Example potentially sensitive content

    // Default constructor - often needed by frameworks
    public DocumentRecord() {
    }

    public DocumentRecord(String documentId, String title, String ownerUserId, String content) {
        this.documentId = documentId;
        this.title = title;
        this.ownerUserId = ownerUserId;
        this.content = content;
    }

    // Getters
    public String getDocumentId() {
        return documentId;
    }

    public String getTitle() {
        return title;
    }

    public String getOwnerUserId() {
        return ownerUserId;
    }

    public String getContent() {
        return content;
    }

    // Setters (optional, but can be useful)
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setOwnerUserId(String ownerUserId) {
        this.ownerUserId = ownerUserId;
    }

    public void setContent(String content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "DocumentRecord{" +
               "documentId='" + documentId + '\'' +
               ", title='" + title + '\'' +
               ", ownerUserId='" + ownerUserId + '\'' +
               // Avoid logging full content in toString for potentially large/sensitive data
               ", contentPresent=" + (content != null && !content.isEmpty()) +
               '}';
    }
}
