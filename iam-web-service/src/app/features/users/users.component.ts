import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { Subject, debounceTime, distinctUntilChanged, takeUntil } from 'rxjs';
import { PermissionService } from '../../core/auth/permission.service';
import { IdentityService } from '../../shared/services/identity.service';
import { AppApiService } from '../../shared/services/app-api.service';

@Component({
  selector: 'app-users',
  imports: [CommonModule, FormsModule],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css'
})
export class UsersComponent implements OnInit, OnDestroy {
  private destroy$ = new Subject<void>();
  private searchSubject$ = new Subject<void>();

  users: any[] = [];

  searchFullName = '';
  searchEmpCode = '';
  searchUsername = '';
  searchDeptId: any = '';
  searchStatus = '';
  page = 0;
  size = 20;
  totalPages = 0;
  totalElements = 0;
  loadingList = false;

  departments: any[] = [];

  constructor(
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService,
    private router: Router
  ) {}

  ngOnInit() {
    this.appApiService.getDepartments().subscribe({ next: res => this.departments = res.data ?? [] });
    this.loadUsers();
    this.searchSubject$.pipe(
      debounceTime(400), distinctUntilChanged(), takeUntil(this.destroy$)
    ).subscribe(() => { this.page = 0; this.loadUsers(); });
  }

  ngOnDestroy() { this.destroy$.next(); this.destroy$.complete(); }

  onSearchChange() { this.searchSubject$.next(); }

  loadUsers() {
    this.loadingList = true;
    const params: any = { page: this.page, size: this.size };
    if (this.searchFullName) params.fullName = this.searchFullName;
    if (this.searchEmpCode) params.employeeCode = this.searchEmpCode;
    if (this.searchUsername) params.username = this.searchUsername;
    if (this.searchDeptId) params.departmentId = this.searchDeptId;
    if (this.searchStatus) params.status = this.searchStatus;

    this.identityService.getUsers(params).subscribe({
      next: res => {
        this.loadingList = false;
        const data = res.data;
        if (data?.content) {
          this.users = data.content;
          this.totalPages = data.totalPages ?? 0;
          this.totalElements = data.totalElements ?? 0;
        } else {
          this.users = data ?? [];
          this.totalPages = 1;
        }
      },
      error: () => { this.loadingList = false; }
    });
  }

  openDetail(user: any) {
    this.router.navigate(['/users/detail'], {
      queryParams: { userId: user.id ?? user.userId, employeeCode: user.employeeCode }
    });
  }

  goToCreate() {
    this.router.navigate(['/users/create']);
  }

  prevPage() { if (this.page > 0) { this.page--; this.loadUsers(); } }
  nextPage() { if (this.page < this.totalPages - 1) { this.page++; this.loadUsers(); } }
  clearSearch() {
    this.searchFullName = ''; this.searchEmpCode = '';
    this.searchUsername = ''; this.searchDeptId = ''; this.searchStatus = '';
    this.page = 0; this.loadUsers();
  }

}
