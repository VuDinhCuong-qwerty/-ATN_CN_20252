package com.demo.change.service;

import java.util.List;

import com.demo.change.dto.request.CreateDocumentRequest;
import com.demo.change.dto.response.DocumentResponse;

public interface DocumentService {
    List<DocumentResponse> getDocuments(Long changeRequestId);
    DocumentResponse addDocument(Long changeRequestId, CreateDocumentRequest body, String createdBy, String createdByCode);
    void deleteDocument(Long changeRequestId, Long documentId, String username);
}
