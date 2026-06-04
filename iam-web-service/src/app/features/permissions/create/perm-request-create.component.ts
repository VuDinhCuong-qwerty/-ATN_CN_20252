import { Component, HostListener, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { PermissionService } from '../../../core/auth/permission.service';
import { IdentityService } from '../../../shared/services/identity.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-perm-request-create',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './perm-request-create.component.html',
  styleUrl: './perm-request-create.component.css'
})
export class PermissionRequestCreateComponent implements OnInit, OnDestroy {
  form = { reviewer: '', reviewerCode: '', grantee: '', granteeCode: '', reason: '' };

  // Reviewer autocomplete
  reviewerSuggestions: any[] = [];
  showReviewerDropdown = false;
  selectedReviewer: any = null;
  private reviewerSearchTimeout: any = null;

  // Grantee autocomplete
  granteeSuggestions: any[] = [];
  showGranteeDropdown = false;
  selectedGrantee: any = null;
  private granteeSearchTimeout: any = null;

  appItems: { appId: string }[] = [];
  resourceItems: {
    appId: string;
    resourceId: string;
    selectedActions: { [key: string]: boolean };
    showActionDropdown: boolean;
    resOptions: any[];
    loadingRes: boolean;
  }[] = [];

  allApps: any[] = [];
  loadingSave = false;
  loadingSend = false;
  message = '';
  messageType = '';

  constructor(
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.appApiService.getApplicationsForSelect().subscribe({
      next: res => this.allApps = res.data?.content ?? res.data ?? []
    });
  }

  ngOnDestroy() {
    clearTimeout(this.reviewerSearchTimeout);
    clearTimeout(this.granteeSearchTimeout);
  }

  onReviewerInput() {
    this.selectedReviewer = null;
    this.form.reviewerCode = '';
    const q = this.form.reviewer.trim();
    clearTimeout(this.reviewerSearchTimeout);
    if (q.length < 2) {
      this.reviewerSuggestions = [];
      this.showReviewerDropdown = false;
      return;
    }
    this.reviewerSearchTimeout = setTimeout(() => {
      this.identityService.getUsers({ username: q, status: 'ACTIVE', size: 8, page: 0 }).subscribe({
        next: res => {
          this.reviewerSuggestions = res.data?.content ?? [];
          this.showReviewerDropdown = this.reviewerSuggestions.length > 0;
        }
      });
    }, 300);
  }

  selectReviewer(user: any, event: Event) {
    event.stopPropagation();
    this.selectedReviewer = user;
    this.form.reviewer = user.username;
    this.form.reviewerCode = user.employeeCode;
    this.showReviewerDropdown = false;
    this.reviewerSuggestions = [];
  }

  onGranteeInput() {
    this.selectedGrantee = null;
    this.form.granteeCode = '';
    const q = this.form.grantee.trim();
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
    this.selectedGrantee = user;
    this.form.grantee = user.username;
    this.form.granteeCode = user.employeeCode;
    this.showGranteeDropdown = false;
    this.granteeSuggestions = [];
  }

  addApp() { this.appItems.push({ appId: '' }); }
  removeApp(i: number) { this.appItems.splice(i, 1); }

  addResource() {
    this.resourceItems.push({
      appId: '', resourceId: '', selectedActions: {},
      showActionDropdown: false, resOptions: [], loadingRes: false
    });
  }
  removeResource(i: number) { this.resourceItems.splice(i, 1); }

  onResourceAppChange(i: number) {
    const item = this.resourceItems[i];
    item.resourceId = '';
    item.resOptions = [];
    item.selectedActions = {};
    item.showActionDropdown = false;
    if (!item.appId) return;
    item.loadingRes = true;
    this.appApiService.getResourcesForSelect(Number(item.appId)).subscribe({
      next: res => { item.resOptions = res.data?.content ?? res.data ?? []; item.loadingRes = false; },
      error: () => { item.loadingRes = false; }
    });
  }

  onResourceChange(i: number) {
    this.resourceItems[i].selectedActions = {};
    this.resourceItems[i].showActionDropdown = false;
  }

  toggleActionDropdown(i: number, event: Event) {
    event.stopPropagation();
    const avail = this.getAvailableActions(i);
    if (avail.length === 0) return;
    const cur = this.resourceItems[i].showActionDropdown;
    this.resourceItems.forEach(r => r.showActionDropdown = false);
    this.resourceItems[i].showActionDropdown = !cur;
  }

  toggleAction(i: number, act: string, event: Event) {
    event.stopPropagation();
    this.resourceItems[i].selectedActions[act] = !this.resourceItems[i].selectedActions[act];
  }

  removeAction(i: number, act: string, event: Event) {
    event.stopPropagation();
    this.resourceItems[i].selectedActions[act] = false;
  }

  getSelectedActions(i: number): string[] {
    return this.getAvailableActions(i).filter(a => this.resourceItems[i].selectedActions[a]);
  }

  @HostListener('document:click')
  closeAllDropdowns() {
    this.resourceItems.forEach(r => r.showActionDropdown = false);
    this.showReviewerDropdown = false;
    this.showGranteeDropdown = false;
  }

  getAvailableActions(i: number): string[] {
    const item = this.resourceItems[i];
    if (!item.resourceId) return [];
    const res = item.resOptions.find(r => String(r.id) === item.resourceId);
    if (!res?.actions) return [];
    // actions từ ResourceListResponse là List<String> → JSON array
    return Array.isArray(res.actions)
      ? res.actions.map((a: string) => a.trim()).filter(Boolean)
      : String(res.actions).split(',').map((a: string) => a.trim()).filter(Boolean);
  }

  submit(type: 'DRAFT' | 'OFFICIAL') {
    if (!this.form.reviewerCode.trim() || !this.form.reviewer.trim() || !this.form.reason.trim()) {
      this.showMessage('Vui lòng điền đầy đủ Người duyệt và Lý do', 'warning');
      return;
    }
    const body = {
      requester: this.perm.userInfo?.username ?? '',
      requesterCode: this.perm.userInfo?.employeeCode ?? '',
      reviewer: this.form.reviewer.trim(),
      reviewerCode: this.form.reviewerCode.trim(),
      requestForCode: this.form.granteeCode.trim() || null,
      reason: this.form.reason.trim(),
      type,
      apps: this.appItems.filter(a => a.appId).map(a => ({ appId: Number(a.appId) })),
      resources: this.resourceItems
        .filter(r => r.resourceId)
        .map((r, i) => ({
          resourceId: Number(r.resourceId),
          actions: this.getSelectedActions(i).join(',')
        }))
        .filter(r => r.actions)
    };

    if (type === 'DRAFT') this.loadingSave = true;
    else this.loadingSend = true;

    this.identityService.createPermissionRequest(body).subscribe({
      next: () => {
        this.loadingSave = false;
        this.loadingSend = false;
        this.showMessage(type === 'DRAFT' ? 'Đã lưu yêu cầu nháp' : 'Đã gửi yêu cầu chính thức', 'success');
        setTimeout(() => this.router.navigate(['/users/permissions']), 1500);
      },
      error: err => {
        this.loadingSave = false;
        this.loadingSend = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể tạo yêu cầu'}`, 'danger');
      }
    });
  }

  cancel() { this.router.navigate(['/users/permissions']); }

  private showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }
}
