import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppApiService } from '../../../../shared/services/app-api.service';
import { PermissionService } from '../../../../core/auth/permission.service';
import { ResourceListComponent } from '../../resources/resource-list.component';
import { ClientListComponent } from '../../clients/client-list.component';
import { FlowListComponent } from '../../flows/flow-list.component';
import { DefaultPermListComponent } from '../../default-perms/default-perm-list.component';

type ConfigTab = 'INFO' | 'RESOURCE' | 'CLIENT' | 'AUTH_FLOW' | 'DEFAULT_PERMISSION';

@Component({
  selector: 'app-app-detail',
  standalone: true,
  imports: [
    CommonModule, FormsModule, RouterLink,
    ResourceListComponent, ClientListComponent, FlowListComponent, DefaultPermListComponent
  ],
  templateUrl: './app-detail.component.html',
  styleUrl: './app-detail.component.css'
})
export class AppDetailComponent implements OnInit {
  appId: any = null;
  detail: any = null;
  loading = false;
  departments: any[] = [];
  flatDepartments: any[] = [];

  editForm: any = {};
  loadingSave = false;
  message = '';
  messageType = '';
  loadingToggle = false;

  // ── Setup status (dùng để lọc tab Client/Auth Flow cho app THIRD_PARTY_LDAP) ──
  setupStatus: any = null;
  activeConfigTab: ConfigTab = 'INFO';
  readonly configTabDefs: { key: ConfigTab; label: string; icon: string }[] = [
    { key: 'INFO', label: 'Thông tin chi tiết', icon: 'bi-info-circle' },
    { key: 'RESOURCE', label: 'Tài nguyên', icon: 'bi-boxes' },
    { key: 'CLIENT', label: 'OAuth2 Client', icon: 'bi-plug' },
    { key: 'AUTH_FLOW', label: 'Luồng MFA', icon: 'bi-diagram-3' },
    { key: 'DEFAULT_PERMISSION', label: 'Quyền mặc định', icon: 'bi-shield-check' },
  ];

  get visibleConfigTabs() {
    if (this.setupStatus?.appType === 'THIRD_PARTY_LDAP') {
      return this.configTabDefs.filter(t => t.key !== 'CLIENT' && t.key !== 'AUTH_FLOW');
    }
    return this.configTabDefs;
  }

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    public perm: PermissionService,
    private appApi: AppApiService
  ) {}

  ngOnInit() {
    this.appId = this.route.snapshot.queryParamMap.get('appId');
    this.appApi.getDepartments().subscribe({
      next: res => {
        this.departments = res.data ?? [];
        this.flatDepartments = this.flattenDepts(this.departments);
      }
    });
    if (this.appId) {
      this.loadDetail();
      this.loadSetupStatus();
    }
  }

  loadDetail() {
    this.loading = true;
    this.appApi.getAppDetail(this.appId).subscribe({
      next: res => {
        this.loading = false;
        this.detail = res.data;
        this.initForm();
      },
      error: () => { this.loading = false; }
    });
  }

  loadSetupStatus() {
    this.appApi.getSetupStatus(Number(this.appId)).subscribe({
      next: res => {
        this.setupStatus = res.data;
        const stillVisible = this.visibleConfigTabs.some(t => t.key === this.activeConfigTab);
        if (!stillVisible) this.activeConfigTab = 'INFO';
      }
    });
  }

  isStepDone(key: ConfigTab): boolean {
    return !!this.setupStatus?.steps?.find((s: any) => s.key === key)?.done;
  }

  missingStepLabels(): string {
    return (this.setupStatus?.steps ?? [])
      .filter((s: any) => s.required && !s.done)
      .map((s: any) => s.label)
      .join(', ');
  }

  resumeOnboarding() {
    this.router.navigate(['/config/apps/onboard'], { queryParams: { appId: this.appId } });
  }

  switchConfigTab(tab: ConfigTab) { this.activeConfigTab = tab; }

  initForm() {
    const d = this.detail;
    if (!d) return;
    this.editForm = {
      name: d.name ?? '',
      description: d.description ?? '',
      appType: d.appType ?? 'INTERNAL',
      logoUri: d.logoUri ?? '',
      defaultUrl: d.defaultUrl ?? '',
      serviceCode: d.serviceCode ?? '',
      departmentId: d.departmentId ?? null,
      groupId: d.groupId ?? null,
      acrLevel: d.acrLevel ?? 2
    };
    this.message = '';
  }

  resetForm() { this.initForm(); }

  save() {
    this.loadingSave = true;
    this.message = '';
    this.appApi.updateApplication(this.appId, this.editForm).subscribe({
      next: res => {
        this.loadingSave = false;
        this.message = 'Cập nhật thành công';
        this.messageType = 'success';
        this.loadDetail();
      },
      error: err => {
        this.loadingSave = false;
        this.message = err?.error?.errorDesc ?? err?.error?.message ?? 'Có lỗi xảy ra';
        this.messageType = 'danger';
      }
    });
  }

  toggleStatus() {
    if (!this.detail) return;
    const newStatus = this.detail.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const label = newStatus === 'ACTIVE' ? 'bật' : 'tắt';
    if (!confirm(`Xác nhận ${label} ứng dụng "${this.detail.name}"?`)) return;
    this.loadingToggle = true;
    this.appApi.toggleAppStatus(this.appId, newStatus).subscribe({
      next: () => {
        this.loadingToggle = false;
        this.detail = { ...this.detail, status: newStatus };
      },
      error: () => { this.loadingToggle = false; }
    });
  }

  back() { this.router.navigate(['/config/apps']); }

  private flattenDepts(nodes: any[], depth = 0): any[] {
    const result: any[] = [];
    for (const n of nodes) {
      result.push({ id: n.id, name: n.name, depth, label: '  '.repeat(depth) + n.name });
      if (n.children?.length) result.push(...this.flattenDepts(n.children, depth + 1));
    }
    return result;
  }
}
