package com.demo.change.service;

import com.demo.change.dto.request.CreateChangeRequest;
import com.demo.change.dto.request.UpdateChangeRequest;
import com.demo.change.dto.response.ChangeDetailResponse;
import com.demo.change.dto.response.ChangeListItemResponse;
import com.demo.change.dto.response.PageResponse;

public interface ChangeService {

    PageResponse<ChangeListItemResponse> getChanges(
            String status, String createdByCode, String fromDate, String toDate, int page, int size);

    ChangeListItemResponse createChange(CreateChangeRequest request, String createdBy, String createdByCode);

    ChangeDetailResponse getChangeDetail(Long id);

    void updateChange(Long id, UpdateChangeRequest request, String updatedBy, String updatedByCode);

    void submitChange(Long id, String submittedBy, String submittedByCode);

    void approveChange(Long id, String approverUsername, String approverCode, String note);

    void rejectChange(Long id, String approverUsername, String approverCode, String note);

    void executeChange(Long id, String executedBy, String executedByCode);

    void updateChecklistItemStatus(Long changeId, Long itemId, String taskStatus, String username);

    void finalizeResult(Long changeId, String username, String userCode);
}
