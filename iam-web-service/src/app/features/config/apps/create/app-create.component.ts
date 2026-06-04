import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { AppApiService } from '../../../../shared/services/app-api.service';
import { PermissionService } from '../../../../core/auth/permission.service';

@Component({
  selector: 'app-app-create',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './app-create.component.html',
  styleUrl: './app-create.component.css'
})
export class AppCreateComponent implements OnInit {
  flatDepartments: any[] = [];

  form: any = {
    name: '',
    serviceCode: '',
    appType: 'INTERNAL',
    acrLevel: 2,
    description: '',
    logoUri: '',
    defaultUrl: '',
    departmentId: null,
    groupId: null
  };

  loadingCreate = false;
  message = '';
  messageType = '';

  constructor(
    private router: Router,
    public perm: PermissionService,
    private appApi: AppApiService
  ) {}

  ngOnInit() {
    this.appApi.getDepartments().subscribe({
      next: res => {
        this.flatDepartments = this.flattenDepts(res.data ?? []);
      }
    });
  }

  submit() {
    if (!this.form.name?.trim() || !this.form.serviceCode?.trim()) {
      this.message = 'Tên và Service Code là bắt buộc';
      this.messageType = 'danger';
      return;
    }
    if (!this.form.logoUri?.trim()) {
      this.message = 'Logo URL là bắt buộc';
      this.messageType = 'danger';
      return;
    }
    if (!this.form.defaultUrl?.trim()) {
      this.message = 'Default URL là bắt buộc';
      this.messageType = 'danger';
      return;
    }
    if (this.form.departmentId == null) {
      this.message = 'Vui lòng chọn phòng ban';
      this.messageType = 'danger';
      return;
    }

    this.loadingCreate = true;
    this.message = '';
    this.appApi.createApplication(this.form).subscribe({
      next: () => {
        this.loadingCreate = false;
        this.message = 'Tạo ứng dụng thành công!';
        this.messageType = 'success';
        setTimeout(() => this.router.navigate(['/config/apps']), 1200);
      },
      error: err => {
        this.loadingCreate = false;
        this.message = err?.error?.errorDesc ?? err?.error?.message ?? 'Có lỗi xảy ra';
        this.messageType = 'danger';
      }
    });
  }

  back() { this.router.navigate(['/config/apps']); }

  private flattenDepts(nodes: any[], depth = 0): any[] {
    const result: any[] = [];
    for (const n of nodes) {
      result.push({ id: n.id, name: n.name, depth, label: '  '.repeat(depth) + n.name });
      if (n.children?.length) result.push(...this.flattenDepts(n.children, depth + 1));
    }
    return result;
  }
}
