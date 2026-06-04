package com.demo.change.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.demo.change.constant.ErrorCode;
import com.demo.change.dto.request.CreateChangeRequest;
import com.demo.change.dto.request.UpdateChangeRequest;
import com.demo.change.dto.response.ChangeDetailResponse;
import com.demo.change.dto.response.ChangeDetailResponse.ApproverDetail;
import com.demo.change.dto.response.ChangeDetailResponse.ChecklistDetail;
import com.demo.change.dto.response.ChangeDetailResponse.JobDetail;
import com.demo.change.dto.response.ChangeDetailResponse.TeamMemberDetail;
import com.demo.change.dto.response.ChangeListItemResponse;
import com.demo.change.dto.response.PageResponse;
import com.demo.change.entity.Approver;
import com.demo.change.entity.ChangeRequest;
import com.demo.change.entity.ChecklistItem;
import com.demo.change.entity.GoliveJob;
import com.demo.change.entity.TeamMember;
import com.demo.change.exception.BusinessException;
import com.demo.change.repository.ApproverRepository;
import com.demo.change.repository.ChangeRequestRepository;
import com.demo.change.repository.ChecklistItemRepository;
import com.demo.change.repository.GoliveJobRepository;
import com.demo.change.repository.TeamMemberRepository;
import com.demo.change.service.ChangeService;
import com.demo.change.service.IdentityValidationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChangeServiceImpl implements ChangeService {

    private final ChangeRequestRepository changeRequestRepository;
    private final GoliveJobRepository goliveJobRepository;
    private final ChecklistItemRepository checklistItemRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final ApproverRepository approverRepository;
    private final IdentityValidationService identityValidationService;

    @Override
    public PageResponse<ChangeListItemResponse> getChanges(
            String status, String createdByCode, String fromDate, String toDate, int page, int size) {

        LocalDateTime from = (fromDate != null && !fromDate.isBlank())
                ? LocalDate.parse(fromDate).atStartOfDay() : null;
        LocalDateTime to = (toDate != null && !toDate.isBlank())
                ? LocalDate.parse(toDate).atTime(23, 59, 59) : null;

        String statusParam = (status != null && !status.isBlank()) ? status : null;
        String codeParam = (createdByCode != null && !createdByCode.isBlank()) ? createdByCode : null;

        Page<ChangeRequest> pageResult = changeRequestRepository.findWithFilters(
                statusParam, codeParam, from, to, PageRequest.of(page, size));

        List<ChangeListItemResponse> content = pageResult.getContent().stream()
                .map(this::toListItem)
                .collect(Collectors.toList());

        log.info("[ChangeService] getChanges status={} createdByCode={} from={} to={} → {}/{} records",
                statusParam, codeParam, fromDate, toDate, content.size(), pageResult.getTotalElements());

        return PageResponse.<ChangeListItemResponse>builder()
                .content(content)
                .totalElement(pageResult.getTotalElements())
                .totalPage(pageResult.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    @Override
    @Transactional
    public ChangeListItemResponse createChange(CreateChangeRequest req, String createdBy, String createdByCode) {

        // ── Validate người tạo ─────────────────────────────────────────────
        identityValidationService.validateUserActive(createdBy);

        // ── Validate danh sách CAB approver ───────────────────────────────
        if (req.getApprovers() != null) {
            for (var a : req.getApprovers()) {
                identityValidationService.validateUserIsCab(a.getUsername());
            }
        }

        // ── Validate ASSIGNED_TO trong checklist ──────────────────────────
        if (req.getChecklistItems() != null) {
            for (var c : req.getChecklistItems()) {
                if (c.getAssignedTo() != null && !c.getAssignedTo().isBlank()) {
                    identityValidationService.validateUserActive(c.getAssignedTo());
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();

        ChangeRequest change = ChangeRequest.builder()
                .changeId(UUID.randomUUID().toString())
                .changeName(req.getChangeName())
                .content(req.getContent())
                .gitLink(req.getGitLink())
                .goliveAt(req.getGoliveAt())
                .status(ChangeRequest.STATUS.DRAFT)
                .createdBy(createdBy)
                .createdByCode(createdByCode)
                .createdAt(now)
                .updatedAt(now)
                .build();

        change = changeRequestRepository.save(change);
        final Long changeId = change.getId();
        log.info("[ChangeService] createChange id={} by={}", changeId, createdBy);

        if (req.getJobs() != null && !req.getJobs().isEmpty()) {
            List<GoliveJob> jobs = req.getJobs().stream()
                    .map(j -> GoliveJob.builder()
                            .changeRequestId(changeId)
                            .name(j.getName())
                            .link(j.getLink())
                            .jobType(j.getJobType())
                            .orderNum(j.getOrderNum())
                            .status(GoliveJob.STATUS.ACTIVE)
                            .createdBy(createdBy)
                            .createdByCode(createdByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build())
                    .collect(Collectors.toList());
            goliveJobRepository.saveAll(jobs);
        }

        if (req.getChecklistItems() != null && !req.getChecklistItems().isEmpty()) {
            List<ChecklistItem> items = req.getChecklistItems().stream()
                    .map(c -> ChecklistItem.builder()
                            .changeRequestId(changeId)
                            .phase(c.getPhase())
                            .stepText(c.getStepText())
                            .orderNum(c.getOrderNum())
                            .assignedTo(c.getAssignedTo())
                            .assignedToCode(c.getAssignedToCode())
                            .taskStatus(ChecklistItem.TASK_STATUS.READY)
                            .status(ChecklistItem.STATUS.ACTIVE)
                            .createdBy(createdBy)
                            .createdByCode(createdByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build())
                    .collect(Collectors.toList());
            checklistItemRepository.saveAll(items);
        }

        if (req.getTeamMembers() != null && !req.getTeamMembers().isEmpty()) {
            List<TeamMember> members = req.getTeamMembers().stream()
                    .map(m -> TeamMember.builder()
                            .changeRequestId(changeId)
                            .userId(m.getUserId())
                            .username(m.getUsername())
                            .fullName(m.getFullName())
                            .employeeCode(m.getEmployeeCode())
                            .memberRole(m.getMemberRole())
                            .isLead(Boolean.TRUE.equals(m.getIsLead()) ? 1 : 0)
                            .status(TeamMember.STATUS.ACTIVE)
                            .createdBy(createdBy)
                            .createdByCode(createdByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build())
                    .collect(Collectors.toList());
            teamMemberRepository.saveAll(members);
        }

        if (req.getApprovers() != null && !req.getApprovers().isEmpty()) {
            List<Approver> approvers = req.getApprovers().stream()
                    .map(a -> Approver.builder()
                            .changeRequestId(changeId)
                            .userId(a.getUserId())
                            .username(a.getUsername())
                            .fullName(a.getFullName())
                            .employeeCode(a.getEmployeeCode())
                            .approveStatus(Approver.APPROVE_STATUS.PENDING)
                            .status(Approver.STATUS.ACTIVE)
                            .createdBy(createdBy)
                            .createdByCode(createdByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build())
                    .collect(Collectors.toList());
            approverRepository.saveAll(approvers);
        }

        return toListItem(change);
    }

    @Override
    public ChangeDetailResponse getChangeDetail(Long id) {
        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        List<JobDetail> jobs = goliveJobRepository.findActiveByChangeRequestId(id).stream()
                .map(j -> JobDetail.builder()
                        .id(j.getId())
                        .name(j.getName())
                        .link(j.getLink())
                        .jobType(j.getJobType())
                        .orderNum(j.getOrderNum())
                        .createdBy(j.getCreatedBy())
                        .createdByCode(j.getCreatedByCode())
                        .createdAt(j.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<ChecklistDetail> checklistItems = checklistItemRepository.findActiveByChangeRequestId(id).stream()
                .map(c -> ChecklistDetail.builder()
                        .id(c.getId())
                        .phase(c.getPhase())
                        .stepText(c.getStepText())
                        .orderNum(c.getOrderNum())
                        .assignedTo(c.getAssignedTo())
                        .assignedToCode(c.getAssignedToCode())
                        .taskStatus(c.getTaskStatus())
                        .createdBy(c.getCreatedBy())
                        .createdByCode(c.getCreatedByCode())
                        .createdAt(c.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<TeamMemberDetail> teamMembers = teamMemberRepository.findActiveByChangeRequestId(id).stream()
                .map(m -> TeamMemberDetail.builder()
                        .id(m.getId())
                        .userId(m.getUserId())
                        .username(m.getUsername())
                        .fullName(m.getFullName())
                        .employeeCode(m.getEmployeeCode())
                        .memberRole(m.getMemberRole())
                        .isLead(m.getIsLead())
                        .createdBy(m.getCreatedBy())
                        .createdByCode(m.getCreatedByCode())
                        .createdAt(m.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        List<ApproverDetail> approvers = approverRepository.findActiveByChangeRequestId(id).stream()
                .map(a -> ApproverDetail.builder()
                        .id(a.getId())
                        .userId(a.getUserId())
                        .username(a.getUsername())
                        .fullName(a.getFullName())
                        .employeeCode(a.getEmployeeCode())
                        .approveStatus(a.getApproveStatus())
                        .note(a.getNote())
                        .decidedAt(a.getDecidedAt())
                        .createdBy(a.getCreatedBy())
                        .createdByCode(a.getCreatedByCode())
                        .createdAt(a.getCreatedAt())
                        .build())
                .collect(Collectors.toList());

        log.info("[ChangeService] getChangeDetail id={}", id);

        return ChangeDetailResponse.builder()
                .id(change.getId())
                .changeId(change.getChangeId())
                .changeName(change.getChangeName())
                .content(change.getContent())
                .gitLink(change.getGitLink())
                .status(change.getStatus())
                .goliveAt(change.getGoliveAt())
                .createdBy(change.getCreatedBy())
                .createdByCode(change.getCreatedByCode())
                .createdAt(change.getCreatedAt())
                .updatedAt(change.getUpdatedAt())
                .jobs(jobs)
                .checklistItems(checklistItems)
                .teamMembers(teamMembers)
                .approvers(approvers)
                .build();
    }

    @Override
    @Transactional
    public void submitChange(Long id, String submittedBy, String submittedByCode) {
        log.info("[ChangeService] submitChange id={} by={}/{}", id, submittedBy, submittedByCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.DRAFT.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái DRAFT");
        }
        if (checklistItemRepository.countActiveByChangeRequestId(id) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Checklist không được trống");
        }
        if (goliveJobRepository.countActiveByChangeRequestId(id) == 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Danh sách job golive không được trống");
        }

        List<Approver> approvers = approverRepository.findActiveByChangeRequestId(id);
        if (approvers.isEmpty()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Danh sách CAB không được trống");
        }

        Set<String> approverUsernames = approvers.stream()
                .map(a -> a.getUsername().toLowerCase())
                .collect(Collectors.toSet());

        if (approverUsernames.contains(submittedBy.toLowerCase())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Người tạo change không được là CAB approver: " + submittedBy);
        }

        for (TeamMember m : teamMemberRepository.findActiveByChangeRequestId(id)) {
            if (approverUsernames.contains(m.getUsername().toLowerCase())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Thành viên nhóm '" + m.getUsername() + "' không được là CAB approver");
            }
        }

        // Reset tất cả approvers về PENDING để bắt đầu chu kỳ duyệt mới
        approvers.forEach(a -> {
            a.setApproveStatus(Approver.APPROVE_STATUS.PENDING);
            a.setNote(null);
            a.setDecidedAt(null);
        });
        approverRepository.saveAll(approvers);

        LocalDateTime now = LocalDateTime.now();
        change.setStatus(ChangeRequest.STATUS.PENDING);
        change.setUpdatedAt(now);
        changeRequestRepository.save(change);

        log.info("[ChangeService] submitChange id={} → PENDING, {} CAB approvers reset", id, approvers.size());
    }

    @Override
    @Transactional
    public void approveChange(Long id, String approverUsername, String approverCode, String note) {
        log.info("[ChangeService] approveChange id={} by={}/{}", id, approverUsername, approverCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.PENDING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái PENDING");
        }

        Approver approver = approverRepository.findActiveByChangeRequestIdAndUsername(id, approverUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Bạn không có trong danh sách CAB của change request này"));

        if (!Approver.APPROVE_STATUS.PENDING.equals(approver.getApproveStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bạn đã quyết định cho change request này rồi");
        }

        LocalDateTime now = LocalDateTime.now();
        approver.setApproveStatus(Approver.APPROVE_STATUS.APPROVED);
        approver.setNote(note);
        approver.setDecidedAt(now);
        approverRepository.save(approver);

        int pendingCount = approverRepository.countPendingByChangeRequestId(id);
        if (pendingCount == 0) {
            change.setStatus(ChangeRequest.STATUS.APPROVED);
            change.setUpdatedAt(now);
            changeRequestRepository.save(change);
            log.info("[ChangeService] approveChange id={} → tất cả CAB approved → APPROVED", id);
        } else {
            log.info("[ChangeService] approveChange id={} by={} approved, còn {} CAB chưa duyệt", id, approverUsername, pendingCount);
        }
    }

    @Override
    @Transactional
    public void rejectChange(Long id, String approverUsername, String approverCode, String note) {
        log.info("[ChangeService] rejectChange id={} by={}/{}", id, approverUsername, approverCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.PENDING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái PENDING");
        }

        Approver approver = approverRepository.findActiveByChangeRequestIdAndUsername(id, approverUsername)
                .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Bạn không có trong danh sách CAB của change request này"));

        if (!Approver.APPROVE_STATUS.PENDING.equals(approver.getApproveStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Bạn đã quyết định cho change request này rồi");
        }

        LocalDateTime now = LocalDateTime.now();
        approver.setApproveStatus(Approver.APPROVE_STATUS.REJECTED);
        approver.setNote(note);
        approver.setDecidedAt(now);
        approverRepository.save(approver);

        // Ngay lập tức về DRAFT, các approvers khác giữ nguyên trạng thái
        change.setStatus(ChangeRequest.STATUS.DRAFT);
        change.setUpdatedAt(now);
        changeRequestRepository.save(change);

        log.info("[ChangeService] rejectChange id={} by={} → DRAFT", id, approverUsername);
    }

    @Override
    @Transactional
    public void executeChange(Long id, String executedBy, String executedByCode) {
        log.info("[ChangeService] executeChange id={} by={}/{}", id, executedBy, executedByCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.APPROVED.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái APPROVED");
        }
        if (!executedBy.equalsIgnoreCase(change.getCreatedBy())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chỉ người tạo change mới được thực hiện execute");
        }

        change.setStatus(ChangeRequest.STATUS.EXECUTING);
        change.setUpdatedAt(LocalDateTime.now());
        changeRequestRepository.save(change);

        log.info("[ChangeService] executeChange id={} by={} → EXECUTING", id, executedBy);
    }

    @Override
    @Transactional
    public void updateChecklistItemStatus(Long changeId, Long itemId, String taskStatus, String username) {
        log.info("[ChangeService] updateChecklistItemStatus changeId={} itemId={} taskStatus={} by={}", changeId, itemId, taskStatus, username);

        if (!ChecklistItem.TASK_STATUS.SUCCESS.equals(taskStatus) && !ChecklistItem.TASK_STATUS.FAIL.equals(taskStatus)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "taskStatus không hợp lệ: " + taskStatus + " (chỉ chấp nhận SUCCESS hoặc FAIL)");
        }

        ChangeRequest change = changeRequestRepository.findById(changeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + changeId));

        if (!ChangeRequest.STATUS.EXECUTING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái EXECUTING");
        }

        ChecklistItem item = checklistItemRepository.findActiveByIdAndChangeRequestId(itemId, changeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy checklist item id=" + itemId + " trong change id=" + changeId));

        if (item.getAssignedTo() == null || item.getAssignedTo().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Checklist item này chưa được gán cho ai, không thể cập nhật trạng thái");
        }
        if (!username.equalsIgnoreCase(item.getAssignedTo())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Chỉ người được gán mới được cập nhật trạng thái bước này (assignedTo=" + item.getAssignedTo() + ")");
        }

        item.setTaskStatus(taskStatus);
        item.setUpdatedAt(LocalDateTime.now());
        checklistItemRepository.save(item);

        log.info("[ChangeService] updateChecklistItemStatus changeId={} itemId={} → {}", changeId, itemId, taskStatus);
    }

    @Override
    @Transactional
    public void finalizeResult(Long changeId, String username, String userCode) {
        log.info("[ChangeService] finalizeResult changeId={} by={}/{}", changeId, username, userCode);

        ChangeRequest change = changeRequestRepository.findById(changeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + changeId));

        if (!ChangeRequest.STATUS.EXECUTING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái EXECUTING");
        }
        if (!username.equalsIgnoreCase(change.getCreatedBy())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chỉ người tạo change mới được ghi nhận kết quả");
        }

        // Có bất kỳ item FAIL → change FAIL ngay
        int failCount = checklistItemRepository.countActiveByChangeRequestIdAndTaskStatus(changeId, ChecklistItem.TASK_STATUS.FAIL);
        if (failCount > 0) {
            change.setStatus(ChangeRequest.STATUS.FAIL);
            change.setUpdatedAt(LocalDateTime.now());
            changeRequestRepository.save(change);
            log.info("[ChangeService] finalizeResult changeId={} → FAIL ({} bước thất bại)", changeId, failCount);
            return;
        }

        // Còn bước chưa xử lý → không cho phép kết thúc
        int readyCount = checklistItemRepository.countActiveByChangeRequestIdAndTaskStatus(changeId, ChecklistItem.TASK_STATUS.READY);
        if (readyCount > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Còn " + readyCount + " bước trong checklist chưa được cập nhật trạng thái");
        }

        change.setStatus(ChangeRequest.STATUS.SUCCESS);
        change.setUpdatedAt(LocalDateTime.now());
        changeRequestRepository.save(change);

        log.info("[ChangeService] finalizeResult changeId={} → SUCCESS", changeId);
    }

    @Override
    @Transactional
    public void updateChange(Long id, UpdateChangeRequest req, String updatedBy, String updatedByCode) {
        log.info("[ChangeService] updateChange id={} by={}/{}", id, updatedBy, updatedByCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.DRAFT.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chỉ được cập nhật change ở trạng thái DRAFT");
        }
        if (!updatedBy.equalsIgnoreCase(change.getCreatedBy())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Chỉ người tạo change mới được cập nhật");
        }

        // ── Pre-validate: chạy hết trước khi write bất kỳ thứ gì ──────────
        identityValidationService.validateUserActive(updatedBy);

        if (req.getApprovers() != null) {
            for (var a : req.getApprovers()) {
                if (!isBeingDeleted(a.getStatus())) {
                    identityValidationService.validateUserIsCab(a.getUsername());
                }
            }
        }

        if (req.getChecklistItems() != null) {
            for (var c : req.getChecklistItems()) {
                if (!isBeingDeleted(c.getStatus()) && c.getAssignedTo() != null && !c.getAssignedTo().isBlank()) {
                    identityValidationService.validateUserActive(c.getAssignedTo());
                }
            }
        }

        LocalDateTime now = LocalDateTime.now();

        // ── Update main fields ──────────────────────────────────────────────
        change.setChangeName(req.getChangeName());
        change.setContent(req.getContent());
        change.setGitLink(req.getGitLink());
        change.setGoliveAt(req.getGoliveAt());
        change.setUpdatedAt(now);
        changeRequestRepository.save(change);

        // ── Sync child sections ─────────────────────────────────────────────
        if (req.getJobs() != null) {
            for (var input : req.getJobs()) {
                if (input.getId() != null) {
                    GoliveJob job = goliveJobRepository.findById(input.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Job id=" + input.getId() + " không tồn tại"));
                    job.setName(input.getName());
                    job.setLink(input.getLink());
                    job.setJobType(input.getJobType());
                    job.setOrderNum(input.getOrderNum());
                    if (input.getStatus() != null) job.setStatus(input.getStatus());
                    job.setUpdatedAt(now);
                    goliveJobRepository.save(job);
                } else {
                    goliveJobRepository.save(GoliveJob.builder()
                            .changeRequestId(id)
                            .name(input.getName())
                            .link(input.getLink())
                            .jobType(input.getJobType())
                            .orderNum(input.getOrderNum())
                            .status(GoliveJob.STATUS.ACTIVE)
                            .createdBy(updatedBy)
                            .createdByCode(updatedByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                }
            }
        }

        if (req.getChecklistItems() != null) {
            for (var input : req.getChecklistItems()) {
                if (input.getId() != null) {
                    ChecklistItem item = checklistItemRepository.findById(input.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Checklist item id=" + input.getId() + " không tồn tại"));
                    item.setPhase(input.getPhase());
                    item.setStepText(input.getStepText());
                    item.setOrderNum(input.getOrderNum());
                    item.setAssignedTo(input.getAssignedTo());
                    item.setAssignedToCode(input.getAssignedToCode());
                    if (input.getStatus() != null) item.setStatus(input.getStatus());
                    item.setUpdatedAt(now);
                    checklistItemRepository.save(item);
                } else {
                    if (input.getAssignedTo() != null && !input.getAssignedTo().isBlank()) {
                        identityValidationService.validateUserActive(input.getAssignedTo());
                    }
                    checklistItemRepository.save(ChecklistItem.builder()
                            .changeRequestId(id)
                            .phase(input.getPhase())
                            .stepText(input.getStepText())
                            .orderNum(input.getOrderNum())
                            .assignedTo(input.getAssignedTo())
                            .assignedToCode(input.getAssignedToCode())
                            .taskStatus(ChecklistItem.TASK_STATUS.READY)
                            .status(ChecklistItem.STATUS.ACTIVE)
                            .createdBy(updatedBy)
                            .createdByCode(updatedByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                }
            }
        }

        if (req.getTeamMembers() != null) {
            for (var input : req.getTeamMembers()) {
                if (input.getId() != null) {
                    TeamMember member = teamMemberRepository.findById(input.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Team member id=" + input.getId() + " không tồn tại"));
                    member.setUsername(input.getUsername());
                    member.setUserId(input.getUserId());
                    member.setFullName(input.getFullName());
                    member.setEmployeeCode(input.getEmployeeCode());
                    member.setMemberRole(input.getMemberRole());
                    member.setIsLead(Boolean.TRUE.equals(input.getIsLead()) ? 1 : 0);
                    if (input.getStatus() != null) member.setStatus(input.getStatus());
                    member.setUpdatedAt(now);
                    teamMemberRepository.save(member);
                } else {
                    teamMemberRepository.save(TeamMember.builder()
                            .changeRequestId(id)
                            .username(input.getUsername())
                            .userId(input.getUserId())
                            .fullName(input.getFullName())
                            .employeeCode(input.getEmployeeCode())
                            .memberRole(input.getMemberRole())
                            .isLead(Boolean.TRUE.equals(input.getIsLead()) ? 1 : 0)
                            .status(TeamMember.STATUS.ACTIVE)
                            .createdBy(updatedBy)
                            .createdByCode(updatedByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                }
            }
        }

        if (req.getApprovers() != null) {
            for (var input : req.getApprovers()) {
                if (input.getId() != null) {
                    Approver approver = approverRepository.findById(input.getId())
                            .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Approver id=" + input.getId() + " không tồn tại"));
                    approver.setUsername(input.getUsername());
                    approver.setUserId(input.getUserId());
                    approver.setFullName(input.getFullName());
                    approver.setEmployeeCode(input.getEmployeeCode());
                    if (input.getStatus() != null) approver.setStatus(input.getStatus());
                    approver.setUpdatedAt(now);
                    approverRepository.save(approver);
                } else {
                    identityValidationService.validateUserIsCab(input.getUsername());
                    approverRepository.save(Approver.builder()
                            .changeRequestId(id)
                            .username(input.getUsername())
                            .userId(input.getUserId())
                            .fullName(input.getFullName())
                            .employeeCode(input.getEmployeeCode())
                            .approveStatus(Approver.APPROVE_STATUS.PENDING)
                            .status(Approver.STATUS.ACTIVE)
                            .createdBy(updatedBy)
                            .createdByCode(updatedByCode)
                            .createdAt(now)
                            .updatedAt(now)
                            .build());
                }
            }
        }

        log.info("[ChangeService] updateChange id={} done", id);
    }

    private ChangeListItemResponse toListItem(ChangeRequest c) {
        return ChangeListItemResponse.builder()
                .id(c.getId())
                .changeId(c.getChangeId())
                .changeName(c.getChangeName())
                .status(c.getStatus())
                .goliveAt(c.getGoliveAt())
                .createdBy(c.getCreatedBy())
                .createdByCode(c.getCreatedByCode())
                .createdAt(c.getCreatedAt())
                .updatedAt(c.getUpdatedAt())
                .build();
    }

    /** status=0 nghĩa là client đang yêu cầu soft-delete → không cần validate */
    private boolean isBeingDeleted(Integer status) {
        return status != null && status == 0;
    }
}
