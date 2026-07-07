package com.demo.change.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

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
import com.demo.change.dto.response.DocumentResponse;
import com.demo.change.dto.response.GoliveJobResponse;
import com.demo.change.dto.response.PageResponse;
import com.demo.change.entity.Approver;
import com.demo.change.entity.AuditLog;
import com.demo.change.entity.ChangeRequest;
import com.demo.change.entity.ChecklistItem;
import com.demo.change.entity.Document;
import com.demo.change.entity.GoliveJob;
import com.demo.change.entity.TeamMember;
import com.demo.change.exception.BusinessException;
import com.demo.change.repository.ApproverRepository;
import com.demo.change.repository.ChangeRequestRepository;
import com.demo.change.repository.ChecklistItemRepository;
import com.demo.change.repository.DocumentRepository;
import com.demo.change.repository.GoliveJobRepository;
import com.demo.change.repository.TeamMemberRepository;
import com.demo.change.service.AuditLogService;
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
    private final DocumentRepository documentRepository;
    private final AuditLogService auditLogService;
    private final IdentityValidationService identityValidationService;

    // ── getChanges ────────────────────────────────────────────────────────────

    @Override
    public PageResponse<ChangeListItemResponse> getChanges(
            String status, String createdByCode, String fromDate, String toDate, int page, int size) {

        String statusParam = blankToNull(status);
        String codeParam   = blankToNull(createdByCode);
        LocalDateTime from = parseFromDate(fromDate);
        LocalDateTime to   = parseToDate(toDate);

        Page<ChangeRequest> pageResult = changeRequestRepository
                .findWithFilters(statusParam, codeParam, from, to, PageRequest.of(page, size));

        List<ChangeListItemResponse> content = new ArrayList<>();
        for (ChangeRequest c : pageResult.getContent()) {
            content.add(toListItem(c));
        }

        log.info("[ChangeService] getChanges status={} code={} → {}/{} records",
                statusParam, codeParam, content.size(), pageResult.getTotalElements());

        return PageResponse.<ChangeListItemResponse>builder()
                .content(content)
                .totalElement(pageResult.getTotalElements())
                .totalPage(pageResult.getTotalPages())
                .currentPage(page)
                .pageSize(size)
                .build();
    }

    // ── createChange ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ChangeListItemResponse createChange(CreateChangeRequest req, String createdBy, String createdByCode) {
        validateCreateRequest(req, createdBy);

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

        Long changeId = change.getId();
        log.info("[ChangeService] createChange id={} by={}", changeId, createdBy);

        saveJobs(changeId, req.getJobs(), createdBy, createdByCode, now);
        saveChecklistItems(changeId, req.getChecklistItems(), createdBy, createdByCode, now);
        saveTeamMembers(changeId, req.getTeamMembers(), createdBy, createdByCode, now);
        saveApprovers(changeId, req.getApprovers(), createdBy, createdByCode, now);

        auditLogService.log(changeId, AuditLog.ACTION.CREATED,
                null, ChangeRequest.STATUS.DRAFT, createdBy, createdByCode, null);
        return toListItem(change);
    }

    private void validateCreateRequest(CreateChangeRequest req, String createdBy) {
        identityValidationService.validateUserActive(createdBy);
        if (req.getApprovers() != null) {
            for (CreateChangeRequest.ApproverInput a : req.getApprovers()) {
                identityValidationService.validateUserIsCab(a.getUsername());
            }
        }
        if (req.getChecklistItems() != null) {
            for (CreateChangeRequest.ChecklistInput c : req.getChecklistItems()) {
                if (c.getAssignedTo() != null && !c.getAssignedTo().isBlank()) {
                    identityValidationService.validateUserActive(c.getAssignedTo());
                }
            }
        }
    }

    private void saveJobs(Long changeId, List<CreateChangeRequest.JobInput> inputs,
                          String createdBy, String createdByCode, LocalDateTime now) {
        if (inputs == null || inputs.isEmpty()) return;
        List<GoliveJob> jobs = new ArrayList<>();
        for (CreateChangeRequest.JobInput j : inputs) {
            jobs.add(GoliveJob.builder()
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
                    .build());
        }
        goliveJobRepository.saveAll(jobs);
    }

    private void saveChecklistItems(Long changeId, List<CreateChangeRequest.ChecklistInput> inputs,
                                    String createdBy, String createdByCode, LocalDateTime now) {
        if (inputs == null || inputs.isEmpty()) return;
        List<ChecklistItem> items = new ArrayList<>();
        for (CreateChangeRequest.ChecklistInput c : inputs) {
            items.add(ChecklistItem.builder()
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
                    .build());
        }
        checklistItemRepository.saveAll(items);
    }

    private void saveTeamMembers(Long changeId, List<CreateChangeRequest.TeamMemberInput> inputs,
                                 String createdBy, String createdByCode, LocalDateTime now) {
        if (inputs == null || inputs.isEmpty()) return;
        List<TeamMember> members = new ArrayList<>();
        for (CreateChangeRequest.TeamMemberInput m : inputs) {
            members.add(TeamMember.builder()
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
                    .build());
        }
        teamMemberRepository.saveAll(members);
    }

    private void saveApprovers(Long changeId, List<CreateChangeRequest.ApproverInput> inputs,
                               String createdBy, String createdByCode, LocalDateTime now) {
        if (inputs == null || inputs.isEmpty()) return;
        List<Approver> approvers = new ArrayList<>();
        for (CreateChangeRequest.ApproverInput a : inputs) {
            approvers.add(Approver.builder()
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
                    .build());
        }
        approverRepository.saveAll(approvers);
    }

    // ── getChangeDetail ───────────────────────────────────────────────────────

    @Override
    public ChangeDetailResponse getChangeDetail(Long id) {
        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));
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
                .jobs(mapJobDetails(id))
                .checklistItems(mapChecklistDetails(id))
                .teamMembers(mapTeamMemberDetails(id))
                .approvers(mapApproverDetails(id))
                .documents(mapDocuments(id))
                .auditLogs(auditLogService.getAuditLogs(id))
                .build();
    }

    private List<JobDetail> mapJobDetails(Long changeId) {
        List<GoliveJob> raw = goliveJobRepository.findActiveByChangeRequestId(changeId);
        List<JobDetail> result = new ArrayList<>();
        for (GoliveJob j : raw) {
            result.add(JobDetail.builder()
                    .id(j.getId())
                    .name(j.getName())
                    .link(j.getLink())
                    .jobType(j.getJobType())
                    .orderNum(j.getOrderNum())
                    .jobStatus(j.getJobStatus() != null ? j.getJobStatus() : GoliveJob.JOB_STATUS.PENDING)
                    .startedAt(j.getStartedAt())
                    .completedAt(j.getCompletedAt())
                    .resultNote(j.getResultNote())
                    .createdBy(j.getCreatedBy())
                    .createdByCode(j.getCreatedByCode())
                    .createdAt(j.getCreatedAt())
                    .build());
        }
        return result;
    }

    private List<ChecklistDetail> mapChecklistDetails(Long changeId) {
        List<ChecklistItem> raw = checklistItemRepository.findActiveByChangeRequestId(changeId);
        List<ChecklistDetail> result = new ArrayList<>();
        for (ChecklistItem c : raw) {
            result.add(ChecklistDetail.builder()
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
                    .build());
        }
        return result;
    }

    private List<TeamMemberDetail> mapTeamMemberDetails(Long changeId) {
        List<TeamMember> raw = teamMemberRepository.findActiveByChangeRequestId(changeId);
        List<TeamMemberDetail> result = new ArrayList<>();
        for (TeamMember m : raw) {
            result.add(TeamMemberDetail.builder()
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
                    .build());
        }
        return result;
    }

    private List<ApproverDetail> mapApproverDetails(Long changeId) {
        List<Approver> raw = approverRepository.findActiveByChangeRequestId(changeId);
        List<ApproverDetail> result = new ArrayList<>();
        for (Approver a : raw) {
            result.add(ApproverDetail.builder()
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
                    .build());
        }
        return result;
    }

    private List<DocumentResponse> mapDocuments(Long changeId) {
        List<Document> raw = documentRepository.findByChangeRequestIdAndStatusOrderByCreatedAtDesc(changeId, 1);
        List<DocumentResponse> result = new ArrayList<>();
        for (Document d : raw) {
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
            result.add(r);
        }
        return result;
    }

    // ── updateChange ──────────────────────────────────────────────────────────

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

        validateUpdateRequest(req, updatedBy);

        LocalDateTime now = LocalDateTime.now();
        change.setChangeName(req.getChangeName());
        change.setContent(req.getContent());
        change.setGitLink(req.getGitLink());
        change.setGoliveAt(req.getGoliveAt());
        change.setUpdatedAt(now);
        changeRequestRepository.save(change);

        syncJobs(id, req.getJobs(), updatedBy, updatedByCode, now);
        syncChecklistItems(id, req.getChecklistItems(), updatedBy, updatedByCode, now);
        syncTeamMembers(id, req.getTeamMembers(), updatedBy, updatedByCode, now);
        syncApprovers(id, req.getApprovers(), updatedBy, updatedByCode, now);

        log.info("[ChangeService] updateChange id={} done", id);
    }

    private void validateUpdateRequest(UpdateChangeRequest req, String updatedBy) {
        identityValidationService.validateUserActive(updatedBy);
        if (req.getApprovers() != null) {
            for (UpdateChangeRequest.ApproverInput a : req.getApprovers()) {
                if (!isBeingDeleted(a.getStatus())) {
                    identityValidationService.validateUserIsCab(a.getUsername());
                }
            }
        }
        if (req.getChecklistItems() != null) {
            for (UpdateChangeRequest.ChecklistInput c : req.getChecklistItems()) {
                if (!isBeingDeleted(c.getStatus()) && c.getAssignedTo() != null && !c.getAssignedTo().isBlank()) {
                    identityValidationService.validateUserActive(c.getAssignedTo());
                }
            }
        }
    }

    private void syncJobs(Long changeId, List<UpdateChangeRequest.JobInput> inputs,
                          String updatedBy, String updatedByCode, LocalDateTime now) {
        if (inputs == null) return;
        for (UpdateChangeRequest.JobInput input : inputs) {
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
                        .changeRequestId(changeId)
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

    private void syncChecklistItems(Long changeId, List<UpdateChangeRequest.ChecklistInput> inputs,
                                    String updatedBy, String updatedByCode, LocalDateTime now) {
        if (inputs == null) return;
        for (UpdateChangeRequest.ChecklistInput input : inputs) {
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
                        .changeRequestId(changeId)
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

    private void syncTeamMembers(Long changeId, List<UpdateChangeRequest.TeamMemberInput> inputs,
                                 String updatedBy, String updatedByCode, LocalDateTime now) {
        if (inputs == null) return;
        for (UpdateChangeRequest.TeamMemberInput input : inputs) {
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
                        .changeRequestId(changeId)
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

    private void syncApprovers(Long changeId, List<UpdateChangeRequest.ApproverInput> inputs,
                               String updatedBy, String updatedByCode, LocalDateTime now) {
        if (inputs == null) return;
        for (UpdateChangeRequest.ApproverInput input : inputs) {
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
                        .changeRequestId(changeId)
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

    // ── submitChange ──────────────────────────────────────────────────────────

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

        List<TeamMember> teamMembers = teamMemberRepository.findActiveByChangeRequestId(id);
        validateNoConflictWithApprovers(submittedBy, approvers, teamMembers);
        resetApproversForNewCycle(approvers);

        change.setStatus(ChangeRequest.STATUS.PENDING);
        change.setUpdatedAt(LocalDateTime.now());
        changeRequestRepository.save(change);

        auditLogService.log(id, AuditLog.ACTION.SUBMITTED,
                ChangeRequest.STATUS.DRAFT, ChangeRequest.STATUS.PENDING, submittedBy, submittedByCode, null);
        log.info("[ChangeService] submitChange id={} → PENDING, {} CAB approvers reset", id, approvers.size());
    }

    private void validateNoConflictWithApprovers(String submittedBy, List<Approver> approvers, List<TeamMember> teamMembers) {
        Set<String> approverUsernames = new HashSet<>();
        for (Approver a : approvers) {
            approverUsernames.add(a.getUsername().toLowerCase());
        }
        if (approverUsernames.contains(submittedBy.toLowerCase())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Người tạo change không được là CAB approver: " + submittedBy);
        }
        for (TeamMember m : teamMembers) {
            if (approverUsernames.contains(m.getUsername().toLowerCase())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Thành viên nhóm '" + m.getUsername() + "' không được là CAB approver");
            }
        }
    }

    private void resetApproversForNewCycle(List<Approver> approvers) {
        for (Approver a : approvers) {
            a.setApproveStatus(Approver.APPROVE_STATUS.PENDING);
            a.setNote(null);
            a.setDecidedAt(null);
        }
        approverRepository.saveAll(approvers);
    }

    private void assertNotSelfApproval(ChangeRequest change, String approverUsername, String approverCode) {
        boolean sameCode = approverCode != null && approverCode.equalsIgnoreCase(change.getCreatedByCode());
        boolean sameUsername = approverUsername != null && approverUsername.equalsIgnoreCase(change.getCreatedBy());
        if (sameCode || sameUsername) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Người tạo change request không thể tự phê duyệt hoặc từ chối yêu cầu của chính mình");
        }
    }

    // ── approveChange ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void approveChange(Long id, String approverUsername, String approverCode, String note) {
        log.info("[ChangeService] approveChange id={} by={}/{}", id, approverUsername, approverCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.PENDING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái PENDING");
        }

        // Separation of duties: người tạo change request không được tự phê duyệt
        assertNotSelfApproval(change, approverUsername, approverCode);

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
            auditLogService.log(id, AuditLog.ACTION.APPROVED,
                    ChangeRequest.STATUS.PENDING, ChangeRequest.STATUS.APPROVED, approverUsername, approverCode, note);
            log.info("[ChangeService] approveChange id={} → tất cả CAB approved → APPROVED", id);
        } else {
            log.info("[ChangeService] approveChange id={} by={} approved, còn {} CAB chưa duyệt", id, approverUsername, pendingCount);
        }
    }

    // ── rejectChange ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void rejectChange(Long id, String approverUsername, String approverCode, String note) {
        log.info("[ChangeService] rejectChange id={} by={}/{}", id, approverUsername, approverCode);

        ChangeRequest change = changeRequestRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + id));

        if (!ChangeRequest.STATUS.PENDING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái PENDING");
        }

        // Separation of duties: người tạo change request không được tự quyết định
        assertNotSelfApproval(change, approverUsername, approverCode);

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

        change.setStatus(ChangeRequest.STATUS.DRAFT);
        change.setUpdatedAt(now);
        changeRequestRepository.save(change);

        auditLogService.log(id, AuditLog.ACTION.REJECTED,
                ChangeRequest.STATUS.PENDING, ChangeRequest.STATUS.DRAFT, approverUsername, approverCode, note);
        log.info("[ChangeService] rejectChange id={} by={} → DRAFT", id, approverUsername);
    }

    // ── executeChange ─────────────────────────────────────────────────────────

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

        auditLogService.log(id, AuditLog.ACTION.EXECUTED,
                ChangeRequest.STATUS.APPROVED, ChangeRequest.STATUS.EXECUTING, executedBy, executedByCode, null);
        log.info("[ChangeService] executeChange id={} by={} → EXECUTING", id, executedBy);
    }

    // ── updateChecklistItemStatus ─────────────────────────────────────────────

    @Override
    @Transactional
    public void updateChecklistItemStatus(Long changeId, Long itemId, String taskStatus, String username) {
        log.info("[ChangeService] updateChecklistItemStatus changeId={} itemId={} status={} by={}",
                changeId, itemId, taskStatus, username);

        if (!ChecklistItem.TASK_STATUS.SUCCESS.equals(taskStatus)
                && !ChecklistItem.TASK_STATUS.FAIL.equals(taskStatus)) {
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

    // ── finalizeResult ────────────────────────────────────────────────────────

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

        validateAllJobsFinished(changeId);
        String finalStatus = determineFinalStatus(changeId);
        applyFinalResult(change, changeId, finalStatus, username, userCode);
    }

    private void validateAllJobsFinished(Long changeId) {
        int pending = goliveJobRepository.countActiveByChangeRequestIdAndJobStatus(changeId, GoliveJob.JOB_STATUS.PENDING);
        int running = goliveJobRepository.countActiveByChangeRequestIdAndJobStatus(changeId, GoliveJob.JOB_STATUS.RUNNING);
        if (pending > 0 || running > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Còn " + (pending + running) + " job chưa hoàn thành. Hãy chạy hết các job trước khi chốt kết quả.");
        }
    }

    private String determineFinalStatus(Long changeId) {
        int failJobs = goliveJobRepository.countActiveByChangeRequestIdAndJobStatus(changeId, GoliveJob.JOB_STATUS.FAIL);
        if (failJobs > 0) return ChangeRequest.STATUS.FAIL;

        int failChecklist = checklistItemRepository.countActiveByChangeRequestIdAndTaskStatus(changeId, ChecklistItem.TASK_STATUS.FAIL);
        if (failChecklist > 0) return ChangeRequest.STATUS.FAIL;

        int readyCount = checklistItemRepository.countActiveByChangeRequestIdAndTaskStatus(changeId, ChecklistItem.TASK_STATUS.READY);
        if (readyCount > 0) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Còn " + readyCount + " bước trong checklist chưa được cập nhật trạng thái");
        }

        return ChangeRequest.STATUS.SUCCESS;
    }

    private void applyFinalResult(ChangeRequest change, Long changeId,
                                  String finalStatus, String username, String userCode) {
        change.setStatus(finalStatus);
        change.setUpdatedAt(LocalDateTime.now());
        changeRequestRepository.save(change);

        String note = ChangeRequest.STATUS.SUCCESS.equals(finalStatus)
                ? "Tất cả jobs và checklist thành công"
                : "Có job hoặc checklist thất bại";

        auditLogService.log(changeId, AuditLog.ACTION.FINALIZED,
                ChangeRequest.STATUS.EXECUTING, finalStatus, username, userCode, note);
        log.info("[ChangeService] finalizeResult changeId={} → {}", changeId, finalStatus);
    }

    // ── runJob ────────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public GoliveJobResponse runJob(Long changeId, Long jobId, String username, String userCode) {
        log.info("[ChangeService] runJob changeId={} jobId={} by={}/{}", changeId, jobId, username, userCode);

        ChangeRequest change = changeRequestRepository.findById(changeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy change request id=" + changeId));

        if (!ChangeRequest.STATUS.EXECUTING.equals(change.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Change request không ở trạng thái EXECUTING");
        }

        GoliveJob job = goliveJobRepository.findById(jobId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Không tìm thấy job id=" + jobId));

        if (!changeId.equals(job.getChangeRequestId())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Job không thuộc change request này");
        }
        if (job.getJobStatus() != null && !GoliveJob.JOB_STATUS.PENDING.equals(job.getJobStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, "Job đã được chạy, trạng thái: " + job.getJobStatus());
        }

        simulateJobExecution(job);
        goliveJobRepository.save(job);

        auditLogService.log(changeId, AuditLog.ACTION.JOB_RUN,
                ChangeRequest.STATUS.EXECUTING, ChangeRequest.STATUS.EXECUTING, username, userCode,
                "Job #" + job.getOrderNum() + " [" + job.getJobType() + "] " + job.getName() + " → " + job.getJobStatus());

        log.info("[ChangeService] runJob changeId={} jobId={} → {}", changeId, jobId, job.getJobStatus());
        return toJobResponse(job);
    }

    private void simulateJobExecution(GoliveJob job) {
        job.setJobStatus(GoliveJob.JOB_STATUS.RUNNING);
        job.setStartedAt(LocalDateTime.now());

        try {
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean success = Math.random() > 0.2; // 80% SUCCESS
        job.setJobStatus(success ? GoliveJob.JOB_STATUS.SUCCESS : GoliveJob.JOB_STATUS.FAIL);
        job.setCompletedAt(LocalDateTime.now());
        job.setResultNote(success
                ? "Job completed successfully"
                : "Job failed: execution error, please check logs");
        job.setUpdatedAt(LocalDateTime.now());
    }

    private GoliveJobResponse toJobResponse(GoliveJob job) {
        GoliveJobResponse resp = new GoliveJobResponse();
        resp.setId(job.getId());
        resp.setChangeRequestId(job.getChangeRequestId());
        resp.setName(job.getName());
        resp.setLink(job.getLink());
        resp.setJobType(job.getJobType());
        resp.setOrderNum(job.getOrderNum());
        resp.setJobStatus(job.getJobStatus());
        resp.setStartedAt(job.getStartedAt());
        resp.setCompletedAt(job.getCompletedAt());
        resp.setResultNote(job.getResultNote());
        resp.setStatus(job.getStatus());
        return resp;
    }

    // ── private helpers ───────────────────────────────────────────────────────

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

    private static LocalDateTime parseFromDate(String fromDate) {
        if (fromDate == null || fromDate.isBlank()) return null;
        return LocalDate.parse(fromDate).atStartOfDay();
    }

    private static LocalDateTime parseToDate(String toDate) {
        if (toDate == null || toDate.isBlank()) return null;
        return LocalDate.parse(toDate).atTime(23, 59, 59);
    }

    private static String blankToNull(String val) {
        return (val != null && !val.isBlank()) ? val : null;
    }

    private static boolean isBeingDeleted(Integer status) {
        return status != null && status == 0;
    }
}
