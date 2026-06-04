import { Component, OnInit } from '@angular/core';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ChangeService } from '../../shared/services/change.service';
import { PermissionService } from '../../core/auth/permission.service';
import {
  GoliveJob, ChecklistItem, TeamMember,
  Approver, Phase, UserSearchResult, JobType
} from '../../shared/models/change.model';

export interface TeamSlot {
  id?: number;   // DB id of CHG_TEAM_MEMBER row — needed for PUT diff update
  query: string;
  result: UserSearchResult | null;
  searching: boolean;
  notFound: boolean;
}

export interface ApproverSlot {
  id?: number;   // DB id of CHG_APPROVER row — needed for PUT diff update
  query: string;
  result: UserSearchResult | null;
  searching: boolean;
  notFound: boolean;
}

export interface ChecklistStep {
  id?: number;
  stepText: string;
  assignedTo?: string;
  assignedToCode?: string;
  // UI search state
  assigneeQuery: string;
  assigneeResult: UserSearchResult | null;
  searchingAssignee: boolean;
  notFoundAssignee: boolean;
}

@Component({
  selector: 'app-change-form',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './change-form.component.html',
  styleUrl: './change-form.component.css'
})
export class ChangeFormComponent implements OnInit {
  editId: number | null = null;
  isEdit = false;
  loading = false;
  saving = false;
  error = '';

  // Section 1
  changeName = '';
  content = '';
  gitLink = '';
  goliveAt = '';

  // Section 2 – golive jobs
  jobs: GoliveJob[] = [];

  // Section 3 – checklist
  activePhase: Phase = 'PRE';
  preList: ChecklistStep[] = [];
  duringList: ChecklistStep[] = [];
  rollbackList: ChecklistStep[] = [];

  // Section 4 – team members
  devSlot: TeamSlot    = this.emptySlot();
  testerSlot: TeamSlot = this.emptySlot();
  baSlot: TeamSlot     = this.emptySlot();

  // Section 5 – CAB approvers
  approverSlots: ApproverSlot[] = [];

  readonly JOB_TYPES: { value: JobType; label: string }[] = [
    { value: 'DEPLOY', label: 'Deploy' },
    { value: 'MERGE',  label: 'Merge code' },
    { value: 'BUILD',  label: 'Build & Test' }
  ];

  constructor(
    private svc: ChangeService,
    public perm: PermissionService,
    private router: Router,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id) { this.editId = +id; this.isEdit = true; this.loadEdit(+id); }
  }

  private emptySlot(): TeamSlot {
    return { query: '', result: null, searching: false, notFound: false };
  }

  private emptyStep(): ChecklistStep {
    return { stepText: '', assigneeQuery: '', assigneeResult: null, searchingAssignee: false, notFoundAssignee: false };
  }

  loadEdit(id: number) {
    this.loading = true;
    this.svc.getChangeDetail(id).subscribe({
      next: cr => {
        this.changeName = cr.changeName;
        this.content    = cr.content   || '';
        this.gitLink    = cr.gitLink   || '';
        this.goliveAt   = cr.goliveAt  ? cr.goliveAt.substring(0, 16) : '';

        this.jobs = (cr.jobs || []).map(j => ({ ...j }));

        (cr.checklistItems || []).forEach(item => {
          const step: ChecklistStep = {
            id: item.id,
            stepText: item.stepText,
            assignedTo: item.assignedTo,
            assignedToCode: item.assignedToCode,
            assigneeQuery: item.assignedTo || '',
            assigneeResult: item.assignedTo ? {
              userId: 0,
              username: item.assignedTo,
              fullName: item.assignedTo,
              employeeCode: item.assignedToCode || ''
            } : null,
            searchingAssignee: false,
            notFoundAssignee: false
          };
          if (item.phase === 'PRE') this.preList.push(step);
          else if (item.phase === 'DURING') this.duringList.push(step);
          else this.rollbackList.push(step);
        });

        (cr.teamMembers || []).forEach(m => {
          const slot: TeamSlot = {
            id: m.id,   // preserve DB id for PUT diff
            query: m.username,
            result: {
              userId: Number(m.userId) || 0,
              username: m.username,
              fullName: m.fullName,
              employeeCode: m.employeeCode || ''
            },
            searching: false, notFound: false
          };
          if (m.memberRole === 'DEV') this.devSlot = slot;
          else if (m.memberRole === 'TESTER') this.testerSlot = slot;
          else this.baSlot = slot;
        });

        this.approverSlots = (cr.approvers || []).map(a => ({
          id: a.id,   // preserve DB id for PUT diff
          query: a.username,
          result: {
            userId: Number(a.userId) || 0,
            username: a.username,
            fullName: a.fullName,
            employeeCode: a.employeeCode
          },
          searching: false, notFound: false
        }));
        this.loading = false;
      },
      error: () => { this.loading = false; this.error = 'Không thể tải dữ liệu.'; }
    });
  }

  // ── Section 2 ──────────────────────────────────────────────────────────────
  addJob() {
    this.jobs.push({ name: '', link: '', jobType: 'DEPLOY', orderNum: this.jobs.length + 1 });
  }
  removeJob(i: number) { this.jobs.splice(i, 1); }

  // ── Section 3 ──────────────────────────────────────────────────────────────
  get checklist(): ChecklistStep[] {
    if (this.activePhase === 'PRE') return this.preList;
    if (this.activePhase === 'DURING') return this.duringList;
    return this.rollbackList;
  }

  addStep() {
    const step = this.emptyStep();
    if (this.activePhase === 'PRE') this.preList.push(step);
    else if (this.activePhase === 'DURING') this.duringList.push(step);
    else this.rollbackList.push(step);
  }

  removeStep(i: number) {
    if (this.activePhase === 'PRE') this.preList.splice(i, 1);
    else if (this.activePhase === 'DURING') this.duringList.splice(i, 1);
    else this.rollbackList.splice(i, 1);
  }

  searchAssignee(step: ChecklistStep) {
    if (!step.assigneeQuery.trim()) return;
    step.searchingAssignee = true; step.notFoundAssignee = false; step.assigneeResult = null;
    this.svc.searchUser(step.assigneeQuery.trim()).subscribe({
      next: res => {
        step.assigneeResult = res?.[0] ?? null;
        step.notFoundAssignee = !step.assigneeResult;
        step.searchingAssignee = false;
      },
      error: () => { step.notFoundAssignee = true; step.searchingAssignee = false; }
    });
  }

  clearAssignee(step: ChecklistStep) {
    step.assigneeQuery = '';
    step.assigneeResult = null;
    step.notFoundAssignee = false;
  }

  // ── Section 4 ──────────────────────────────────────────────────────────────
  searchSlot(slot: TeamSlot) {
    if (!slot.query.trim()) return;
    slot.searching = true; slot.notFound = false; slot.result = null;
    this.svc.searchUser(slot.query.trim()).subscribe({
      next: res => { slot.result = res?.[0] ?? null; slot.notFound = !slot.result; slot.searching = false; },
      error: () => { slot.notFound = true; slot.searching = false; }
    });
  }
  clearSlot(slot: TeamSlot) { slot.query = ''; slot.result = null; slot.notFound = false; }

  // ── Section 5 ──────────────────────────────────────────────────────────────
  addApprover() {
    this.approverSlots.push({ query: '', result: null, searching: false, notFound: false });
  }
  removeApprover(i: number) { this.approverSlots.splice(i, 1); }
  searchApprover(slot: ApproverSlot) {
    if (!slot.query.trim()) return;
    slot.searching = true; slot.notFound = false; slot.result = null;
    this.svc.searchUser(slot.query.trim()).subscribe({
      next: res => { slot.result = res?.[0] ?? null; slot.notFound = !slot.result; slot.searching = false; },
      error: () => { slot.notFound = true; slot.searching = false; }
    });
  }

  // ── Build & save ──────────────────────────────────────────────────────────
  private buildChecklist(list: ChecklistStep[], phase: Phase): ChecklistItem[] {
    return list
      .filter(s => s.stepText.trim())
      .map((s, i) => ({
        id: s.id,
        phase,
        stepText: s.stepText.trim(),
        orderNum: i + 1,
        assignedTo:     s.assigneeResult?.username || s.assignedTo || undefined,
        assignedToCode: s.assigneeResult?.employeeCode || s.assignedToCode || undefined
      }));
  }

  private buildRequest() {
    const teamMembers: TeamMember[] = [];
    if (this.devSlot.result) teamMembers.push({
      id: this.devSlot.id,
      userId: String(this.devSlot.result.userId),
      username: this.devSlot.result.username,
      fullName: this.devSlot.result.fullName,
      employeeCode: this.devSlot.result.employeeCode,
      memberRole: 'DEV', isLead: 1
    });
    if (this.testerSlot.result) teamMembers.push({
      id: this.testerSlot.id,
      userId: String(this.testerSlot.result.userId),
      username: this.testerSlot.result.username,
      fullName: this.testerSlot.result.fullName,
      employeeCode: this.testerSlot.result.employeeCode,
      memberRole: 'TESTER', isLead: 0
    });
    if (this.baSlot.result) teamMembers.push({
      id: this.baSlot.id,
      userId: String(this.baSlot.result.userId),
      username: this.baSlot.result.username,
      fullName: this.baSlot.result.fullName,
      employeeCode: this.baSlot.result.employeeCode,
      memberRole: 'BA', isLead: 0
    });

    const approvers: Approver[] = this.approverSlots
      .filter(s => s.result)
      .map(s => ({
        id: s.id,
        userId: String(s.result!.userId),
        username: s.result!.username,
        fullName: s.result!.fullName,
        employeeCode: s.result!.employeeCode
      }));

    return {
      changeName: this.changeName.trim(),
      content:    this.content,
      gitLink:    this.gitLink,
      goliveAt:   this.goliveAt || null,
      jobs: this.jobs.filter(j => j.name.trim()).map((j, i) => ({ ...j, orderNum: i + 1 })),
      checklistItems: [
        ...this.buildChecklist(this.preList,      'PRE'),
        ...this.buildChecklist(this.duringList,   'DURING'),
        ...this.buildChecklist(this.rollbackList, 'ROLLBACK')
      ],
      teamMembers,
      approvers
    };
  }

  save(thenSubmit = false) {
    if (!this.changeName.trim()) { this.error = 'Tên change là bắt buộc.'; return; }
    this.saving = true; this.error = '';

    const body = this.buildRequest();
    const op = this.isEdit && this.editId
      ? this.svc.updateChange(this.editId, body)
      : this.svc.createChange(body);

    op.subscribe({
      next: cr => {
        // PUT /api/changes/{id} returns null — fall back to editId for navigation & submit
        const changeId = cr?.id ?? this.editId;
        if (thenSubmit && changeId) {
          this.svc.submitChange(changeId).subscribe({
            next: () => this.router.navigate(['/changes', changeId]),
            error: () => this.router.navigate(['/changes', changeId])
          });
        } else {
          this.saving = false;
          this.router.navigate(['/changes', changeId ?? '']);
        }
      },
      error: e => {
        this.saving = false;
        this.error = e?.error?.errorDesc || e?.error?.message || 'Không thể lưu. Vui lòng thử lại.';
      }
    });
  }

  cancel() { this.router.navigate(['/changes']); }
}
