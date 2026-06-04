import { Component, OnInit, HostListener } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { TokenStoreService } from './core/auth/token-store.service';
import { AuthService } from './core/auth/auth.service';
import { PermissionService } from './core/auth/permission.service';
import { IdentityService } from './shared/services/identity.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, FormsModule],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  sidebarOpen = true;
  userExpanded = true;
  configExpanded = false;
  userMenuOpen = false;

  // Change password modal
  showChangePwModal = false;
  changePwForm = { oldpass: '', newPass: '', confirmNewPass: '' };
  loadingChangePw = false;
  changePwMessage = '';
  changePwMessageType = '';
  showOldPass = false;
  showNewPass = false;
  showConfirmPass = false;

  get pwPolicies() {
    const pw = this.changePwForm.newPass;
    return [
      { label: 'Ít nhất 8 ký tự',              met: pw.length >= 8 },
      { label: 'Ít nhất 1 chữ hoa (A–Z)',       met: /[A-Z]/.test(pw) },
      { label: 'Ít nhất 1 chữ thường (a–z)',    met: /[a-z]/.test(pw) },
      { label: 'Ít nhất 1 chữ số (0–9)',        met: /\d/.test(pw) },
      { label: 'Ít nhất 1 ký tự đặc biệt',      met: /[^A-Za-z0-9]/.test(pw) },
    ];
  }

  get userInfo() { return this.perm.userInfo; }
  get isLoggedIn() { return this.perm.DEV_MODE || this.tokenStore.isLoggedIn(); }
  get userInitial(): string {
    return (this.userInfo?.username ?? '?').charAt(0).toUpperCase();
  }

  constructor(
    public tokenStore: TokenStoreService,
    public authService: AuthService,
    public perm: PermissionService,
    private identityService: IdentityService,
    private router: Router
  ) {}

  ngOnInit() {
    window.addEventListener('storage', (e) => {
      if (e.key === '_iam_logout') {
        this.tokenStore.clear();
        window.location.href = '/login';
      }
    });

    if (window.location.pathname.startsWith('/callback')) return;
    if (!this.isLoggedIn) return;

    const path = window.location.pathname;
    if (path.startsWith('/users')) this.userExpanded = true;
    if (path.startsWith('/config')) this.configExpanded = true;
  }

  @HostListener('document:click')
  onDocumentClick() { this.userMenuOpen = false; }

  toggleSidebar() { this.sidebarOpen = !this.sidebarOpen; }
  toggleUser() { this.userExpanded = !this.userExpanded; }
  toggleConfig() { this.configExpanded = !this.configExpanded; }
  toggleUserMenu(event: Event) { event.stopPropagation(); this.userMenuOpen = !this.userMenuOpen; }
  logout() { this.authService.logout(); }

  // ===== Change Password =====
  openChangePw() {
    this.changePwForm = { oldpass: '', newPass: '', confirmNewPass: '' };
    this.changePwMessage = '';
    this.changePwMessageType = '';
    this.showOldPass = false;
    this.showNewPass = false;
    this.showConfirmPass = false;
    this.showChangePwModal = true;
    this.userMenuOpen = false;
  }

  closeChangePw() { this.showChangePwModal = false; }

  submitChangePassword() {
    const f = this.changePwForm;
    if (!f.oldpass || !f.newPass || !f.confirmNewPass) {
      this.changePwMessage = 'Vui lòng điền đầy đủ các trường.';
      this.changePwMessageType = 'warning'; return;
    }
    if (f.newPass !== f.confirmNewPass) {
      this.changePwMessage = 'Mật khẩu mới và xác nhận không khớp.';
      this.changePwMessageType = 'danger'; return;
    }
    const body = {
      userId: Number(this.userInfo?.sub ?? 0),
      employeeCode: this.userInfo?.employeeCode ?? '',
      oldpass: f.oldpass,
      newPass: f.newPass,
      confirmNewPass: f.confirmNewPass
    };
    this.loadingChangePw = true;
    this.identityService.changePassword(body).subscribe({
      next: () => {
        this.loadingChangePw = false;
        this.changePwMessage = 'Đổi mật khẩu thành công! Đang đăng xuất...';
        this.changePwMessageType = 'success';
        setTimeout(() => this.logout(), 1500);
      },
      error: err => {
        this.loadingChangePw = false;
        this.changePwMessage = err.error?.errorDesc ?? err.error?.message ?? 'Không thể đổi mật khẩu.';
        this.changePwMessageType = 'danger';
      }
    });
  }

  // ===== My Profile =====
  openMyProfile() {
    this.userMenuOpen = false;
    const userId = this.userInfo?.sub ?? '';
    const empCode = this.userInfo?.employeeCode ?? '';
    this.router.navigate(['/users/detail'], {
      queryParams: { userId, employeeCode: empCode, selfProfile: true }
    });
  }
}
