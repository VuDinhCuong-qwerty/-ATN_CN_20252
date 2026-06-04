import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { PermissionService } from '../../../../core/auth/permission.service';
import { AppApiService } from '../../../../shared/services/app-api.service';

const GRANT_TYPE_OPTIONS = [
  'authorization_code', 'refresh_token', 'client_credentials', 'password'
];
const TOKEN_ENDPOINT_AUTH_OPTIONS = ['none', 'client_secret_basic', 'client_secret_post'];

@Component({
  selector: 'app-client-detail',
  imports: [CommonModule, FormsModule],
  templateUrl: './client-detail.component.html',
  styleUrl: './client-detail.component.css'
})
export class ClientDetailComponent implements OnInit {
  clientId = '';    // OAuth2 string clientId — dùng cho GET /clients/{clientId}
  internalId: any = null;  // numeric id — dùng cho update/reset/scopes
  loading = false;
  detail: any = null;

  activeTab: 'detail' | 'scopes' | 'reset' = 'detail';

  // Tab 1 — inline edit
  editForm: any = {};
  editGrantTypes: Record<string, boolean> = {};
  loadingSave = false;
  saveMessage = '';
  saveMessageType = '';

  // Tab 2 — scopes
  scopeList: string[] = [];
  newScopeInput = '';
  loadingScope = false;
  scopeMessage = '';
  scopeMessageType = '';

  // Tab 3 — reset secret
  resetting = false;
  resetDone = false;
  newSecret = '';
  resetError = '';

  readonly grantTypeOptions = GRANT_TYPE_OPTIONS;
  readonly tokenEndpointAuthOptions = TOKEN_ENDPOINT_AUTH_OPTIONS;

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public perm: PermissionService,
    private appApiService: AppApiService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(p => {
      this.clientId = p['clientId'];
      this.internalId = p['id'];
      if (this.clientId) this.loadDetail();
    });
  }

  loadDetail() {
    this.loading = true;
    this.appApiService.getClientDetail(this.clientId).subscribe({
      next: res => {
        this.loading = false;
        this.detail = res.data ?? res;
        this.initEditForm();
        this.initScopes();
      },
      error: () => { this.loading = false; }
    });
  }

  initEditForm() {
    const d = this.detail;
    const existingGrants = (d.grantTypes ?? '').split(',').map((s: string) => s.trim()).filter(Boolean);
    this.editGrantTypes = {};
    for (const g of GRANT_TYPE_OPTIONS) {
      this.editGrantTypes[g] = existingGrants.includes(g);
    }
    this.editForm = {
      name: d.name ?? '',
      logoUri: d.logoUri ?? '',
      description: d.description ?? '',
      defaultUrl: d.defaultUrl ?? '',
      postLogoutRedirect: d.postLogoutRedirect ?? '',
      tokenEndpointAuth: d.tokenEndpointAuth ?? 'none',
      accessTokenTtl: d.accessTokenTtl ?? 3600,
      refreshTokenTtl: d.refreshTokenTtl ?? 86400,
      idTokenTtl: d.idTokenTtl ?? 3600,
      requirePkce: d.requirePkce ?? false,
      enabled: d.enabled === 1 || d.enabled === true,
      redirectUris: d.redirectUris ?? '',
    };
  }

  initScopes() {
    const s = this.detail?.allowedScopes ?? '';
    this.scopeList = s.split(' ').map((x: string) => x.trim()).filter(Boolean);
  }

  resetForm() { this.initEditForm(); this.saveMessage = ''; }

  save() {
    if (!this.editForm.name?.trim()) {
      this.saveMessage = 'Tên client không được để trống';
      this.saveMessageType = 'warning';
      return;
    }
    this.loadingSave = true;
    const body = {
      ...this.editForm,
      enabled: !!this.editForm.enabled,
      grantTypes: Object.keys(this.editGrantTypes).filter(k => this.editGrantTypes[k]),
    };
    this.appApiService.updateClient(this.internalId, body).subscribe({
      next: () => {
        this.loadingSave = false;
        this.saveMessage = 'Cập nhật thành công';
        this.saveMessageType = 'success';
        this.loadDetail();
        setTimeout(() => this.saveMessage = '', 4000);
      },
      error: err => {
        this.loadingSave = false;
        this.saveMessage = `Lỗi: ${err.error?.errorDesc ?? err.error?.message ?? 'Không thể cập nhật'}`;
        this.saveMessageType = 'danger';
      }
    });
  }

  // === Scopes ===
  addScope() {
    const scope = this.newScopeInput.trim();
    if (!scope) return;
    this.loadingScope = true;
    this.appApiService.manageClientScopes(this.internalId, { action: 'add', scopes: [scope] }).subscribe({
      next: () => {
        if (!this.scopeList.includes(scope)) this.scopeList = [...this.scopeList, scope];
        this.newScopeInput = '';
        this.loadingScope = false;
        this.showScopeMsg(`Đã thêm scope "${scope}"`, 'success');
      },
      error: err => {
        this.loadingScope = false;
        this.showScopeMsg(`Lỗi: ${err.error?.errorDesc ?? err.error?.message ?? 'Không thể thêm'}`, 'danger');
      }
    });
  }

  removeScope(scope: string) {
    this.loadingScope = true;
    this.appApiService.manageClientScopes(this.internalId, { action: 'delete', scopes: [scope] }).subscribe({
      next: () => {
        this.scopeList = this.scopeList.filter(s => s !== scope);
        this.loadingScope = false;
        this.showScopeMsg(`Đã xoá scope "${scope}"`, 'success');
      },
      error: err => {
        this.loadingScope = false;
        this.showScopeMsg(`Lỗi: ${err.error?.errorDesc ?? err.error?.message ?? 'Không thể xoá'}`, 'danger');
      }
    });
  }

  private showScopeMsg(msg: string, type: string) {
    this.scopeMessage = msg; this.scopeMessageType = type;
    setTimeout(() => this.scopeMessage = '', 3000);
  }

  // === Reset Secret ===
  confirmResetSecret() {
    this.resetting = true;
    this.resetError = '';
    this.appApiService.resetClientSecret(this.internalId).subscribe({
      next: res => {
        this.resetting = false;
        this.newSecret = res.data?.clientSecret ?? res.data ?? '';
        this.resetDone = true;
      },
      error: err => {
        this.resetting = false;
        this.resetError = err.error?.errorDesc ?? err.error?.message ?? 'Không thể reset secret';
      }
    });
  }

  goBack() { this.router.navigate(['/config/clients']); }

  statusBadge(enabled: any): string {
    return enabled === 1 || enabled === true ? 'bg-success' : 'bg-secondary';
  }

  statusLabel(enabled: any): string {
    return enabled === 1 || enabled === true ? 'ACTIVE' : 'INACTIVE';
  }
}
