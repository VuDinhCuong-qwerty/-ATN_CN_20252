import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ChangeService } from '../../shared/services/change.service';
import { PermissionService } from '../../core/auth/permission.service';
import {
  ChangeRequest, ChecklistItem, Approver, Phase,
  GoliveJob, TeamMember
} from '../../shared/models/change.model';
import { TeamSlot, ApproverSlot, ChecklistStep } from '../change-form/change-form.component';

type EditStep = ChecklistStep & { status?: number };
type EditApproverSlot = ApproverSlot & { status?: number };

@Component({
  selector: 'app-change-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './change-detail.component.html',
  styleUrl: './change-detail.component.css'
})
export class ChangeDetailComponent implements OnInit {
  cr: ChangeRequest | null = null;
  loading = false;
  error = '';
  actionError = '';
  actionSuccess = '';

  // Approve/reject modal
  showApproveModal = false;
  approveNote = '';
  showRejectModal = false;
  rejectNote = '';

  // Execute / result modal
  showExecuteConfirm = false;
  showResultModal = false;

  acting = false;
  updatingItemId: number | null = null;
  activePhase: Phase = 'PRE';

  // ── Edit mode ─────────────────────────────────────────────────────────────
  editMode = false;
  saving = false;
  editError = '';

  editName = '';
  editContent = '';
  editGitLink = '';
  editGoliveAt = '';
  editJobs: GoliveJob[] = [];
  editPreList: EditStep[] = [];
  editDuringList: EditStep[] = [];
  editRollbackList: EditStep[] = [];
  editActivePhase: Phase = 'PRE';
  editDevSlot: TeamSlot    = this.emptySlot();
  editTesterSlot: TeamSlot = this.emptySlot();
  editBaSlot: TeamSlot     = this.emptySlot();
  editApproverSlots: EditApproverSlot[] = [];
  showCancelConfirm = false;

  readonly JOB_TYPES: { value: string; label: string }[] = [
    { value: 'DEPLOY', label: 'Deploy' },
    { value: 'MERGE',  label: 'Merge code' },
    { value: 'BUILD',  label: 'Build & Test' }
  ];

  readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  readonly TASK_STATUS_LABELS: Record<string, string> = {
    READY: 'Chưa thực hiện', SUCCESS: 'Hoàn thành', FAIL: 'Thất bại'
  };

  readonly JOB_TYPE_LABELS: Record<string, string> = {
    MERGE: 'Merge code', BUILD: 'Build & Test', DEPLOY: 'Deploy'
  };

  readonly MEMBER_LABELS: Record<string, string> = {
    DEV: 'Developer', TESTER: 'Tester', BA: 'Business Analyst'
  };

  constructor(
    private svc: ChangeService,
    public perm: PermissionService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) this.load(+id);
  }

  load(id: number) {
    this.loading = true; this.error = '';
    this.svc.getChangeDetail(id).subscribe({
      next: cr => { this.cr = cr; this.loading = false; },
      error: () => { this.error = 'Không thể tải chi tiết.'; this.loading = false; }
    });
  }

  reload() { if (this.cr?.id) this.load(this.cr.id); }

  // ── Computed permissions ──────────────────────────────────────────────────

  get canEdit(): boolean {
    return !!(this.perm.has('update') && this.cr?.status === 'DRAFT' && this.cr?.createdBy === this.perm.username);
  }

  get canSubmit(): boolean {
    return !!(this.perm.has('update') && this.cr?.status === 'DRAFT' && this.cr?.createdBy === this.perm.username);
  }

  get canApprove(): boolean {
    if (!this.perm.has('approve') || this.cr?.status !== 'PENDING') return false;
    const myCode = this.perm.employeeCode;
    return !!(myCode && this.cr?.approvers?.some(a => a.employeeCode === myCode));
  }

  get canExecute(): boolean {
    return !!(this.perm.has('execute') && this.cr?.status === 'APPROVED' && this.cr?.createdBy === this.perm.username);
  }

  get canUpdateResult(): boolean {
    return !!(this.perm.has('execute') && this.cr?.status === 'EXECUTING' && this.cr?.createdByCode === this.perm.employeeCode);
  }

  // ── View-mode checklist helpers ───────────────────────────────────────────

  get checklistByPhase(): ChecklistItem[] {
    return (this.cr?.checklistItems || [])
      .filter(c => c.phase === this.activePhase)
      .sort((a, b) => a.orderNum - b.orderNum);
  }

  get preCount()      { return (this.cr?.checklistItems || []).filter(c => c.phase === 'PRE').length; }
  get duringCount()   { return (this.cr?.checklistItems || []).filter(c => c.phase === 'DURING').length; }
  get rollbackCount() { return (this.cr?.checklistItems || []).filter(c => c.phase === 'ROLLBACK').length; }

  canUpdateItem(item: ChecklistItem): boolean {
    return !!(
      this.perm.has('update') &&
      this.cr?.status === 'EXECUTING' &&
      (!item.taskStatus || item.taskStatus === 'READY') &&
      item.assignedToCode === this.perm.employeeCode
    );
  }

  updateChecklistItem(item: ChecklistItem, taskStatus: 'SUCCESS' | 'FAIL') {
    if (!this.cr?.id || !item.id) return;
    this.updatingItemId = item.id;
    this.svc.updateChecklistItemStatus(this.cr.id, item.id, taskStatus).subscribe({
      next: () => { this.updatingItemId = null; this.reload(); },
      error: e => {
        this.updatingItemId = null;
        this.actionError = e?.error?.errorDesc || e?.error?.message || 'Không thể cập nhật checklist.';
      }
    });
  }

  // ── Edit-mode helpers ─────────────────────────────────────────────────────

  private emptySlot(): TeamSlot {
    return { query: '', result: null, searching: false, notFound: false };
  }

  private emptyStep(): ChecklistStep {
    return {
      stepText: '', assigneeQuery: '', assigneeResult: null,
      searchingAssignee: false, notFoundAssignee: false
    };
  }

  enterEditMode() {
    if (!this.cr) return;
    this.editName     = this.cr.changeName;
    this.editContent  = this.cr.content  || '';
    this.editGitLink  = this.cr.gitLink  || '';
    this.editGoliveAt = this.cr.goliveAt ? this.cr.goliveAt.substring(0, 16) : '';

    this.editJobs = (this.cr.jobs || []).map(j => ({ ...j }));

    this.editPreList = []; this.editDuringList = []; this.editRollbackList = [];
    (this.cr.checklistItems || []).forEach(item => {
      const step: ChecklistStep = {
        id: item.id, stepText: item.stepText,
        assignedTo: item.assignedTo, assignedToCode: item.assignedToCode,
        assigneeQuery: item.assignedTo || '',
        assigneeResult: item.assignedTo ? {
          userId: 0, username: item.assignedTo,
          fullName: item.assignedTo, employeeCode: item.assignedToCode || ''
        } : null,
        searchingAssignee: false, notFoundAssignee: false
      };
      if (item.phase === 'PRE') this.editPreList.push(step);
      else if (item.phase === 'DURING') this.editDuringList.push(step);
      else this.editRollbackList.push(step);
    });

    this.editDevSlot = this.emptySlot();
    this.editTesterSlot = this.emptySlot();
    this.editBaSlot = this.emptySlot();
    (this.cr.teamMembers || []).forEach(m => {
      const slot: TeamSlot = {
        id: m.id, query: m.username,
        result: {
          userId: Number(m.userId) || 0, username: m.username,
          fullName: m.fullName, employeeCode: m.employeeCode || ''
        },
        searching: false, notFound: false
      };
      if (m.memberRole === 'DEV')         this.editDevSlot    = slot;
      else if (m.memberRole === 'TESTER') this.editTesterSlot = slot;
      else                                this.editBaSlot     = slot;
    });

    this.editApproverSlots = (this.cr.approvers || []).map(a => ({
      id: a.id, query: a.username,
      result: {
        userId: Number(a.userId) || 0, username: a.username,
        fullName: a.fullName, employeeCode: a.employeeCode
      },
      searching: false, notFound: false
    }));

    this.editActivePhase = 'PRE';
    this.editError = '';
    this.editMode = true;
    this.actionSuccess = '';
    this.actionError = '';
  }

  exitEditMode() { this.editMode = false; this.editError = ''; this.showCancelConfirm = false; }
  cancelEdit()          { this.showCancelConfirm = true; }
  confirmCancelEdit()   { this.exitEditMode(); }
  dismissCancelConfirm(){ this.showCancelConfirm = false; }

  // Edit jobs
  addEditJob() {
    this.editJobs.push({ name: '', link: '', jobType: 'DEPLOY', orderNum: this.editJobs.length + 1 });
  }
  removeEditJob(job: GoliveJob) {
    if (job.id) { job.status = 0; }
    else { this.editJobs.splice(this.editJobs.indexOf(job), 1); }
  }

  // Edit checklist
  get editActiveJobs(): GoliveJob[]          { return this.editJobs.filter(j => j.status !== 0); }
  get editActiveApprovers(): EditApproverSlot[] { return this.editApproverSlots.filter(s => s.status !== 0); }
  get editActiveChecklist(): EditStep[] {
    const list = this.editActivePhase === 'PRE' ? this.editPreList
      : this.editActivePhase === 'DURING' ? this.editDuringList
      : this.editRollbackList;
    return list.filter(s => s.status !== 0);
  }
  get editPreCount()      { return this.editPreList.filter(s => s.status !== 0 && s.stepText.trim()).length; }
  get editDuringCount()   { return this.editDuringList.filter(s => s.status !== 0 && s.stepText.trim()).length; }
  get editRollbackCount() { return this.editRollbackList.filter(s => s.status !== 0 && s.stepText.trim()).length; }

  addEditStep() {
    const step = this.emptyStep();
    if (this.editActivePhase === 'PRE')         this.editPreList.push(step);
    else if (this.editActivePhase === 'DURING') this.editDuringList.push(step);
    else                                        this.editRollbackList.push(step);
  }
  removeEditStep(step: EditStep) {
    if (step.id) {
      step.status = 0;
    } else {
      const list = this.editActivePhase === 'PRE' ? this.editPreList
        : this.editActivePhase === 'DURING' ? this.editDuringList
        : this.editRollbackList;
      list.splice(list.indexOf(step), 1);
    }
  }
  searchEditAssignee(step: EditStep) {
    if (!step.assigneeQuery.trim()) return;
    step.searchingAssignee = true; step.notFoundAssignee = false; step.assigneeResult = null;
    this.svc.searchUser(step.assigneeQuery.trim()).subscribe({
      next: res => { step.assigneeResult = res?.[0] ?? null; step.notFoundAssignee = !step.assigneeResult; step.searchingAssignee = false; },
      error: () => { step.notFoundAssignee = true; step.searchingAssignee = false; }
    });
  }
  clearEditAssignee(step: EditStep) {
    step.assigneeQuery = ''; step.assigneeResult = null; step.notFoundAssignee = false;
  }

  // Edit team
  searchEditSlot(slot: TeamSlot) {
    if (!slot.query.trim()) return;
    slot.searching = true; slot.notFound = false; slot.result = null;
    this.svc.searchUser(slot.query.trim()).subscribe({
      next: res => { slot.result = res?.[0] ?? null; slot.notFound = !slot.result; slot.searching = false; },
      error: () => { slot.notFound = true; slot.searching = false; }
    });
  }
  clearEditSlot(slot: TeamSlot) { slot.query = ''; slot.result = null; slot.notFound = false; }

  // Edit approvers
  addEditApprover() {
    this.editApproverSlots.push({ query: '', result: null, searching: false, notFound: false });
  }
  removeEditApprover(slot: EditApproverSlot) {
    if (slot.id) { slot.status = 0; }
    else { this.editApproverSlots.splice(this.editApproverSlots.indexOf(slot), 1); }
  }
  searchEditApprover(slot: EditApproverSlot) {
    if (!slot.query.trim()) return;
    slot.searching = true; slot.notFound = false; slot.result = null;
    this.svc.searchUser(slot.query.trim()).subscribe({
      next: res => { slot.result = res?.[0] ?? null; slot.notFound = !slot.result; slot.searching = false; },
      error: () => { slot.notFound = true; slot.searching = false; }
    });
  }

  private buildEditChecklist(list: EditStep[], phase: Phase): ChecklistItem[] {
    const active  = list.filter(s => s.status !== 0 && s.stepText.trim());
    const deleted = list.filter(s => s.status === 0 && s.id);
    return [
      ...active.map((s, i) => ({
        id: s.id, phase, stepText: s.stepText.trim(), orderNum: i + 1,
        assignedTo:     s.assigneeResult?.username || s.assignedTo || undefined,
        assignedToCode: s.assigneeResult?.employeeCode || s.assignedToCode || undefined,
        status: 1 as number
      })),
      ...deleted.map(s => ({ id: s.id, phase, stepText: s.stepText, orderNum: 0, status: 0 as number }))
    ];
  }

  saveEdit() {
    if (!this.editName.trim()) { this.editError = 'Tên change là bắt buộc.'; return; }
    if (!this.cr?.id) return;
    this.saving = true; this.editError = '';

    // Team members — gửi status:0 nếu slot có id nhưng result bị xóa
    const teamMembers: TeamMember[] = [];
    const addSlot = (slot: TeamSlot, role: 'DEV' | 'TESTER' | 'BA', isLead: number) => {
      if (slot.result) {
        teamMembers.push({
          id: slot.id, userId: String(slot.result.userId),
          username: slot.result.username, fullName: slot.result.fullName,
          employeeCode: slot.result.employeeCode, memberRole: role, isLead, status: 1
        });
      } else if (slot.id) {
        teamMembers.push({ id: slot.id, username: '', fullName: '', memberRole: role, isLead, status: 0 });
      }
    };
    addSlot(this.editDevSlot,    'DEV',    1);
    addSlot(this.editTesterSlot, 'TESTER', 0);
    addSlot(this.editBaSlot,     'BA',     0);

    // Approvers — active + deleted (status:0 cho item có id)
    const approvers: Approver[] = [
      ...this.editActiveApprovers
        .filter(s => s.result)
        .map(s => ({
          id: s.id, userId: String(s.result!.userId),
          username: s.result!.username, fullName: s.result!.fullName,
          employeeCode: s.result!.employeeCode, status: 1
        })),
      ...this.editApproverSlots
        .filter(s => s.status === 0 && s.id)
        .map(s => ({ id: s.id, username: s.query || '', fullName: '', employeeCode: '', status: 0 }))
    ];

    // Jobs — active (re-ordered) + deleted (status:0, chỉ những có id)
    const jobs: GoliveJob[] = [
      ...this.editActiveJobs
        .filter(j => j.name.trim())
        .map((j, i) => ({ ...j, orderNum: i + 1, status: 1 })),
      ...this.editJobs
        .filter(j => j.status === 0 && j.id)
        .map(j => ({ ...j, status: 0 }))
    ];

    const body = {
      changeName: this.editName.trim(),
      content:    this.editContent,
      gitLink:    this.editGitLink,
      goliveAt:   this.editGoliveAt || null,
      jobs,
      checklistItems: [
        ...this.buildEditChecklist(this.editPreList,      'PRE'),
        ...this.buildEditChecklist(this.editDuringList,   'DURING'),
        ...this.buildEditChecklist(this.editRollbackList, 'ROLLBACK')
      ],
      teamMembers,
      approvers
    };

    this.svc.updateChange(this.cr.id, body).subscribe({
      next: () => {
        this.saving = false;
        this.editMode = false;
        this.actionSuccess = 'Đã lưu thay đổi thành công.';
        this.reload();
      },
      error: e => {
        this.saving = false;
        this.editError = e?.error?.errorDesc || e?.error?.message || 'Không thể lưu. Vui lòng thử lại.';
      }
    });
  }

  // ── Actions ───────────────────────────────────────────────────────────────

  submit() {
    if (!this.cr?.id) return;
    this.acting = true; this.actionError = ''; this.actionSuccess = '';
    this.svc.submitChange(this.cr.id).subscribe({
      next: () => { this.actionSuccess = 'Đã gửi duyệt thành công.'; this.reload(); this.acting = false; },
      error: e => { this.actionError = e?.error?.errorDesc || e?.error?.message || 'Không thể gửi duyệt.'; this.acting = false; }
    });
  }

  confirmApprove() {
    if (!this.cr?.id) return;
    this.acting = true; this.actionError = ''; this.actionSuccess = '';
    this.svc.approveChange(this.cr.id, this.approveNote).subscribe({
      next: () => {
        this.actionSuccess = 'Đã phê duyệt thành công.';
        this.showApproveModal = false; this.approveNote = '';
        this.reload(); this.acting = false;
      },
      error: e => { this.actionError = e?.error?.errorDesc || e?.error?.message || 'Không thể phê duyệt.'; this.acting = false; }
    });
  }

  confirmReject() {
    if (!this.cr?.id) return;
    this.acting = true; this.actionError = ''; this.actionSuccess = '';
    this.svc.rejectChange(this.cr.id, this.rejectNote).subscribe({
      next: () => {
        this.actionSuccess = 'Đã từ chối.';
        this.showRejectModal = false; this.rejectNote = '';
        this.reload(); this.acting = false;
      },
      error: e => { this.actionError = e?.error?.errorDesc || e?.error?.message || 'Không thể từ chối.'; this.acting = false; }
    });
  }

  confirmExecute() {
    if (!this.cr?.id) return;
    this.acting = true; this.actionError = ''; this.actionSuccess = '';
    this.svc.executeChange(this.cr.id).subscribe({
      next: () => {
        this.actionSuccess = 'Đã bắt đầu go-live.';
        this.showExecuteConfirm = false;
        this.reload(); this.acting = false;
      },
      error: e => { this.actionError = e?.error?.errorDesc || e?.error?.message || 'Không thể thực thi.'; this.acting = false; }
    });
  }

  confirmResult() {
    if (!this.cr?.id) return;
    this.acting = true; this.actionError = ''; this.actionSuccess = '';
    const id = this.cr.id;
    this.svc.finalizeResult(id).subscribe({
      next: () => {
        this.showResultModal = false;
        this.acting = false;
        // Reload trước, sau đó đọc cr.status thực tế từ server
        this.load(id);
        // Dùng setTimeout để chờ load xong rồi hiển thị thông báo
        setTimeout(() => {
          const label = this.cr?.status === 'SUCCESS' ? 'Thành công' : 'Thất bại';
          this.actionSuccess = `Kết quả go-live: ${label}.`;
        }, 600);
      },
      error: e => { this.actionError = e?.error?.errorDesc || e?.error?.message || 'Không thể ghi nhận kết quả.'; this.acting = false; }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  fmtDate(dt?: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  shortId(id?: string) { return id ? id.substring(0, 8).toUpperCase() : '—'; }

  approverStatusLabel(a: Approver): string {
    const map: Record<string, string> = { PENDING: 'Chờ duyệt', APPROVED: 'Đã duyệt', REJECTED: 'Từ chối' };
    return map[a.approveStatus || ''] || a.approveStatus || '—';
  }

  approverStatusClass(a: Approver): string {
    const map: Record<string, string> = { PENDING: 'PENDING', APPROVED: 'APPROVED', REJECTED: 'REJECTED' };
    return 'badge-' + (map[a.approveStatus || ''] || 'PENDING') + '-sm';
  }

  taskStatusClass(ts?: string): string {
    const map: Record<string, string> = { READY: 'PENDING', SUCCESS: 'APPROVED', FAIL: 'REJECTED' };
    return 'badge-' + (map[ts || 'READY'] || 'PENDING') + '-sm';
  }
}
