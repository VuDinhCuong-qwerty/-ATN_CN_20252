import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { IdentityService } from '../../../shared/services/identity.service';

@Component({
  selector: 'app-user-credential',
  imports: [CommonModule, FormsModule],
  templateUrl: './user-credential.component.html',
  styleUrl: './user-credential.component.css'
})
export class UserCredentialComponent {
  searchEmpCode = '';
  foundUser: any = null;
  loadingSearch = false;
  notFound = false;

  newPass = '';
  confirmNewPass = '';
  showConfirm = false;
  loading = false;
  showNewPass = false;
  showConfirmPass = false;

  get pwPolicies() {
    const pw = this.newPass;
    return [
      { label: 'Ít nhất 8 ký tự', met: pw.length >= 8 },
      { label: 'Ít nhất 1 chữ hoa (A–Z)', met: /[A-Z]/.test(pw) },
      { label: 'Ít nhất 1 chữ thường (a–z)', met: /[a-z]/.test(pw) },
      { label: 'Ít nhất 1 chữ số (0–9)', met: /\d/.test(pw) },
      { label: 'Ít nhất 1 ký tự đặc biệt', met: /[^A-Za-z0-9]/.test(pw) },
    ];
  }

  message = '';
  messageType = '';

  constructor(private identityService: IdentityService) {}

  searchUser() {
    const code = this.searchEmpCode.trim();
    if (!code) return;
    this.foundUser = null;
    this.notFound = false;
    this.message = '';
    this.newPass = '';
    this.confirmNewPass = '';
    this.showConfirm = false;
    this.loadingSearch = true;

    this.identityService.getUsers({ employeeCode: code, page: 0, size: 1 }).subscribe({
      next: res => {
        this.loadingSearch = false;
        const list = res.data?.content ?? (Array.isArray(res.data) ? res.data : []);
        const user = list.find((u: any) => u.employeeCode === code) ?? list[0];
        if (user) {
          this.foundUser = user;
        } else {
          this.notFound = true;
        }
      },
      error: () => { this.loadingSearch = false; this.notFound = true; }
    });
  }

  requestConfirm() {
    if (!this.newPass || !this.confirmNewPass) {
      this.showMessage('Vui lòng nhập mật khẩu mới và xác nhận.', 'warning'); return;
    }
    if (this.newPass !== this.confirmNewPass) {
      this.showMessage('Mật khẩu xác nhận không khớp.', 'danger'); return;
    }
    this.message = '';
    this.showConfirm = true;
  }

  confirmAction() {
    const body = {
      userId: this.foundUser.id ?? this.foundUser.userId,
      employeeCode: this.foundUser.employeeCode,
      newPass: this.newPass,
      confirmNewPass: this.confirmNewPass
    };
    this.loading = true;
    this.identityService.resetPassword(body).subscribe({
      next: () => {
        this.loading = false;
        this.showConfirm = false;
        this.newPass = '';
        this.confirmNewPass = '';
        this.showMessage('Đặt lại mật khẩu thành công!', 'success');
      },
      error: err => {
        this.loading = false;
        this.showConfirm = false;
        this.showMessage(err.error?.message ?? 'Không thể đặt lại mật khẩu.', 'danger');
      }
    });
  }

  reset() {
    this.searchEmpCode = '';
    this.foundUser = null;
    this.notFound = false;
    this.newPass = '';
    this.confirmNewPass = '';
    this.showConfirm = false;
    this.message = '';
    this.showNewPass = false;
    this.showConfirmPass = false;
  }

  private showMessage(msg: string, type: string) {
    this.message = msg;
    this.messageType = type;
    if (type === 'success') setTimeout(() => this.message = '', 6000);
  }
}
