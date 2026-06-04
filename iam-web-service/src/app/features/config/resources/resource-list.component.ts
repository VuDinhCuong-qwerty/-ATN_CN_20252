import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PermissionService } from '../../../core/auth/permission.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-resource-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './resource-list.component.html',
  styleUrl: './resource-list.component.css'
})
export class ResourceListComponent implements OnInit {
  applications: any[] = [];
  resources: any[] = [];
  loading = false;

  // Filter
  filterAppId: any = '';
  filterStatus = '';

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  // Detail / inline-edit modal
  showDetailModal = false;
  detailResource: any = null;
  loadingDetail = false;
  editForm: any = {};
  loadingSave = false;
  saveMessage = '';
  saveMessageType = '';

  // Create modal
  showCreateModal = false;
  createForm: any = {};
  loadingCreate = false;
  createMessage = '';
  createMessageType = '';

  readonly RESOURCE_TYPES = ['ENDPOINT', 'LDAP_GROUP', 'MENU', 'FUNCTION'];

  constructor(public perm: PermissionService, private appApi: AppApiService) {}

  ngOnInit() {
    this.appApi.getApplications({ size: 200 }).subscribe({
      next: res => {
        this.applications = res.data?.content ?? res.data ?? [];
      }
    });
  }

  loadResources() {
    if (!this.filterAppId) return;
    this.loading = true;
    const params: any = { page: this.currentPage, size: this.pageSize };
    if (this.filterStatus) params.status = this.filterStatus;
    this.appApi.getResources(Number(this.filterAppId), params).subscribe({
      next: res => {
        this.loading = false;
        const d = res.data;
        this.resources = d?.content ?? d ?? [];
        this.totalPages = d?.totalPages ?? d?.totalPage ?? 1;
        this.totalElements = d?.totalElements ?? d?.totalElement ?? this.resources.length;
      },
      error: () => { this.loading = false; }
    });
  }

  onAppChange() { this.currentPage = 0; this.resources = []; this.loadResources(); }
  search() { this.currentPage = 0; this.loadResources(); }

  clearFilter() {
    this.filterStatus = '';
    this.currentPage = 0;
    this.loadResources();
  }

  goPage(p: number) {
    if (p < 0 || p >= this.totalPages) return;
    this.currentPage = p;
    this.loadResources();
  }

  getVisiblePages(): number[] {
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    const pages = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  actionsDisplay(actions: any): string {
    if (Array.isArray(actions)) return actions.join(', ');
    return actions ?? '—';
  }

  // ── Detail modal ──────────────────────────────────────────────
  openDetail(r: any) {
    this.showDetailModal = true;
    this.detailResource = null;
    this.editForm = {};
    this.saveMessage = '';
    this.loadingDetail = true;
    const resourceId = r.id ?? r.resourceId;
    this.appApi.getResourceDetail(Number(this.filterAppId), resourceId).subscribe({
      next: res => {
        this.loadingDetail = false;
        this.detailResource = res.data ?? r;
        this.initEditForm();
      },
      error: () => {
        this.loadingDetail = false;
        this.detailResource = r;
        this.initEditForm();
      }
    });
  }

  initEditForm() {
    const d = this.detailResource;
    if (!d) return;
    const actionsArr = Array.isArray(d.actions) ? d.actions
      : (d.actions ? String(d.actions).split(',').map((a: string) => a.trim()) : []);
    this.editForm = {
      resourceCode: d.resourceCode ?? '',
      resourceName: d.resourceName ?? '',
      resourceType: d.resourceType ?? 'ENDPOINT',
      actionsRaw: actionsArr.join(', '),
      status: d.status ?? 'ACTIVE',
      ldapGroupName: d.ldapGroupName ?? '',
      description: d.description ?? '',
    };
    this.saveMessage = '';
  }

  resetEditForm() { this.initEditForm(); }

  closeDetail() {
    this.showDetailModal = false;
    this.saveMessage = '';
  }

  saveResource() {
    const actions = (this.editForm.actionsRaw || '')
      .split(',')
      .map((a: string) => a.trim())
      .filter((a: string) => a.length > 0);

    if (!this.editForm.resourceCode?.trim() || !this.editForm.resourceName?.trim() || actions.length === 0) {
      this.saveMessage = 'Resource Code, Tên và Actions là bắt buộc';
      this.saveMessageType = 'danger';
      return;
    }

    this.loadingSave = true;
    this.saveMessage = '';
    const resourceId = this.detailResource?.id ?? this.detailResource?.resourceId;
    const body = {
      resourceCode: this.editForm.resourceCode,
      resourceName: this.editForm.resourceName,
      resourceType: this.editForm.resourceType,
      actions,
      status: this.editForm.status,
      ldapGroupName: this.editForm.ldapGroupName || null,
      description: this.editForm.description || null,
    };

    this.appApi.updateResource(Number(this.filterAppId), resourceId, body).subscribe({
      next: res => {
        this.loadingSave = false;
        this.saveMessage = 'Cập nhật thành công';
        this.saveMessageType = 'success';
        this.detailResource = res.data ?? this.detailResource;
        this.initEditForm();
        this.loadResources();
      },
      error: err => {
        this.loadingSave = false;
        this.saveMessage = err?.error?.errorDesc ?? 'Có lỗi xảy ra';
        this.saveMessageType = 'danger';
      }
    });
  }

  // ── Create modal ──────────────────────────────────────────────
  openCreate() {
    this.createForm = {
      appId: this.filterAppId || (this.applications[0]?.id ?? ''),
      resourceCode: '',
      resourceName: '',
      resourceType: 'ENDPOINT',
      actions: 'read',
      ldapGroupName: '',
      description: ''
    };
    this.createMessage = '';
    this.showCreateModal = true;
  }

  closeCreate() { this.showCreateModal = false; this.createMessage = ''; }

  submitCreate() {
    if (!this.createForm.resourceCode?.trim() || !this.createForm.resourceName?.trim()) {
      this.createMessage = 'Resource Code và Tên là bắt buộc';
      this.createMessageType = 'danger';
      return;
    }
    if (!this.createForm.appId) {
      this.createMessage = 'Vui lòng chọn ứng dụng';
      this.createMessageType = 'danger';
      return;
    }
    const actionsStr = (this.createForm.actions || '')
      .split(',')
      .map((a: string) => a.trim())
      .filter((a: string) => a.length > 0)
      .join(',');

    if (!actionsStr) {
      this.createMessage = 'Actions không được để trống';
      this.createMessageType = 'danger';
      return;
    }

    this.loadingCreate = true;
    this.createMessage = '';
    const body = {
      items: [{
        resourceCode: this.createForm.resourceCode,
        resourceName: this.createForm.resourceName,
        resourceType: this.createForm.resourceType,
        actions: actionsStr,
        ldapGroupName: this.createForm.ldapGroupName || null,
        description: this.createForm.description || null
      }]
    };
    this.appApi.createResources(Number(this.createForm.appId), body).subscribe({
      next: () => {
        this.loadingCreate = false;
        this.showCreateModal = false;
        this.filterAppId = this.createForm.appId;
        this.loadResources();
      },
      error: err => {
        this.loadingCreate = false;
        this.createMessage = err?.error?.errorDesc ?? 'Có lỗi xảy ra';
        this.createMessageType = 'danger';
      }
    });
  }
}
