import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { PermissionService } from '../../../core/auth/permission.service';
import { IdentityService } from '../../../shared/services/identity.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-perm-request-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './perm-request-detail.component.html',
  styleUrl: './perm-request-detail.component.css'
})
export class PermissionRequestDetailComponent implements OnInit, OnDestroy {
  requestId = '';
  mode: 'view' | 'approve' = 'view';
  detail: any = null;
  loading = true;
  submitting = false;
  saving = false;
  message = '';
  messageType = '';

  // Editable details state
  editReason = '';
  editGrantee = '';
  editGranteeCode = '';
  editApps: { appId: number; name: string; code: string }[] = [];
  editResources: { resourceId: number; name: string; code: string; actions: string; appCode: string }[] = [];

  // Grantee autocomplete (edit mode)
  granteeSuggestions: any[] = [];
  showGranteeDropdown = false;
  selectedEditGrantee: any = null;
  private granteeSearchTimeout: any = null;

  // Add app panel
  allApps: any[] = [];
  appsLoaded = false;
  showAddApp = false;
  addAppId: number | null = null;

  // Add resource panel
  showAddResource = false;
  addResAppId: number | null = null;
  addResList: any[] = [];
  loadingRes = false;
  addResId: number | null = null;
  addResSelectedActions: { [key: string]: boolean } = {};
  showAddActionsDropdown = false;

  get addResAvailableActions(): string[] {
    if (!this.addResId) return [];
    const res = this.addResList.find(r => r.id === this.addResId);
    if (!res?.actions) return [];
    // actions từ ResourceListResponse là List<String> → JSON array
    return Array.isArray(res.actions)
      ? res.actions.map((a: string) => a.trim()).filter(Boolean)
      : String(res.actions).split(',').map((a: string) => a.trim()).filter(Boolean);
  }

  // Approve/Reject
  approveNote = '';
  approving = false;
  rejecting = false;
  cancelling = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService
  ) {}

  ngOnInit() {
    this.requestId = this.route.snapshot.queryParamMap.get('requestId') ?? '';
    this.mode = this.route.snapshot.queryParamMap.get('mode') === 'approve' ? 'approve' : 'view';
    if (this.requestId) this.loadDetail();
  }

  ngOnDestroy() {
    clearTimeout(this.granteeSearchTimeout);
  }

  loadDetail() {
    this.loading = true;
    this.identityService.getPermissionRequestDetail(this.requestId as any).subscribe({
      next: res => {
        this.loading = false;
        this.detail = res.data;
        this.populateEditState();
      },
      error: () => { this.loading = false; }
    });
  }

  private populateEditState() {
    if (!this.detail) return;
    this.editReason = this.detail.reason ?? '';
    this.editGrantee = this.detail.grantee?.username ?? '';
    this.editGranteeCode = this.detail.grantee?.employeeCode ?? '';
    this.selectedEditGrantee = this.detail.grantee ?? null;
    this.editApps = (this.detail.details?.apps ?? []).map((a: any) => ({
      appId: a.id, name: a.name ?? a.code, code: a.code
    }));
    this.editResources = (this.detail.details?.resources ?? []).map((r: any) => ({
      resourceId: r.id, name: r.name ?? r.code, code: r.code,
      actions: r.actions ?? '', appCode: r.appCode ?? ''
    }));
  }

  onGranteeInput() {
    this.selectedEditGrantee = null;
    this.editGranteeCode = '';
    const q = this.editGrantee.trim();
    clearTimeout(this.granteeSearchTimeout);
    if (q.length < 2) { this.granteeSuggestions = []; this.showGranteeDropdown = false; return; }
    this.granteeSearchTimeout = setTimeout(() => {
      this.identityService.getUsers({ username: q, status: 'ACTIVE', size: 8, page: 0 }).subscribe({
        next: res => {
          this.granteeSuggestions = res.data?.content ?? [];
          this.showGranteeDropdown = this.granteeSuggestions.length > 0;
        }
      });
    }, 300);
  }

  selectGrantee(user: any, event: Event) {
    event.stopPropagation();
    this.selectedEditGrantee = user;
    this.editGrantee = user.username;
    this.editGranteeCode = user.employeeCode;
    this.showGranteeDropdown = false;
    this.granteeSuggestions = [];
  }

  get canEdit(): boolean {
    return this.mode === 'view'
      && this.detail?.status === 'DRAFT'
      && this.perm.has('user-permission', 'request');
  }

  // === Apps management ===
  removeApp(i: number) { this.editApps.splice(i, 1); }

  openAddApp() {
    if (!this.appsLoaded) {
      this.appApiService.getApplicationsForSelect().subscribe({
        next: res => {
          this.allApps = res.data?.content ?? res.data ?? [];
          this.appsLoaded = true;
          this.showAddApp = true;
        }
      });
    } else {
      this.showAddApp = true;
    }
  }

  confirmAddApp() {
    if (!this.addAppId) return;
    if (this.editApps.some(a => a.appId === this.addAppId)) {
      this.showMessage('Ứng dụng đã có trong danh sách', 'warning'); return;
    }
    const app = this.allApps.find(a => a.id === this.addAppId);
    if (!app) return;
    this.editApps.push({ appId: app.id, name: app.name, code: app.code });
    this.addAppId = null;
    this.showAddApp = false;
  }

  // === Resources management ===
  removeResource(i: number) { this.editResources.splice(i, 1); }

  openAddResource() {
    if (!this.appsLoaded) {
      this.appApiService.getApplicationsForSelect().subscribe({
        next: res => {
          this.allApps = res.data?.content ?? res.data ?? [];
          this.appsLoaded = true;
          this.showAddResource = true;
        }
      });
    } else {
      this.showAddResource = true;
    }
  }

  onAddResAppChange() {
    this.addResId = null;
    this.addResList = [];
    this.addResSelectedActions = {};
    if (!this.addResAppId) return;
    this.loadingRes = true;
    this.appApiService.getResourcesForSelect(this.addResAppId).subscribe({
      next: res => { this.addResList = res.data?.content ?? res.data ?? []; this.loadingRes = false; },
      error: () => { this.loadingRes = false; }
    });
  }

  onAddResIdChange() {
    this.addResSelectedActions = {};
    this.showAddActionsDropdown = false;
  }

  toggleAddActionsDropdown(event: Event) {
    event.stopPropagation();
    if (this.addResAvailableActions.length === 0) return;
    this.showAddActionsDropdown = !this.showAddActionsDropdown;
  }

  toggleAddAction(act: string, event: Event) {
    event.stopPropagation();
    this.addResSelectedActions[act] = !this.addResSelectedActions[act];
  }

  removeAddAction(act: string, event: Event) {
    event.stopPropagation();
    this.addResSelectedActions[act] = false;
  }

  get addResSelectedList(): string[] {
    return this.addResAvailableActions.filter(a => this.addResSelectedActions[a]);
  }

  @HostListener('document:click')
  closeDropdowns() {
    this.showAddActionsDropdown = false;
    this.showGranteeDropdown = false;
  }

  confirmAddResource() {
    const actions = this.addResSelectedList.join(',');
    if (!this.addResId || !actions) {
      this.showMessage('Chọn tài nguyên và ít nhất 1 action', 'warning'); return;
    }
    if (this.editResources.some(r => r.resourceId === this.addResId)) {
      this.showMessage('Tài nguyên đã có trong danh sách', 'warning'); return;
    }
    const res = this.addResList.find(r => r.id === this.addResId);
    const app = this.allApps.find(a => a.id === this.addResAppId);
    if (!res) return;
    this.editResources.push({
      resourceId: res.id,
      name: res.resourceName ?? res.resourceCode,
      code: res.resourceCode,
      actions,
      appCode: app?.code ?? ''
    });
    this.addResId = null;
    this.addResSelectedActions = {};
    this.showAddResource = false;
  }

  // === Save (update DRAFT) ===
  saveUpdate() {
    if (this.editApps.length === 0 && this.editResources.length === 0) {
      this.showMessage('Yêu cầu cần ít nhất 1 ứng dụng hoặc 1 tài nguyên', 'warning'); return;
    }
    this.saving = true;
    const body = {
      requestId: +this.requestId,
      requester: this.perm.userInfo?.username ?? '',
      requesterCode: this.perm.userInfo?.employeeCode ?? '',
      reviewer: this.detail.reviewer?.username ?? '',
      reviewerCode: this.detail.reviewer?.employeeCode ?? '',
      requestForCode: this.editGranteeCode.trim() || null,
      reason: this.editReason.trim(),
      type: 'DRAFT',
      apps: this.editApps.map(a => ({ appId: a.appId })),
      resources: this.editResources.map(r => ({ resourceId: r.resourceId, actions: r.actions }))
    };
    this.identityService.updatePermissionRequest(body).subscribe({
      next: () => {
        this.saving = false;
        this.showMessage('Đã lưu thay đổi', 'success');
        this.loadDetail();
      },
      error: err => {
        this.saving = false;
        this.showMessage(`Lỗi: ${err.error?.errorDesc ?? err.error?.message ?? 'Không thể lưu'}`, 'danger');
      }
    });
  }

  cancelEdit() {
    this.populateEditState();
    this.showAddApp = false;
    this.showAddResource = false;
    this.addAppId = null;
    this.addResId = null;
    this.addResSelectedActions = {};
    this.showAddActionsDropdown = false;
    this.granteeSuggestions = [];
    this.showGranteeDropdown = false;
  }

  // === Submit OFFICIAL ===
  submitRequest() {
    if (!this.detail) return;
    this.submitting = true;
    const username = this.perm.userInfo?.username ?? '';
    const empCode = this.perm.userInfo?.employeeCode ?? '';
    this.identityService.submitPermissionRequest(this.requestId, username, empCode).subscribe({
      next: () => {
        this.submitting = false;
        this.showMessage('Đã gửi yêu cầu chính thức', 'success');
        setTimeout(() => this.loadDetail(), 1000);
      },
      error: err => {
        this.submitting = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể gửi'}`, 'danger');
      }
    });
  }

  // === Approve / Reject ===
  doApprove() {
    this.approving = true;
    this.identityService.approveRequest({ requestId: +this.requestId, note: this.approveNote }).subscribe({
      next: () => {
        this.approving = false;
        this.showMessage('Đã duyệt yêu cầu', 'success');
        setTimeout(() => this.loadDetail(), 1000);
      },
      error: err => {
        this.approving = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể duyệt'}`, 'danger');
      }
    });
  }

  doCancel() {
    if (!confirm('Bạn có chắc muốn hủy yêu cầu này?')) return;
    this.cancelling = true;
    this.identityService.cancelRequest(+this.requestId).subscribe({
      next: () => {
        this.cancelling = false;
        this.showMessage('Đã hủy yêu cầu', 'success');
        setTimeout(() => this.router.navigate(['/users/permissions']), 1500);
      },
      error: err => {
        this.cancelling = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể hủy yêu cầu'}`, 'danger');
      }
    });
  }

  doReject() {
    if (!this.approveNote.trim()) {
      this.showMessage('Vui lòng nhập lý do từ chối', 'warning'); return;
    }
    this.rejecting = true;
    this.identityService.rejectRequest({ requestId: +this.requestId, note: this.approveNote }).subscribe({
      next: () => {
        this.rejecting = false;
        this.showMessage('Đã từ chối yêu cầu', 'success');
        setTimeout(() => this.loadDetail(), 1000);
      },
      error: err => {
        this.rejecting = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể từ chối'}`, 'danger');
      }
    });
  }

  goBack() { this.router.navigate(['/users/permissions']); }

  statusBadge(status: string): string {
    switch (status) {
      case 'DRAFT': return 'bg-secondary';
      case 'OFFICIAL': return 'bg-primary';
      case 'APPROVED': return 'bg-success';
      case 'REJECTED': return 'bg-danger';
      case 'CANCELLED': return 'bg-dark';
      default: return 'bg-light text-dark';
    }
  }

  private showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }
}
