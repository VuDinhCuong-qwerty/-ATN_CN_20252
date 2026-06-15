package com.demo.change.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.change.constant.ErrorCode;
import com.demo.change.dto.request.CreateDocumentRequest;
import com.demo.change.dto.response.DocumentResponse;
import com.demo.change.entity.Document;
import com.demo.change.exception.BusinessException;
import com.demo.change.repository.ChangeRequestRepository;
import com.demo.change.repository.DocumentRepository;
import com.demo.change.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final DocumentRepository documentRepository;
    private final ChangeRequestRepository changeRequestRepository;

    @Override
    public List<DocumentResponse> getDocuments(Long changeRequestId) {
        List<Document> docs = documentRepository.findByChangeRequestIdAndStatusOrderByCreatedAtDesc(changeRequestId, 1);
        List<DocumentResponse> result = new ArrayList<>();
        for (Document d : docs) {
            result.add(toResponse(d));
        }
        return result;
    }

    @Override
    @Transactional
    public DocumentResponse addDocument(Long changeRequestId, CreateDocumentRequest body,
                                        String createdBy, String createdByCode) {
        if (!changeRequestRepository.existsById(changeRequestId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + changeRequestId);
        }
        LocalDateTime now = LocalDateTime.now();
        Document doc = Document.builder()
                .changeRequestId(changeRequestId)
                .docType(body.getDocType())
                .title(body.getTitle())
                .url(body.getUrl())
                .note(body.getNote())
                .status(1)
                .createdBy(createdBy)
                .createdByCode(createdByCode)
                .createdAt(now)
                .updatedAt(now)
                .build();
        doc = documentRepository.save(doc);
        log.info("[DocumentService] addDocument changeId={} title={} by={}", changeRequestId, body.getTitle(), createdBy);
        return toResponse(doc);
    }

    @Override
    @Transactional
    public void deleteDocument(Long changeRequestId, Long documentId, String username) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy document id=" + documentId));
        if (!changeRequestId.equals(doc.getChangeRequestId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Document không thuộc change request này");
        }
        doc.setStatus(0);
        doc.setUpdatedAt(LocalDateTime.now());
        documentRepository.save(doc);
        log.info("[DocumentService] deleteDocument changeId={} docId={} by={}", changeRequestId, documentId, username);
    }

    private DocumentResponse toResponse(Document d) {
        DocumentResponse r = new DocumentResponse();
        r.setId(d.getId());
        r.setChangeRequestId(d.getChangeRequestId());
        r.setDocType(d.getDocType());
        r.setTitle(d.getTitle());
        r.setUrl(d.getUrl());
        r.setNote(d.getNote());
        r.setCreatedBy(d.getCreatedBy());
        r.setCreatedByCode(d.getCreatedByCode());
        r.setCreatedAt(d.getCreatedAt());
        return r;
    }
}
