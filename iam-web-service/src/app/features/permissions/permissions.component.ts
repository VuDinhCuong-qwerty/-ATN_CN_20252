import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PermissionService } from '../../core/auth/permission.service';
import { IdentityService } from '../../shared/services/identity.service';
import { AppApiService } from '../../shared/services/app-api.service';

@Component({
  selector: 'app-permissions',
  imports: [CommonModule, FormsModule],
  templateUrl: './permissions.component.html',
  styleUrl: './permissions.component.css'
})
export class PermissionsComponent implements OnInit {
  activeTab: 'myperms' | 'request' | 'approve' = 'myperms';
  myPermsSubTab: 'app' | 'resource' = 'app';

  // Tab: Danh sách quyền
  appPermissions: any[] = [];
  resourcePermissions: any[] = [];
  loadingMyPerms = false;
  queryEmpCode = '';

  // Tab: Danh sách yêu cầu (luôn scope theo requester = empCode hiện tại)
  myRequests: any[] = [];
  requestPage = 0;
  requestSize = 10;
  requestTotal = 0;
  requestTotalPages = 0;
  loadingRequests = false;
  filterRequestStatus = '';
  filterRequestFrom = '';
  filterRequestTo = '';

  // Tab: Duyệt yêu cầu (luôn scope theo reviewer = empCode hiện tại)
  officialRequests: any[] = [];
  officialPage = 0;
  officialSize = 10;
  officialTotal = 0;
  officialTotalPages = 0;
  loadingOfficial = false;
  filterOfficialStatus = 'OFFICIAL';

  message = '';
  messageType = '';

  get empCode(): string { return this.perm.userInfo?.employeeCode ?? ''; }

  constructor(
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.queryEmpCode = this.empCode;
    this.loadMyPermissions();
    if (this.perm.has('user-permission', 'request') || this.perm.has('user-permission', 'cancel')) {
      this.loadMyRequests();
    }
    if (this.perm.has('user-permission', 'approve') || this.perm.has('user-permission', 'reject')) {
      this.loadOfficialRequests();
    }
  }

  switchTab(tab: 'myperms' | 'request' | 'approve') {
    this.activeTab = tab;
    if (tab === 'approve') this.loadOfficialRequests();
  }

  // === Tab: Danh sách quyền ===
  loadMyPermissions() {
    const code = this.queryEmpCode || this.empCode;
    if (!code) return;
    this.loadingMyPerms = true;
    this.identityService.getAppPermissions(code).subscribe({
      next: res => { this.appPermissions = res.data?.content ?? res.data ?? []; },
      error: () => {}
    });
    this.identityService.getResourcePermissions(code).subscribe({
      next: res => { this.loadingMyPerms = false; this.resourcePermissions = res.data?.content ?? res.data ?? []; },
      error: () => { this.loadingMyPerms = false; }
    });
  }

  searchPerms() { this.loadMyPermissions(); }

  // === Tab: Danh sách yêu cầu ===
  searchRequests() { this.requestPage = 0; this.loadMyRequests(); }
  clearRequestFilter() {
    this.filterRequestStatus = '';
    this.filterRequestFrom = '';
    this.filterRequestTo = '';
    this.requestPage = 0;
    this.loadMyRequests();
  }

  loadMyRequests() {
    this.loadingRequests = true;
    // Luôn scope theo requester = currentUser
    const q: any = { page: this.requestPage, size: this.requestSize, requester: this.empCode };
    if (this.filterRequestStatus) q.status = this.filterRequestStatus;
    if (this.filterRequestFrom) q.from = this.filterRequestFrom;
    if (this.filterRequestTo) q.to = this.filterRequestTo;
    this.identityService.getPermissionRequests(q).subscribe({
      next: res => {
        this.loadingRequests = false;
        const data = res.data;
        if (data?.content) {
          this.myRequests = data.content;
          this.requestTotal = data.totalElements ?? 0;
          this.requestTotalPages = data.totalPages ?? 1;
        } else {
          this.myRequests = data ?? [];
          this.requestTotal = this.myRequests.length;
          this.requestTotalPages = 1;
        }
      },
      error: () => { this.loadingRequests = false; }
    });
  }

  goToRequestPage(page: number) {
    if (page < 0 || page >= this.requestTotalPages) return;
    this.requestPage = page;
    this.loadMyRequests();
  }

  openRequestDetail(req: any) {
    this.router.navigate(['/users/permissions/detail'], { queryParams: { requestId: req.requestId } });
  }

  openCreate() {
    this.router.navigate(['/users/permissions/create']);
  }

  // === Tab: Duyệt yêu cầu ===
  searchOfficial() { this.officialPage = 0; this.loadOfficialRequests(); }
  clearOfficialFilter() {
    this.filterOfficialStatus = 'OFFICIAL';
    this.officialPage = 0;
    this.loadOfficialRequests();
  }

  loadOfficialRequests() {
    this.loadingOfficial = true;
    // Luôn scope theo reviewer = currentUser
    const q: any = { page: this.officialPage, size: this.officialSize, reviewer: this.empCode };
    if (this.filterOfficialStatus) q.status = this.filterOfficialStatus;
    this.identityService.getPermissionRequests(q).subscribe({
      next: res => {
        this.loadingOfficial = false;
        const data = res.data;
        if (data?.content) {
          this.officialRequests = data.content;
          this.officialTotal = data.totalElements ?? 0;
          this.officialTotalPages = data.totalPages ?? 1;
        } else {
          this.officialRequests = data ?? [];
          this.officialTotal = this.officialRequests.length;
          this.officialTotalPages = 1;
        }
      },
      error: () => { this.loadingOfficial = false; }
    });
  }

  goToOfficialPage(page: number) {
    if (page < 0 || page >= this.officialTotalPages) return;
    this.officialPage = page;
    this.loadOfficialRequests();
  }

  openOfficialDetail(req: any) {
    this.router.navigate(['/users/permissions/detail'], {
      queryParams: { requestId: req.requestId, mode: 'approve' }
    });
  }

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

  get requestPages(): number[] {
    return Array.from({ length: this.requestTotalPages }, (_, i) => i);
  }

  get officialPages(): number[] {
    return Array.from({ length: this.officialTotalPages }, (_, i) => i);
  }

  private showMessage(msg: string, type: string) {
    this.message = msg; this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }
}
