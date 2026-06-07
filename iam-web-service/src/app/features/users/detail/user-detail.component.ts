import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';
import { PermissionService } from '../../../core/auth/permission.service';
import { IdentityService } from '../../../shared/services/identity.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-user-detail',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-detail.component.html',
  styleUrl: './user-detail.component.css'
})
export class UserDetailComponent implements OnInit {
  detailUser: any = null;
  loading = false;
  userId: any = null;
  employeeCode = '';
  selfProfile = false;

  editForm: any = {};
  addressForms: any[] = [];
  loadingUpdate = false;
  message = '';
  messageType = '';

  // Quyền hiện tại (ADMIN only)
  detailAppPerms: any[] = [];
  detailResPerms: any[] = [];
  loadingDetailPerms = false;
  detailPermsSubTab: 'app' | 'resource' = 'app';
  revokeTarget: any = null;
  revokeType: 'app' | 'resource' = 'app';
  revokeReason = '';
  loadingRevoke = false;

  departments: any[] = [];
  flatDepartments: any[] = [];
  positions: any[] = [];

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.userId = params['userId'] ? Number(params['userId']) : null;
      this.employeeCode = params['employeeCode'] ?? '';
      this.selfProfile = params['selfProfile'] === 'true';
      this.loadDetail();
    });
    this.appApiService.getDepartments().subscribe({ next: res => {
      this.departments = res.data ?? [];
      this.flatDepartments = this.flattenDepts(this.departments);
    }});
    this.appApiService.getPositions().subscribe({ next: res => this.positions = res.data ?? [] });
  }

  @HostListener('document:click')
  onDocumentClick() {
    this.addressForms.forEach(a => { a.showProvinceSugg = false; a.showWardSugg = false; });
  }

  loadDetail() {
    if (!this.userId && !this.employeeCode) { this.router.navigate(['/users']); return; }
    this.loading = true;
    this.detailUser = null;
    this.identityService.getUserDetail(this.userId, this.employeeCode).subscribe({
      next: res => {
        this.detailUser = res.data;
        this.loading = false;
        this.initForm();
        if (this.isAdmin && !this.selfProfile) this.loadDetailPerms();
      },
      error: () => { this.loading = false; this.router.navigate(['/users']); }
    });
  }

  initForm() {
    if (!this.canEdit) { this.editForm = {}; this.addressForms = []; return; }
    if (this.canEditFull) {
      this.editForm = {
        fullName: this.detailUser.fullName ?? '',
        dob: this.detailUser.dob ?? '',
        gender: this.detailUser.gender ?? 'MALE',
        mobile: this.detailUser.mobile ?? '',
        cccd: this.detailUser.numberId ?? '',
        cccdIssuedDate: this.detailUser.numberIdIssuedDate ?? '',
        cccdIssuedPlace: this.detailUser.numberIdIssuedPlace ?? '',
        nationality: this.detailUser.nationality ?? '',
        ethnic: this.detailUser.ethnic ?? '',
        religion: this.detailUser.religion ?? '',
        departmentId: (() => {
          const depts = this.detailUser.departments as any[];
          if (!depts?.length) return '';
          const maxId = Math.max(...depts.map((d: any) => d.departmentId ?? 0));
          return maxId > 0 ? String(maxId) : '';
        })(),
        position: this.detailUser.positionCode ?? '',
        joinDate: this.detailUser.joinDate ?? '',
      };
      this.addressForms = [];
    } else {
      this.editForm = {
        displayName: this.detailUser.displayName ?? '',
        emailPersonal: this.detailUser.emailPersonal ?? '',
        avatarUrl: this.detailUser.avatarUrl ?? '',
      };
      this.initAddressForms();
    }
  }

  private initAddressForms() {
    const existing = (this.detailUser.addresses as any[]) ?? [];
    this.addressForms = ['PERMANENT', 'TEMPORARY', 'BIRTH'].map(type => {
      const found = existing.find((a: any) => a.type === type);
      return {
        type,
        enabled: !!found,
        provinceCode: found?.provinceCode ?? '',
        provinceName: found?.provinceName ?? '',
        wardCode: found?.wardCode ?? '',
        wardName: found?.wardName ?? '',
        detail: found?.detail ?? '',
        provinceSugg: [],
        wardSugg: [],
        showProvinceSugg: false,
        showWardSugg: false,
      };
    });
  }

  resetForm() { this.initForm(); this.message = ''; }

  // ── Address autocomplete ──────────────────────────────────────────────────

  onAddrProvinceInput(i: number, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.addressForms[i].provinceName = value;
    this.addressForms[i].provinceCode = '';
    this.addressForms[i].wardCode = '';
    this.addressForms[i].wardName = '';
    if (!value) { this.addressForms[i].provinceSugg = []; this.addressForms[i].showProvinceSugg = false; return; }
    this.identityService.getProvinces(value).subscribe({
      next: res => {
        this.addressForms[i].provinceSugg = res.data ?? [];
        this.addressForms[i].showProvinceSugg = true;
      }
    });
  }

  selectAddrProvince(i: number, prov: any) {
    this.addressForms[i].provinceCode = prov.provinceCode;
    this.addressForms[i].provinceName = prov.provinceName;
    this.addressForms[i].wardCode = '';
    this.addressForms[i].wardName = '';
    this.addressForms[i].provinceSugg = [];
    this.addressForms[i].showProvinceSugg = false;
  }

  onAddrWardInput(i: number, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.addressForms[i].wardName = value;
    this.addressForms[i].wardCode = '';
    if (!value || !this.addressForms[i].provinceCode) {
      this.addressForms[i].wardSugg = []; this.addressForms[i].showWardSugg = false; return;
    }
    this.identityService.getWards(this.addressForms[i].provinceCode, value).subscribe({
      next: res => {
        this.addressForms[i].wardSugg = res.data ?? [];
        this.addressForms[i].showWardSugg = true;
      }
    });
  }

  selectAddrWard(i: number, ward: any) {
    this.addressForms[i].wardCode = ward.wardCode;
    this.addressForms[i].wardName = ward.wardName;
    this.addressForms[i].wardSugg = [];
    this.addressForms[i].showWardSugg = false;
  }

  getAddrTypeLabel(type: string): string {
    return type === 'PERMANENT' ? 'Thường trú' : type === 'TEMPORARY' ? 'Tạm trú' : 'Quê quán';
  }

  // ── Permissions ───────────────────────────────────────────────────────────

  get departmentPath(): string {
    const depts = this.detailUser?.departments as any[];
    if (!depts?.length) return '—';
    const leaf = [...depts].sort((a, b) => (b.departmentId ?? 0) - (a.departmentId ?? 0))[0];
    return leaf?.name ?? '—';
  }

  get isAdmin(): boolean { return this.perm.userInfo?.role === 'ADMIN'; }

  get canEditFull(): boolean {
    if (this.selfProfile) return false;
    return this.perm.has('user', 'update') && this.perm.userInfo?.role === 'ADMIN';
  }

  get canEditPersonal(): boolean {
    if (this.selfProfile) return this.perm.has('user', 'update');
    return this.perm.has('user', 'update') && !this.canEditFull;
  }

  get canEdit(): boolean { return this.canEditFull || this.canEditPersonal; }

  // ── Save ──────────────────────────────────────────────────────────────────

  private buildBody(form: any): any {
    const body: any = {};
    for (const [key, val] of Object.entries(form)) {
      if (val !== '' && val !== null && val !== undefined) body[key] = val;
    }
    return body;
  }

  save() {
    this.loadingUpdate = true;
    const empCode = this.detailUser.employeeCode;

    if (this.canEditFull) {
      const body = this.buildBody(this.editForm);
      this.identityService.updateUserProfile(empCode, body).subscribe({
        next: () => { this.loadingUpdate = false; this.showMessage('Cập nhật thành công!', 'success'); this.loadDetail(); },
        error: err => { this.loadingUpdate = false; this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể cập nhật'}`, 'danger'); }
      });
      return;
    }

    // Personal mode: main fields + one call per enabled address
    const mainBody = this.buildBody(this.editForm);
    const calls: Observable<any>[] = [this.identityService.updateUserPersonal(empCode, mainBody)];

    for (const addr of this.addressForms) {
      if (addr.enabled && addr.wardCode && addr.provinceCode) {
        calls.push(this.identityService.updateUserPersonal(empCode, {
          address: {
            type: addr.type,
            wardCode: Number(addr.wardCode),
            provinceCode: Number(addr.provinceCode),
            detail: addr.detail || null
          }
        }));
      }
    }

    forkJoin(calls).subscribe({
      next: () => { this.loadingUpdate = false; this.showMessage('Cập nhật thành công!', 'success'); this.loadDetail(); },
      error: err => { this.loadingUpdate = false; this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể cập nhật'}`, 'danger'); }
    });
  }

  // ── Helpers ───────────────────────────────────────────────────────────────

  private flattenDepts(nodes: any[], depth = 0): any[] {
    const result: any[] = [];
    for (const n of nodes) {
      result.push({ id: n.id, code: n.code, name: n.name, depth });
      if (n.children?.length) result.push(...this.flattenDepts(n.children, depth + 1));
    }
    return result;
  }

  goBack() { this.router.navigate(['/users']); }

  // ── Permissions (ADMIN only) ──────────────────────────────────────────────

  loadDetailPerms() {
    const code = this.employeeCode || this.detailUser?.employeeCode;
    if (!code) return;
    this.loadingDetailPerms = true;
    this.identityService.getAppPermissions(code).subscribe({
      next: res => { this.detailAppPerms = res.data?.content ?? res.data ?? []; },
      error: () => {}
    });
    this.identityService.getResourcePermissions(code).subscribe({
      next: res => { this.loadingDetailPerms = false; this.detailResPerms = res.data?.content ?? res.data ?? []; },
      error: () => { this.loadingDetailPerms = false; }
    });
  }

  openRevokeModal(type: 'app' | 'resource', p: any, event: Event) {
    event.stopPropagation();
    this.revokeType = type;
    this.revokeTarget = p;
    this.revokeReason = '';
  }

  closeRevokeModal() { this.revokeTarget = null; }

  confirmRevoke() {
    if (!this.revokeTarget) return;
    const code = this.employeeCode || this.detailUser?.employeeCode;
    this.loadingRevoke = true;
    const obs = this.revokeType === 'app'
      ? this.identityService.revokeAppPermission(code, { apps: [this.revokeTarget.appId], reason: this.revokeReason })
      : this.identityService.revokeResourcePermission(code, { resourceIds: [this.revokeTarget.resourceId], reason: this.revokeReason });
    obs.subscribe({
      next: () => {
        this.loadingRevoke = false;
        this.revokeTarget = null;
        this.showMessage('Thu hồi quyền thành công', 'success');
        this.loadDetailPerms();
      },
      error: err => {
        this.loadingRevoke = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể thu hồi'}`, 'danger');
      }
    });
  }

  private showMessage(msg: string, type: string) {
    this.message = msg; this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }
}
