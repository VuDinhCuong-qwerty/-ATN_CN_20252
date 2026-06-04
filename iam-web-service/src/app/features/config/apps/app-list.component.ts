import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { PermissionService } from '../../../core/auth/permission.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-app-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './app-list.component.html',
  styleUrl: './app-list.component.css'
})
export class AppListComponent implements OnInit {
  apps: any[] = [];
  loading = false;

  // Search
  searchName = '';
  searchServiceCode = '';
  searchType = '';
  searchStatus = '';

  // Pagination
  currentPage = 0;
  pageSize = 20;
  totalPages = 0;
  totalElements = 0;

  // Status toggle in table
  togglingId: any = null;

  constructor(public perm: PermissionService, private appApi: AppApiService, private router: Router) {}

  ngOnInit() {
    this.loadApps();
  }

  loadApps() {
    this.loading = true;
    const params: any = { page: this.currentPage, size: this.pageSize };
    if (this.searchName) params.name = this.searchName;
    if (this.searchServiceCode) params.serviceCode = this.searchServiceCode;
    if (this.searchType) params.type = this.searchType;
    if (this.searchStatus) params.status = this.searchStatus;
    this.appApi.getApplications(params).subscribe({
      next: res => {
        this.loading = false;
        const d = res.data;
        this.apps = d?.content ?? d ?? [];
        this.totalPages = d?.totalPages ?? d?.totalPage ?? 1;
        this.totalElements = d?.totalElements ?? d?.totalElement ?? this.apps.length;
      },
      error: () => { this.loading = false; }
    });
  }

  search() { this.currentPage = 0; this.loadApps(); }

  clearSearch() {
    this.searchName = '';
    this.searchServiceCode = '';
    this.searchType = '';
    this.searchStatus = '';
    this.currentPage = 0;
    this.loadApps();
  }

  goPage(p: number) {
    if (p < 0 || p >= this.totalPages) return;
    this.currentPage = p;
    this.loadApps();
  }

  getVisiblePages(): number[] {
    const start = Math.max(0, this.currentPage - 2);
    const end = Math.min(this.totalPages - 1, this.currentPage + 2);
    const pages = [];
    for (let i = start; i <= end; i++) pages.push(i);
    return pages;
  }

  goToDetail(app: any) {
    const id = app.id ?? app.appId;
    this.router.navigate(['/config/apps/detail'], { queryParams: { appId: id } });
  }

  goToCreate() {
    this.router.navigate(['/config/apps/create']);
  }

  toggleStatusInList(app: any, event: Event) {
    event.stopPropagation();
    const id = app.id ?? app.appId;
    const newStatus = app.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
    const label = newStatus === 'ACTIVE' ? 'bật' : 'tắt';
    if (!confirm(`Xác nhận ${label} ứng dụng "${app.name}"?`)) return;
    this.togglingId = id;
    this.appApi.toggleAppStatus(id, newStatus).subscribe({
      next: () => {
        this.togglingId = null;
        app.status = newStatus;
      },
      error: () => { this.togglingId = null; }
    });
  }
}
