import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PermissionService } from '../../../core/auth/permission.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-default-perm-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './default-perm-list.component.html',
  styleUrl: './default-perm-list.component.css'
})
export class DefaultPermListComponent implements OnInit {

  activeTab: 'app' | 'resource' = 'app';
  roles: any[] = [];
  applications: any[] = [];

  // ── App permissions ──────────────────────────────────────────────────────
  appPerms: any[] = [];
  appPermsTotal = 0;
  appPermsPage = 0;
  appPermsSize = 20;
  loadingApp = false;
  togglingAppPermId: any = null;
  filterApp = { roleId: '', positionCode: '', applicationId: '', status: '' };

  // ── Resource permissions ─────────────────────────────────────────────────
  resourcePerms: any[] = [];
  resourcePermsTotal = 0;
  resourcePermsPage = 0;
  resourcePermsSize = 20;
  loadingResource = false;
  filterResource = { roleId: '', positionCode: '', resourceId: '', status: '' };
  filterResAppId = '';
  filterResResources: any[] = [];

  // ── Create App Perm modal ────────────────────────────────────────────────
  showCreateAppModal = false;
  createAppForm = { roleId: '', positionCode: '', applicationId: '' };
  loadingCreateApp = false;
  createAppMessage = '';
  createAppMessageType = 'success';

  // ── Create Resource Perm modal ───────────────────────────────────────────
  showCreateResourceModal = false;
  createResForm = { roleId: '', positionCode: '', appId: '', resourceId: '' };
  createResResources: any[] = [];
  createResActions: string[] = [];
  createResSelectedActions: string[] = [];
  loadingCreateResource = false;
  createResourceMessage = '';
  createResourceMessageType = 'success';

  // ── Edit Resource Perm modal ─────────────────────────────────────────────
  showEditResourceModal = false;
  editResourceItem: any = null;
  editResStatus = 'ACTIVE';
  editResActionsText = '';
  loadingEditResource = false;
  editResourceMessage = '';
  editResourceMessageType = 'success';

  constructor(public perm: PermissionService, private api: AppApiService) {}

  ngOnInit() {
    this.api.getRoles().subscribe({ next: res => this.roles = res.data ?? [] });
    this.api.getApplications({ size: 200 }).subscribe({
      next: res => this.applications = res.data?.content ?? res.data ?? []
    });
    this.loadAppPerms();
  }

  switchTab(tab: 'app' | 'resource') {
    this.activeTab = tab;
    if (tab === 'app') this.loadAppPerms();
    else this.loadResourcePerms();
  }

  // ── App Perms ────────────────────────────────────────────────────────────

  loadAppPerms() {
    this.loadingApp = true;
    const params: any = { page: this.appPermsPage, size: this.appPermsSize };
    if (this.filterApp.roleId) params.roleId = this.filterApp.roleId;
    if (this.filterApp.positionCode) params.positionCode = this.filterApp.positionCode;
    if (this.filterApp.applicationId) params.applicationId = this.filterApp.applicationId;
    if (this.filterApp.status) params.status = this.filterApp.status;
    this.api.getDefaultAppPerms(params).subscribe({
      next: res => {
        this.loadingApp = false;
        this.appPerms = res.data?.content ?? [];
        this.appPermsTotal = res.data?.totalElements ?? 0;
      },
      error: () => { this.loadingApp = false; }
    });
  }

  searchAppPerms() { this.appPermsPage = 0; this.loadAppPerms(); }
  clearAppFilter() {
    this.filterApp = { roleId: '', positionCode: '', applicationId: '', status: '' };
    this.appPermsPage = 0; this.loadAppPerms();
  }
  totalAppPages(): number { return Math.ceil(this.appPermsTotal / this.appPermsSize) || 1; }
  goAppPage(p: number) { if (p < 0 || p >= this.totalAppPages()) return; this.appPermsPage = p; this.loadAppPerms(); }

  toggleAppPermStatus(item: any) {
    if (this.togglingAppPermId === item.id) return;
    const newStatus = item.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    this.togglingAppPermId = item.id;
    this.api.updateDefaultAppPermStatus({ items: [{ id: item.id, status: newStatus }] }).subscribe({
      next: () => { item.status = newStatus; this.togglingAppPermId = null; },
      error: () => { this.togglingAppPermId = null; }
    });
  }

  openCreateApp() {
    this.createAppForm = { roleId: '', positionCode: '', applicationId: '' };
    this.createAppMessage = '';
    this.showCreateAppModal = true;
  }
  closeCreateApp() { this.showCreateAppModal = false; }

  submitCreateApp() {
    if (!this.createAppForm.roleId || !this.createAppForm.positionCode || !this.createAppForm.applicationId) {
      this.createAppMessage = 'Vui lòng điền đầy đủ thông tin.';
      this.createAppMessageType = 'danger';
      return;
    }
    this.loadingCreateApp = true;
    this.createAppMessage = '';
    this.api.createDefaultAppPerms({
      items: [{
        roleId: this.createAppForm.roleId,
        positionCode: this.createAppForm.positionCode,
        applicationId: Number(this.createAppForm.applicationId)
      }]
    }).subscribe({
      next: () => { this.loadingCreateApp = false; this.showCreateAppModal = false; this.searchAppPerms(); },
      error: err => {
        this.loadingCreateApp = false;
        this.createAppMessage = err?.error?.errorDesc ?? 'Đã có lỗi xảy ra.';
        this.createAppMessageType = 'danger';
      }
    });
  }

  // ── Resource Perms ────────────────────────────────────────────────────────

  loadResourcePerms() {
    this.loadingResource = true;
    const params: any = { page: this.resourcePermsPage, size: this.resourcePermsSize };
    if (this.filterResource.roleId) params.roleId = this.filterResource.roleId;
    if (this.filterResource.positionCode) params.positionCode = this.filterResource.positionCode;
    if (this.filterResource.resourceId) params.resourceId = this.filterResource.resourceId;
    if (this.filterResource.status) params.status = this.filterResource.status;
    this.api.getDefaultResourcePerms(params).subscribe({
      next: res => {
        this.loadingResource = false;
        this.resourcePerms = res.data?.content ?? [];
        this.resourcePermsTotal = res.data?.totalElements ?? 0;
      },
      error: () => { this.loadingResource = false; }
    });
  }

  searchResourcePerms() { this.resourcePermsPage = 0; this.loadResourcePerms(); }
  clearResourceFilter() {
    this.filterResource = { roleId: '', positionCode: '', resourceId: '', status: '' };
    this.filterResAppId = '';
    this.filterResResources = [];
    this.resourcePermsPage = 0;
    this.loadResourcePerms();
  }
  totalResourcePages(): number { return Math.ceil(this.resourcePermsTotal / this.resourcePermsSize) || 1; }
  goResourcePage(p: number) { if (p < 0 || p >= this.totalResourcePages()) return; this.resourcePermsPage = p; this.loadResourcePerms(); }

  onFilterResAppChange() {
    this.filterResource.resourceId = '';
    this.filterResResources = [];
    if (!this.filterResAppId) return;
    this.api.getResources(Number(this.filterResAppId), { size: 200 }).subscribe({
      next: res => { this.filterResResources = res.data?.content ?? res.data ?? []; }
    });
  }

  openCreateResource() {
    this.createResForm = { roleId: '', positionCode: '', appId: '', resourceId: '' };
    this.createResResources = [];
    this.createResActions = [];
    this.createResSelectedActions = [];
    this.createResourceMessage = '';
    this.showCreateResourceModal = true;
  }
  closeCreateResource() { this.showCreateResourceModal = false; }

  onCreateResAppChange() {
    this.createResForm.resourceId = '';
    this.createResActions = [];
    this.createResSelectedActions = [];
    if (!this.createResForm.appId) { this.createResResources = []; return; }
    this.api.getResources(Number(this.createResForm.appId), { size: 200 }).subscribe({
      next: res => { this.createResResources = res.data?.content ?? res.data ?? []; }
    });
  }

  onCreateResResourceChange() {
    this.createResSelectedActions = [];
    const r = this.createResResources.find(x => String(x.id) === String(this.createResForm.resourceId));
    this.createResActions = r?.actions
      ? (Array.isArray(r.actions) ? r.actions : r.actions.split(',').map((a: string) => a.trim()))
          .filter(Boolean)
      : [];
  }

  toggleResAction(action: string) {
    const idx = this.createResSelectedActions.indexOf(action);
    if (idx >= 0) this.createResSelectedActions.splice(idx, 1);
    else this.createResSelectedActions.push(action);
  }

  submitCreateResource() {
    if (!this.createResForm.roleId || !this.createResForm.positionCode ||
        !this.createResForm.resourceId || !this.createResSelectedActions.length) {
      this.createResourceMessage = 'Vui lòng điền đầy đủ thông tin và chọn ít nhất 1 action.';
      this.createResourceMessageType = 'danger';
      return;
    }
    this.loadingCreateResource = true;
    this.createResourceMessage = '';
    this.api.createDefaultResourcePerms({
      items: [{
        roleId: Number(this.createResForm.roleId),
        positionCode: this.createResForm.positionCode,
        resourceId: Number(this.createResForm.resourceId),
        actions: this.createResSelectedActions
      }]
    }).subscribe({
      next: () => { this.loadingCreateResource = false; this.showCreateResourceModal = false; this.searchResourcePerms(); },
      error: err => {
        this.loadingCreateResource = false;
        this.createResourceMessage = err?.error?.errorDesc ?? 'Đã có lỗi xảy ra.';
        this.createResourceMessageType = 'danger';
      }
    });
  }

  openEditResource(item: any) {
    this.editResourceItem = item;
    this.editResStatus = item.status;
    this.editResActionsText = item.actions ?? '';
    this.editResourceMessage = '';
    this.showEditResourceModal = true;
  }
  closeEditResource() { this.showEditResourceModal = false; }

  submitEditResource() {
    const actions = this.editResActionsText.split(',').map((a: string) => a.trim()).filter(Boolean);
    if (!actions.length) {
      this.editResourceMessage = 'Phải có ít nhất 1 action.';
      this.editResourceMessageType = 'danger';
      return;
    }
    this.loadingEditResource = true;
    this.editResourceMessage = '';
    this.api.updateDefaultResourcePerms({
      items: [{ id: this.editResourceItem.id, status: this.editResStatus, actions }]
    }).subscribe({
      next: res => {
        this.loadingEditResource = false;
        const updated = res.data?.[0];
        if (updated) {
          this.editResourceItem.status = updated.status;
          this.editResourceItem.actions = updated.actions;
        } else {
          this.editResourceItem.status = this.editResStatus;
          this.editResourceItem.actions = actions.join(',');
        }
        this.showEditResourceModal = false;
      },
      error: err => {
        this.loadingEditResource = false;
        this.editResourceMessage = err?.error?.errorDesc ?? 'Đã có lỗi xảy ra.';
        this.editResourceMessageType = 'danger';
      }
    });
  }

  parseActions(actions: string | string[]): string[] {
    if (Array.isArray(actions)) return actions.filter(Boolean);
    return (actions ?? '').split(',').map(a => a.trim()).filter(Boolean);
  }
}
