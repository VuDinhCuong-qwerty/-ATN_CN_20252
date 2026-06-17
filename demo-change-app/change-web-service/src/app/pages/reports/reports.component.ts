import { Component, OnInit } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { ChangeService } from '../../shared/services/change.service';
import { ReportSummary } from '../../shared/models/change.model';
import { DonutChartComponent, DonutSegment } from '../../shared/components/donut-chart/donut-chart.component';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [DecimalPipe, DonutChartComponent],
  templateUrl: './reports.component.html',
  styleUrl: './reports.component.css'
})
export class ReportsComponent implements OnInit {
  summary: ReportSummary | null = null;
  loading = true;
  error = '';

  private readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  private readonly STATUS_COLORS: Record<string, string> = {
    DRAFT: '#94A3B8', PENDING: '#F59E0B', APPROVED: '#3B82F6',
    EXECUTING: '#8B5CF6', SUCCESS: '#10B981', FAIL: '#EF4444'
  };

  constructor(private svc: ChangeService) {}

  ngOnInit() {
    this.svc.getReportSummary().subscribe({
      next: s => { this.summary = s; this.loading = false; },
      error: () => { this.error = 'Không thể tải báo cáo.'; this.loading = false; }
    });
  }

  get statusSegments(): DonutSegment[] {
    if (!this.summary?.byStatus) return [];
    const order = ['SUCCESS', 'EXECUTING', 'APPROVED', 'PENDING', 'DRAFT', 'FAIL'];
    return order
      .filter(s => (this.summary!.byStatus[s] ?? 0) > 0)
      .map(s => ({
        label: this.STATUS_LABELS[s] || s,
        value: this.summary!.byStatus[s],
        color: this.STATUS_COLORS[s] || '#94A3B8'
      }));
  }

  get successCount(): number { return this.summary?.byStatus?.['SUCCESS'] ?? 0; }
  get failCount(): number    { return this.summary?.byStatus?.['FAIL'] ?? 0; }
}
