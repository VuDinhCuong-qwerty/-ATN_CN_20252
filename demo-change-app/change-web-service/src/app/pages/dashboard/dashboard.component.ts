import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ChangeService } from '../../shared/services/change.service';
import { DashboardStats } from '../../shared/models/change.model';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.css'
})
export class DashboardComponent implements OnInit {
  stats: DashboardStats | null = null;
  loading = true;
  error = '';

  readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  constructor(private svc: ChangeService) {}

  ngOnInit() {
    this.svc.getDashboardStats().subscribe({
      next: s => { this.stats = s; this.loading = false; },
      error: () => { this.error = 'Không thể tải dữ liệu dashboard.'; this.loading = false; }
    });
  }

  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }

  shortId(id?: string): string {
    return id ? id.substring(0, 8).toUpperCase() : '—';
  }
}
