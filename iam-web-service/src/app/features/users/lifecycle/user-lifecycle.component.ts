import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PermissionService } from '../../../core/auth/permission.service';
import { IdentityService } from '../../../shared/services/identity.service';
import { AppApiService } from '../../../shared/services/app-api.service';

type LifecycleTab = 'onboard' | 'offboard' | 'leave' | 'transfer';
type LeaveSubTab = 'working' | 'on-leave';
type LeaveAction = 'leave' | 'extend' | 'return';

@Component({
  selector: 'app-user-lifecycle',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-lifecycle.component.html',
  styleUrl: './user-lifecycle.component.css'
})
export class UserLifecycleComponent implements OnInit {
  activeTab: LifecycleTab = 'onboard';

  // Search
  searchFullName = '';
  searchEmpCode = '';
  searchUsername = '';
  searchDeptId: any = '';
  searchStatus = '';
  userList: any[] = [];
  loadingSearch = false;
  searchPage = 0;
  searchSize = 10;
  searchTotal = 0;
  searchTotalPages = 0;

  // Modal — onboard / offboard / transfer
  showActionModal = false;
  selectedUser: any = null;
  showConfirm = false;
  loading = false;
  onboardForm = { roleIds: [] as any[], departmentId: '' as any, positionCode: '' as any, joinDate: '' };
  transferForm = { roleIds: [] as any[], departmentId: '' as any, positionCode: '' as any, transferDate: '' };

  // Leave sub-tab
  leaveSubTab: LeaveSubTab = 'working';

  // Modal — leave actions (separate from action modal)
  showLeaveModal = false;
  showLeaveConfirm = false;
  leaveActionLoading = false;
  selectedLeaveUser: any = null;
  currentLeaveAction: LeaveAction | null = null;
  leaveCreateForm = { fromDate: '', toDate: '' };
  leaveExtendForm = { toDate: '' };

  departments: any[] = [];
  flatDepartments: any[] = [];
  positions: any[] = [];
  roles: any[] = [];
  message = '';
  messageType = '';

  constructor(
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService
  ) {}

  ngOnInit() {
    this.appApiService.getDepartments().subscribe({ next: res => {
      this.departments = res.data ?? [];
      this.flatDepartments = this.flattenDepts(this.departments);
    }});
    this.appApiService.getPositions().subscribe({ next: res => this.positions = res.data ?? [] });
    this.appApiService.getRoles().subscribe({ next: res => this.roles = res.data ?? [] });

    if (this.perm.has('user-lifecycle', 'onboard')) this.activeTab = 'onboard';
    else if (this.perm.has('user-lifecycle', 'offboard')) this.activeTab = 'offboard';
    else if (this.perm.has('user-lifecycle', 'leave') || this.perm.has('user-lifecycle', 'return')) this.activeTab = 'leave';
    else if (this.perm.has('user-lifecycle', 'transfer')) this.activeTab = 'transfer';

    if (this.activeTab === 'leave') {
      this.leaveSubTab = 'working';
      this.searchStatus = 'ACTIVE';
    }

    this.searchUsers();
  }

  switchTab(tab: LifecycleTab) {
    this.activeTab = tab;
    this.searchFullName = '';
    this.searchEmpCode = '';
    this.searchUsername = '';
    this.searchDeptId = '';
    this.userList = [];
    this.searchPage = 0;
    this.searchTotal = 0;
    this.searchTotalPages = 0;
    this.closeModal();

    if (tab === 'leave') {
      this.leaveSubTab = 'working';
      this.searchStatus = 'ACTIVE';
    } else {
      this.searchStatus = '';
    }

    this.searchUsers();
  }

  switchLeaveSubTab(tab: LeaveSubTab) {
    this.leaveSubTab = tab;
    this.searchStatus = tab === 'working' ? 'ACTIVE' : 'INACTIVE';
    this.searchPage = 0;
    this.userList = [];
    this.searchUsers();
  }

  searchUsers() {
    this.loadingSearch = true;
    const params: any = { page: this.searchPage, size: this.searchSize };
    if (this.searchFullName.trim()) params.fullName = this.searchFullName.trim();
    if (this.searchEmpCode.trim()) params.employeeCode = this.searchEmpCode.trim();
    if (this.searchUsername.trim()) params.username = this.searchUsername.trim();
    if (this.searchDeptId) params.departmentId = this.searchDeptId;
    if (this.activeTab === 'onboard') {
      params.offboarded = true;
    } else if (this.activeTab === 'leave' && this.leaveSubTab === 'on-leave') {
      params.onLeave = true;
    } else if (this.searchStatus) {
      params.status = this.searchStatus;
    }

    this.identityService.getUsers(params).subscribe({
      next: res => {
        this.loadingSearch = false;
        const data = res.data;
        if (data?.content) {
          this.userList = data.content;
          this.searchTotal = data.totalElements ?? 0;
          this.searchTotalPages = data.totalPages ?? 1;
        } else {
          this.userList = data ?? [];
          this.searchTotal = this.userList.length;
          this.searchTotalPages = 1;
        }
      },
      error: () => { this.loadingSearch = false; }
    });
  }

  onSearch() {
    this.searchPage = 0;
    this.searchUsers();
  }

  goToPage(page: number) {
    if (page < 0 || page >= this.searchTotalPages) return;
    this.searchPage = page;
    this.searchUsers();
  }

  get pages(): number[] {
    return Array.from({ length: this.searchTotalPages }, (_, i) => i);
  }

  // ── Onboard / Offboard / Transfer modal ──────────────────────────────────

  openModal(user: any) {
    this.selectedUser = user;
    this.showActionModal = true;
    this.showConfirm = false;
    this.onboardForm = { roleIds: [], departmentId: '', positionCode: '', joinDate: '' };
    this.transferForm = { roleIds: [], departmentId: '', positionCode: '', transferDate: '' };
  }

  closeModal() {
    this.showActionModal = false;
    this.selectedUser = null;
    this.showConfirm = false;
  }

  requestConfirm() {
    if (this.activeTab === 'onboard') {
      if (!this.onboardForm.roleIds.length) {
        this.showMessage('Vui lòng chọn ít nhất một vai trò', 'warning'); return;
      }
      if (!this.onboardForm.departmentId || !this.onboardForm.joinDate) {
        this.showMessage('Vui lòng điền đủ thông tin bắt buộc', 'warning'); return;
      }
    }
    if (this.activeTab === 'transfer') {
      if (!this.transferForm.roleIds.length) {
        this.showMessage('Vui lòng chọn ít nhất một vai trò', 'warning'); return;
      }
      if (!this.transferForm.departmentId || !this.transferForm.transferDate) {
        this.showMessage('Vui lòng điền đủ thông tin bắt buộc', 'warning'); return;
      }
    }
    this.showConfirm = true;
  }

  cancelConfirm() { this.showConfirm = false; }

  confirmAction() {
    this.loading = true;
    const u = this.selectedUser;
    let action$: any;

    switch (this.activeTab) {
      case 'onboard':
        action$ = this.identityService.onboardUser(u.userId, u.employeeCode, {
          roleIds: this.onboardForm.roleIds.map(Number),
          departmentId: Number(this.onboardForm.departmentId),
          positionCode: this.onboardForm.positionCode || null,
          joinDate: this.onboardForm.joinDate
        });
        break;
      case 'offboard':
        action$ = this.identityService.offboardUser(u.userId, u.employeeCode);
        break;
      case 'transfer':
        action$ = this.identityService.transferUser(u.userId, u.employeeCode, {
          roleIds: this.transferForm.roleIds.map(Number),
          departmentId: Number(this.transferForm.departmentId),
          positionCode: this.transferForm.positionCode || null,
          transferDate: this.transferForm.transferDate
        });
        break;
      default:
        this.loading = false;
        return;
    }

    action$.subscribe({
      next: () => {
        this.loading = false;
        this.showMessage('Thực hiện thành công!', 'success');
        this.closeModal();
        this.searchUsers();
      },
      error: (err: any) => {
        this.loading = false;
        this.showConfirm = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? err.error?.errorDesc ?? 'Không thể thực hiện'}`, 'danger');
      }
    });
  }

  // ── Leave modal ──────────────────────────────────────────────────────────

  openLeaveCreateModal(user: any) {
    this.selectedLeaveUser = user;
    this.currentLeaveAction = 'leave';
    this.leaveCreateForm = { fromDate: '', toDate: '' };
    this.showLeaveModal = true;
    this.showLeaveConfirm = false;
  }

  openExtendModal(user: any) {
    this.selectedLeaveUser = user;
    this.currentLeaveAction = 'extend';
    this.leaveExtendForm = { toDate: '' };
    this.showLeaveModal = true;
    this.showLeaveConfirm = false;
  }

  openReturnModal(user: any) {
    this.selectedLeaveUser = user;
    this.currentLeaveAction = 'return';
    this.showLeaveModal = true;
    this.showLeaveConfirm = false;
  }

  closeLeaveModal() {
    this.showLeaveModal = false;
    this.selectedLeaveUser = null;
    this.currentLeaveAction = null;
    this.showLeaveConfirm = false;
  }

  requestLeaveConfirm() {
    if (this.currentLeaveAction === 'leave') {
      if (!this.leaveCreateForm.fromDate || !this.leaveCreateForm.toDate) {
        this.showMessage('Vui lòng chọn thời gian nghỉ', 'warning'); return;
      }
      if (this.leaveCreateForm.toDate <= this.leaveCreateForm.fromDate) {
        this.showMessage('Ngày kết thúc phải sau ngày bắt đầu', 'warning'); return;
      }
    }
    if (this.currentLeaveAction === 'extend') {
      if (!this.leaveExtendForm.toDate) {
        this.showMessage('Vui lòng chọn ngày kết thúc mới', 'warning'); return;
      }
    }
    this.showLeaveConfirm = true;
  }

  doLeaveAction() {
    this.leaveActionLoading = true;
    const u = this.selectedLeaveUser;
    let action$: any;

    if (this.currentLeaveAction === 'leave') {
      action$ = this.identityService.leaveUser(u.userId, u.employeeCode, {
        fromDate: this.leaveCreateForm.fromDate + 'T00:00:00',
        toDate: this.leaveCreateForm.toDate + 'T00:00:00'
      });
    } else if (this.currentLeaveAction === 'extend') {
      action$ = this.identityService.leaveUser(u.userId, u.employeeCode,
        { toDate: this.leaveExtendForm.toDate + 'T00:00:00' });
    } else {
      action$ = this.identityService.returnUser(u.userId, u.employeeCode);
    }

    action$.subscribe({
      next: () => {
        this.leaveActionLoading = false;
        this.showMessage('Thực hiện thành công!', 'success');
        this.closeLeaveModal();
        this.searchUsers();
      },
      error: (err: any) => {
        this.leaveActionLoading = false;
        this.showLeaveConfirm = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? err.error?.errorDesc ?? 'Không thể thực hiện'}`, 'danger');
      }
    });
  }

  leaveActionLabel(): string {
    switch (this.currentLeaveAction) {
      case 'leave': return 'Tạo đơn nghỉ phép';
      case 'extend': return 'Gia hạn nghỉ phép';
      case 'return': return 'Xác nhận đi làm';
      default: return '';
    }
  }

  // ── Helpers ──────────────────────────────────────────────────────────────

  statusBadge(status: string): string {
    switch (status) {
      case 'ACTIVE': return 'bg-success';
      case 'INACTIVE': return 'bg-secondary';
      case 'LOCKED': return 'bg-danger';
      case 'DELETED': return 'bg-dark';
      default: return 'bg-light text-dark';
    }
  }

  tabLabel(tab: LifecycleTab): string {
    switch (tab) {
      case 'onboard': return 'Tiếp nhận';
      case 'offboard': return 'Thôi việc';
      case 'leave': return 'Nghỉ phép';
      case 'transfer': return 'Luân chuyển';
    }
  }

  private flattenDepts(nodes: any[], depth = 0): any[] {
    const result: any[] = [];
    for (const n of nodes) {
      result.push({ id: n.id, name: n.name, depth, label: '  '.repeat(depth) + n.name });
      if (n.children?.length) result.push(...this.flattenDepts(n.children, depth + 1));
    }
    return result;
  }

  getDeptName(id: any): string {
    return this.flatDepartments.find(d => String(d.id) === String(id))?.name ?? '—';
  }

  getPosName(code: any): string {
    if (!code) return '—';
    return this.positions.find(p => String(p.code) === String(code))?.name ?? '—';
  }

  getRoleName(id: any): string {
    if (!id) return '—';
    return this.roles.find(r => String(r.id) === String(id))?.name ?? '—';
  }

  toggleRole(roleId: any, form: 'onboard' | 'transfer') {
    const arr = form === 'onboard' ? this.onboardForm.roleIds : this.transferForm.roleIds;
    const idx = arr.findIndex((id: any) => String(id) === String(roleId));
    if (idx >= 0) arr.splice(idx, 1);
    else arr.push(roleId);
  }

  isRoleSelected(roleId: any, form: 'onboard' | 'transfer'): boolean {
    const arr = form === 'onboard' ? this.onboardForm.roleIds : this.transferForm.roleIds;
    return arr.some((id: any) => String(id) === String(roleId));
  }

  getSelectedRoleNames(form: 'onboard' | 'transfer'): string {
    const ids = form === 'onboard' ? this.onboardForm.roleIds : this.transferForm.roleIds;
    return ids.map((id: any) => this.getRoleName(id)).join(', ') || '—';
  }

  private showMessage(msg: string, type: string) {
    this.message = msg; this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }
}
