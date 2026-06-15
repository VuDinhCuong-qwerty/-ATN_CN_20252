import { Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ChangeService } from '../../shared/services/change.service';
import { ReportSummary } from '../../shared/models/change.model';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [DecimalPipe],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  summary: ReportSummary | null = null;
  loading = true;
  error = '';

  readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  constructor(private svc: ChangeService) {}

  ngOnInit() {
    this.svc.getReportSummary().subscribe({
      next: s => { this.summary = s; this.loading = false; },
      error: () => { this.error = 'Không thể tải báo cáo.'; this.loading = false; }
    });
  }

  get statusEntries(): { status: string; count: number }[] {
    if (!this.summary?.byStatus) return [];
    return Object.entries(this.summary.byStatus).map(([status, count]) => ({ status, count }));
  }

  get successCount(): number {
    return this.summary?.byStatus?.['SUCCESS'] ?? 0;
  }

  get failCount(): number {
    return this.summary?.byStatus?.['FAIL'] ?? 0;
  }
}
