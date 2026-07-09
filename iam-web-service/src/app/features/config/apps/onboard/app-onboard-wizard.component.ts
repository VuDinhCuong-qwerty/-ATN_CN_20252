import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppApiService } from '../../../../shared/services/app-api.service';
import { PermissionService } from '../../../../core/auth/permission.service';

interface StepDef {
  key: string;
  label: string;
}

const ALL_STEPS: StepDef[] = [
  { key: 'APPLICATION', label: 'Thông tin ứng dụng' },
  { key: 'RESOURCE', label: 'Tài nguyên' },
  { key: 'DEFAULT_PERMISSION', label: 'Quyền mặc định' },
  { key: 'CLIENT', label: 'OAuth2 Client' },
  { key: 'CLIENT_METHOD', label: 'Phương thức xác thực' },
  { key: 'AUTH_FLOW', label: 'Luồng MFA' },
];

@Component({
  selector: 'app-app-onboard-wizard',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './app-onboard-wizard.component.html',
  styleUrl: './app-onboard-wizard.component.css'
})
export class AppOnboardWizardComponent implements OnInit {

  visibleSteps: StepDef[] = ALL_STEPS;
  currentIndex = 0;

  appId: number | null = null;
  appType = '';
  loadingResume = false;
  finished = false;

  message = '';
  messageType = '';

  // ── Step 1: Application ──────────────────────────────────────────────────
  flatDepartments: any[] = [];
  appForm: any = {
    name: '', serviceCode: '', appType: 'INTERNAL', acrLevel: 2,
    description: '', logoUri: '', defaultUrl: '', departmentId: null, groupId: null
  };
  loadingStep1 = false;

  // ── Step 2: Resource ─────────────────────────────────────────────────────
  resourceForm: any = { resourceCode: '', resourceName: '', resourceType: 'ENDPOINT', actions: 'read', ldapGroupName: '', description: '' };
  pendingResources: any[] = [];
  loadingStep2 = false;

  // ── Step 3: Default Permission ───────────────────────────────────────────
  roles: any[] = [];
  positions: any[] = [];
  defaultPermTab: 'app' | 'resource' = 'app';
  defaultAppForm: any = { roleId: '', positionCode: '' };
  appResources: any[] = [];
  defaultResForm: any = { roleId: '', positionCode: '', resourceId: '' };
  defaultResActions: string[] = [];
  defaultResSelectedActions: string[] = [];
  loadingStep3 = false;

  // ── Step 4: OAuth2 Client ────────────────────────────────────────────────
  clientForm: any = {};
  createGrantTypes: any = { authorization_code: true, refresh_token: true };
  createScopes: any = { openid: true, profile: true };
  createRedirectUris = '';
  createdSecret = '';
  loadingStep4 = false;

  // ── Step 5: Client Method ────────────────────────────────────────────────
  authMethods: any[] = [];
  selectedMethodIds: number[] = [];
  loadingStep5 = false;

  // ── Step 6: Auth Flow ────────────────────────────────────────────────────
  selectedMethods: any[] = []; // ordered — dùng cho chuỗi tuyến tính
  flowForm: any = { alias: '', description: '' };
  loadingStep6 = false;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public perm: PermissionService,
    private appApi: AppApiService
  ) {}

  ngOnInit() {
    this.appApi.getDepartments().subscribe({ next: res => this.flatDepartments = this.flattenDepts(res.data ?? []) });
    this.appApi.getRoles().subscribe({ next: res => this.roles = res.data ?? [] });
    this.appApi.getPositions().subscribe({ next: res => this.positions = res.data ?? [] });

    const resumeAppId = this.route.snapshot.queryParamMap.get('appId');
    if (resumeAppId) {
      this.resumeFrom(Number(resumeAppId));
    }
  }

  private flattenDepts(nodes: any[], depth = 0): any[] {
    const result: any[] = [];
    for (const n of nodes) {
      result.push({ id: n.id, name: n.name, depth, label: '  '.repeat(depth) + n.name });
      if (n.children?.length) result.push(...this.flattenDepts(n.children, depth + 1));
    }
    return result;
  }

  private resumeFrom(appId: number) {
    this.loadingResume = true;
    this.appApi.getSetupStatus(appId).subscribe({
      next: res => {
        this.loadingResume = false;
        const data = res.data;
        this.appId = appId;
        this.appType = data.appType;
        this.visibleSteps = data.appType === 'THIRD_PARTY_LDAP' ? ALL_STEPS.slice(0, 3) : ALL_STEPS;
        if (data.overallComplete) {
          this.finished = true;
          return;
        }
        const idx = this.visibleSteps.findIndex((s: StepDef) => s.key === data.nextStep);
        this.currentIndex = idx >= 0 ? idx : 0;
        if (this.currentIndex === 2) this.loadAppResourcesForStep3();
      },
      error: () => { this.loadingResume = false; }
    });
  }

  get currentStep(): StepDef { return this.visibleSteps[this.currentIndex]; }

  private showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }

  // ── Step 1 ────────────────────────────────────────────────────────────────
  submitStep1() {
    if (!this.appForm.name?.trim() || !this.appForm.serviceCode?.trim()) {
      this.showMessage('Tên và Service Code là bắt buộc', 'danger'); return;
    }
    if (!this.appForm.logoUri?.trim() || !this.appForm.defaultUrl?.trim()) {
      this.showMessage('Logo URL và Default URL là bắt buộc', 'danger'); return;
    }
    if (this.appForm.departmentId == null) {
      this.showMessage('Vui lòng chọn phòng ban', 'danger'); return;
    }
    this.loadingStep1 = true;
    this.appApi.createApplication(this.appForm).subscribe({
      next: res => {
        this.loadingStep1 = false;
        this.appId = res.data.id;
        this.appType = res.data.appType;
        this.visibleSteps = this.appType === 'THIRD_PARTY_LDAP' ? ALL_STEPS.slice(0, 3) : ALL_STEPS;
        this.showMessage('Đã tạo ứng dụng. Tiếp tục cấu hình tài nguyên.', 'success');
        this.currentIndex = 1;
      },
      error: err => {
        this.loadingStep1 = false;
        this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger');
      }
    });
  }

  // ── Step 2 ────────────────────────────────────────────────────────────────
  addPendingResource() {
    if (!this.resourceForm.resourceCode?.trim() || !this.resourceForm.resourceName?.trim()) {
      this.showMessage('Resource Code và Tên là bắt buộc', 'danger'); return;
    }
    const actionsStr = (this.resourceForm.actions || '').split(',').map((a: string) => a.trim()).filter(Boolean).join(',');
    if (!actionsStr) { this.showMessage('Actions không được để trống', 'danger'); return; }
    this.pendingResources.push({
      resourceCode: this.resourceForm.resourceCode,
      resourceName: this.resourceForm.resourceName,
      resourceType: this.resourceForm.resourceType,
      actions: actionsStr,
      ldapGroupName: this.resourceForm.ldapGroupName || null,
      description: this.resourceForm.description || null
    });
    this.resourceForm = { resourceCode: '', resourceName: '', resourceType: 'ENDPOINT', actions: 'read', ldapGroupName: '', description: '' };
  }

  removePendingResource(i: number) { this.pendingResources.splice(i, 1); }

  submitStep2() {
    if (!this.pendingResources.length) {
      this.showMessage('Thêm ít nhất 1 tài nguyên trước khi tiếp tục (hoặc bấm Bỏ qua).', 'danger'); return;
    }
    this.loadingStep2 = true;
    this.appApi.createResources(this.appId!, { items: this.pendingResources }).subscribe({
      next: () => {
        this.loadingStep2 = false;
        this.showMessage('Đã tạo tài nguyên.', 'success');
        this.loadAppResourcesForStep3();
        this.currentIndex = 2;
      },
      error: err => {
        this.loadingStep2 = false;
        this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger');
      }
    });
  }

  skipStep2() { this.currentIndex = 2; this.loadAppResourcesForStep3(); }

  // ── Step 3 ────────────────────────────────────────────────────────────────
  private loadAppResourcesForStep3() {
    if (!this.appId) return;
    this.appApi.getResources(this.appId, { size: 200 }).subscribe({
      next: res => this.appResources = res.data?.content ?? res.data ?? []
    });
  }

  submitDefaultApp() {
    if (!this.defaultAppForm.roleId || !this.defaultAppForm.positionCode) {
      this.showMessage('Chọn role và chức danh', 'danger'); return;
    }
    this.loadingStep3 = true;
    this.appApi.createDefaultAppPerms({
      items: [{ roleId: this.defaultAppForm.roleId, positionCode: this.defaultAppForm.positionCode, applicationId: this.appId }]
    }).subscribe({
      next: res => {
        this.loadingStep3 = false;
        const count = res.data?.[0]?.affectedUserCount;
        this.showMessage(`Đã thêm quyền mặc định.${count != null ? ' Kích hoạt cho ' + count + ' user hiện có.' : ''}`, 'success');
        this.defaultAppForm = { roleId: '', positionCode: '' };
      },
      error: err => { this.loadingStep3 = false; this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger'); }
    });
  }

  onDefaultResResourceChange() {
    this.defaultResSelectedActions = [];
    const r = this.appResources.find(x => String(x.id) === String(this.defaultResForm.resourceId));
    this.defaultResActions = r?.actions
      ? (Array.isArray(r.actions) ? r.actions : r.actions.split(',').map((a: string) => a.trim())).filter(Boolean)
      : [];
  }

  toggleDefaultResAction(action: string) {
    const idx = this.defaultResSelectedActions.indexOf(action);
    if (idx >= 0) this.defaultResSelectedActions.splice(idx, 1);
    else this.defaultResSelectedActions.push(action);
  }

  submitDefaultResource() {
    if (!this.defaultResForm.roleId || !this.defaultResForm.positionCode || !this.defaultResForm.resourceId || !this.defaultResSelectedActions.length) {
      this.showMessage('Điền đầy đủ role/chức danh/resource và chọn ít nhất 1 action', 'danger'); return;
    }
    this.loadingStep3 = true;
    this.appApi.createDefaultResourcePerms({
      items: [{
        roleId: Number(this.defaultResForm.roleId),
        positionCode: this.defaultResForm.positionCode,
        resourceId: Number(this.defaultResForm.resourceId),
        actions: this.defaultResSelectedActions
      }]
    }).subscribe({
      next: res => {
        this.loadingStep3 = false;
        const count = res.data?.[0]?.affectedUserCount;
        this.showMessage(`Đã thêm quyền mặc định.${count != null ? ' Kích hoạt cho ' + count + ' user hiện có.' : ''}`, 'success');
        this.defaultResForm = { roleId: '', positionCode: '', resourceId: '' };
        this.defaultResActions = []; this.defaultResSelectedActions = [];
      },
      error: err => { this.loadingStep3 = false; this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger'); }
    });
  }

  finishStep3() {
    if (this.appType === 'THIRD_PARTY_LDAP') { this.finished = true; return; }
    this.currentIndex = 3;
  }

  // ── Step 4 ────────────────────────────────────────────────────────────────
  openClientForm() {
    this.clientForm = {
      clientId: '', name: '', type: 'public',
      logoUri: '', description: '', defaultUrl: '', postLogoutRedirect: '',
      tokenEndpointAuth: 'none', accessTokenTtl: 3600,
      refreshTokenTtl: 86400, idTokenTtl: 3600,
      requirePkce: true, requireConsent: false
    };
    this.createGrantTypes = { authorization_code: true, refresh_token: true };
    this.createScopes = { openid: true, profile: true };
    this.createRedirectUris = '';
    this.createdSecret = '';
  }

  submitStep4() {
    if (!this.clientForm.clientId?.trim() || !this.clientForm.name?.trim()) {
      this.showMessage('Client ID và Tên là bắt buộc', 'danger'); return;
    }
    this.loadingStep4 = true;
    const body = {
      ...this.clientForm,
      appId: this.appId,
      grantTypes: Object.keys(this.createGrantTypes).filter(k => this.createGrantTypes[k]),
      scopes: Object.keys(this.createScopes).filter(k => this.createScopes[k]),
      redirectUris: this.createRedirectUris.trim(),
    };
    this.appApi.createClient(body).subscribe({
      next: res => {
        this.loadingStep4 = false;
        this.createdSecret = res.data?.clientSecret ?? '';
        this.showMessage('Đã tạo OAuth2 Client.' + (this.createdSecret ? ' Lưu client secret bên dưới — chỉ hiển thị 1 lần.' : ''), 'success');
        this.loadAuthMethods();
        this.currentIndex = 4;
      },
      error: err => { this.loadingStep4 = false; this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger'); }
    });
  }

  // ── Step 5 ────────────────────────────────────────────────────────────────
  private loadAuthMethods() {
    this.appApi.getAuthMethods().subscribe({ next: res => this.authMethods = res.data ?? [] });
  }

  toggleMethod(id: number) {
    const idx = this.selectedMethodIds.indexOf(id);
    if (idx >= 0) this.selectedMethodIds.splice(idx, 1);
    else this.selectedMethodIds.push(id);
  }

  submitStep5() {
    if (!this.selectedMethodIds.length) {
      this.showMessage('Chọn ít nhất 1 phương thức xác thực', 'danger'); return;
    }
    this.loadingStep5 = true;
    const items = this.selectedMethodIds.map(id => {
      const m = this.authMethods.find(x => x.id === id);
      return { methodId: id, methodName: m?.method, config: {} };
    });
    this.appApi.createMethods(this.appId!, { items }).subscribe({
      next: () => {
        this.loadingStep5 = false;
        this.showMessage('Đã cấu hình phương thức xác thực.', 'success');
        this.selectedMethods = this.selectedMethodIds.map(id => this.authMethods.find(x => x.id === id));
        this.flowForm = { alias: (this.appForm.serviceCode || 'app') + '-default', description: '' };
        this.currentIndex = 5;
      },
      error: err => { this.loadingStep5 = false; this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger'); }
    });
  }

  // ── Step 6 ────────────────────────────────────────────────────────────────
  moveMethod(i: number, dir: -1 | 1) {
    const j = i + dir;
    if (j < 0 || j >= this.selectedMethods.length) return;
    [this.selectedMethods[i], this.selectedMethods[j]] = [this.selectedMethods[j], this.selectedMethods[i]];
  }

  submitStep6() {
    if (!this.flowForm.alias?.trim()) { this.showMessage('Alias không được để trống', 'danger'); return; }
    if (!this.selectedMethods.length) { this.showMessage('Chưa có phương thức nào để tạo luồng', 'danger'); return; }

    const executions = this.selectedMethods.map((m, i) => ({
      nodeId: i + 1,
      parentNodeId: i === 0 ? null : i,
      methodId: m.id,
      requirement: 'REQUIRED',
      isDefault: 1
    }));

    this.loadingStep6 = true;
    this.appApi.createFlow(this.appId!, { alias: this.flowForm.alias, description: this.flowForm.description, executions }).subscribe({
      next: () => {
        this.loadingStep6 = false;
        this.showMessage('Đã tạo luồng MFA. Hoàn tất thiết lập ứng dụng!', 'success');
        this.finished = true;
      },
      error: err => { this.loadingStep6 = false; this.showMessage(err?.error?.errorDesc ?? 'Có lỗi xảy ra', 'danger'); }
    });
  }

  goToDetail() {
    this.router.navigate(['/config/apps/detail'], { queryParams: { appId: this.appId } });
  }

  goBack() {
    if (this.currentIndex > 0) this.currentIndex--;
  }
}
