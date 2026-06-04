import { Component, OnInit, HostListener, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Subject } from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { forkJoin } from 'rxjs';
import { PermissionService } from '../../../core/auth/permission.service';
import { IdentityService } from '../../../shared/services/identity.service';
import { AppApiService } from '../../../shared/services/app-api.service';

@Component({
  selector: 'app-user-roles',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-roles.component.html'
})
export class UserRolesComponent implements OnInit {
  searchInput = '';
  suggestions: any[] = [];
  showSuggestions = false;
  private searchSubject = new Subject<string>();

  targetUser: any = null;
  userRoles: any[] = [];
  allRoles: any[] = [];
  selectedRoleCode = '';
  loadingLookup = false;
  loadingAction = false;
  message = '';
  messageType = '';

  constructor(
    public perm: PermissionService,
    private identityService: IdentityService,
    private appApiService: AppApiService,
    private eRef: ElementRef
  ) {}

  ngOnInit() {
    this.appApiService.getRoles().subscribe({ next: res => this.allRoles = res.data ?? [] });
    this.searchSubject.pipe(debounceTime(300), distinctUntilChanged()).subscribe(term => {
      if (term.length < 2) { this.suggestions = []; this.showSuggestions = false; return; }
      this.fetchSuggestions(term);
    });
  }

  onSearchInput() {
    this.showSuggestions = false;
    this.searchSubject.next(this.searchInput.trim());
  }

  private fetchSuggestions(term: string) {
    forkJoin([
      this.identityService.getUsers({ employeeCode: term, size: 5 }),
      this.identityService.getUsers({ username: term, size: 5 })
    ]).subscribe({
      next: ([empRes, usernameRes]) => {
        const empList: any[] = empRes.data?.content ?? empRes.data ?? [];
        const usernameList: any[] = usernameRes.data?.content ?? usernameRes.data ?? [];
        const seen = new Set<number>();
        const merged: any[] = [];
        for (const u of [...empList, ...usernameList]) {
          if (!seen.has(u.userId)) { seen.add(u.userId); merged.push(u); }
        }
        this.suggestions = merged.slice(0, 8);
        this.showSuggestions = this.suggestions.length > 0;
      }
    });
  }

  selectSuggestion(user: any) {
    this.searchInput = user.employeeCode;
    this.showSuggestions = false;
    this.suggestions = [];
    this.targetUser = user;
    this.userRoles = [];
    this.selectedRoleCode = '';
    this.loadUserRoles(user.employeeCode);
  }

  onEnter() {
    if (this.suggestions.length > 0) this.selectSuggestion(this.suggestions[0]);
  }

  clearSearch() {
    this.searchInput = '';
    this.showSuggestions = false;
    this.suggestions = [];
    this.targetUser = null;
    this.userRoles = [];
    this.selectedRoleCode = '';
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick(event: Event) {
    if (!this.eRef.nativeElement.contains(event.target)) {
      this.showSuggestions = false;
    }
  }

  loadUserRoles(empCode: string) {
    this.loadingLookup = true;
    this.identityService.getUserRoles(empCode).subscribe({
      next: res => { this.loadingLookup = false; this.userRoles = res.data?.content ?? res.data ?? []; },
      error: () => { this.loadingLookup = false; }
    });
  }

  get availableRoles(): any[] {
    const currentCodes = this.userRoles.map((r: any) => r.roleCode ?? r.code);
    return this.allRoles.filter(r => !currentCodes.includes(r.code));
  }

  assignRole() {
    if (!this.targetUser || !this.selectedRoleCode) return;
    this.loadingAction = true;
    this.identityService.assignRole(this.targetUser.employeeCode, { roleCode: this.selectedRoleCode }).subscribe({
      next: () => {
        this.loadingAction = false;
        this.showMessage('Gán role thành công', 'success');
        this.selectedRoleCode = '';
        this.loadUserRoles(this.targetUser.employeeCode);
      },
      error: err => {
        this.loadingAction = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể gán role'}`, 'danger');
      }
    });
  }

  revokeRole(roleCode: string) {
    if (!this.targetUser) return;
    this.loadingAction = true;
    this.identityService.revokeRole(this.targetUser.employeeCode, roleCode).subscribe({
      next: () => {
        this.loadingAction = false;
        this.showMessage('Thu hồi role thành công', 'success');
        this.loadUserRoles(this.targetUser.employeeCode);
      },
      error: err => {
        this.loadingAction = false;
        this.showMessage(`Lỗi: ${err.error?.message ?? 'Không thể thu hồi role'}`, 'danger');
      }
    });
  }

  private showMessage(msg: string, type: string) {
    this.message = msg; this.messageType = type;
    setTimeout(() => this.message = '', 6000);
  }
}
