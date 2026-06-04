import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PermissionService } from '../../../core/auth/permission.service';
import { AppApiService } from '../../../shared/services/app-api.service';

const GRANT_TYPE_OPTIONS = [
  'authorization_code', 'refresh_token', 'client_credentials', 'password'
];
const SCOPE_OPTIONS = ['openid', 'profile', 'email', 'phone'];
const TOKEN_ENDPOINT_AUTH_OPTIONS = ['none', 'client_secret_basic', 'client_secret_post'];

@Component({
  selector: 'app-client-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './client-list.component.html',
  styleUrl: './client-list.component.css'
})
export class ClientListComponent implements OnInit {
  applications: any[] = [];

  filterAppId: any = '';
  filterStatus = '';
  filterGrantType = '';
  filterClientId = '';

  clients: any[] = [];
  loading = false;
  currentPage = 0;
  pageSize = 10;
  totalPages = 0;
  totalElements = 0;

  // Create modal
  showCreateModal = false;
  createForm: any = {};
  createGrantTypes: Record<string, boolean> = { authorization_code: true, refresh_token: true };
  createScopes: Record<string, boolean> = { openid: true, profile: true };
  createRedirectUris = '';
  loadingCreate = false;
  createMessage = '';
  createMessageType = '';
  createdSecret = '';

  message = '';
  messageType = '';

  readonly grantTypeOptions = GRANT_TYPE_OPTIONS;
  readonly scopeOptions = SCOPE_OPTIONS;
  readonly tokenEndpointAuthOptions = TOKEN_ENDPOINT_AUTH_OPTIONS;

  constructor(
    public perm: PermissionService,
    private appApiService: AppApiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.appApiService.getApplications({ size: 200 }).subscribe({
      next: res => {
        this.applications = res.data?.content ?? res.data ?? [];
        this.loadClients();
      }
    });
  }

  loadClients() {
    this.loading = true;
    const q: any = { page: this.currentPage, size: this.pageSize };
    if (this.filterAppId) q.appId = this.filterAppId;
    if (this.filterStatus) q.status = this.filterStatus;
    if (this.filterGrantType) q.grantType = this.filterGrantType;
    if (this.filterClientId) q.clientId = this.filterClientId;
    this.appApiService.getClients(q).subscribe({
      next: res => {
        this.loading = false;
        const data = res.data;
        if (data?.content) {
          this.clients = data.content;
          this.totalElements = data.totalElements ?? 0;
          this.totalPages = data.totalPages ?? 1;
        } else {
          this.clients = data ?? [];
          this.totalElements = this.clients.length;
          this.totalPages = 1;
        }
      },
      error: () => { this.loading = false; }
    });
  }

  search() { this.currentPage = 0; this.loadClients(); }

  clearSearch() {
    this.filterAppId = '';
    this.filterStatus = '';
    this.filterGrantType = '';
    this.filterClientId = '';
    this.currentPage = 0;
    this.loadClients();
  }

  goPage(page: number) {
    if (page < 0 || page >= this.totalPages) return;
    this.currentPage = page; this.loadClients();
  }

  getVisiblePages(): number[] {
    const pages: number[] = [];
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  goToDetail(c: any) {
    this.router.navigate(['/config/clients/detail'], {
      queryParams: { clientId: c.clientId, id: c.id }
    });
  }

  getGrantTypeList(grantTypes: string): string[] {
    return (grantTypes ?? '').split(',').map((s: string) => s.trim()).filter(Boolean);
  }

  statusBadge(enabled: any): string {
    return enabled === 1 ? 'bg-success' : 'bg-secondary';
  }

  statusLabel(enabled: any): string {
    return enabled === 1 ? 'ACTIVE' : 'INACTIVE';
  }

  // === Create ===
  openCreate() {
    this.showCreateModal = true;
    this.createdSecret = '';
    this.createMessage = '';
    this.createForm = {
      clientId: '', name: '', type: 'public',
      appId: this.filterAppId || (this.applications[0]?.id ?? ''),
      logoUri: '', description: '', defaultUrl: '', postLogoutRedirect: '',
      tokenEndpointAuth: 'none', accessTokenTtl: 3600,
      refreshTokenTtl: 86400, idTokenTtl: 3600,
      requirePkce: true, requireConsent: false
    };
    this.createGrantTypes = { authorization_code: true, refresh_token: true };
    this.createScopes = { openid: true, profile: true };
    this.createRedirectUris = '';
  }

  closeCreate() { this.showCreateModal = false; this.createdSecret = ''; }

  submitCreate() {
    if (!this.createForm.clientId?.trim() || !this.createForm.name?.trim()) {
      this.createMessage = 'Client ID và Tên là bắt buộc'; this.createMessageType = 'warning'; return;
    }
    this.loadingCreate = true;
    const body = {
      ...this.createForm,
      appId: Number(this.createForm.appId),
      grantTypes: Object.keys(this.createGrantTypes).filter(k => this.createGrantTypes[k]),
      scopes: Object.keys(this.createScopes).filter(k => this.createScopes[k]),
      redirectUris: this.createRedirectUris.trim(),
    };
    this.appApiService.createClient(body).subscribe({
      next: res => {
        this.loadingCreate = false;
        this.createdSecret = res.data?.clientSecret ?? '';
        this.createMessage = 'Tạo client thành công!' + (this.createdSecret ? ' Lưu client secret bên dưới — chỉ hiển thị một lần.' : '');
        this.createMessageType = 'success';
        this.loadClients();
      },
      error: err => {
        this.loadingCreate = false;
        this.createMessage = `Lỗi: ${err.error?.message ?? err.error?.errorDesc ?? 'Không thể tạo client'}`;
        this.createMessageType = 'danger';
      }
    });
  }
}
