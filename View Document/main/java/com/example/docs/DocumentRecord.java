package com.example.docs.model;

/**
 * Represents a document record.
 */
public class DocumentRecord {
    private String documentId;
    private String title;
    private String ownerUserId; // ID of the user who owns/can access this doc
    private String content; // Example potentially sensitive content

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
}