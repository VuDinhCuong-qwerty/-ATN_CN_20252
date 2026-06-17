import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DecimalPipe } from '@angular/common';
import { ChangeService } from '../../shared/services/change.service';
import { DashboardStats } from '../../shared/models/change.model';
import { DonutChartComponent, DonutSegment } from '../../shared/components/donut-chart/donut-chart.component';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [RouterLink, DecimalPipe, DonutChartComponent],
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

  private readonly STATUS_COLORS: Record<string, string> = {
    DRAFT: '#94A3B8', PENDING: '#F59E0B', APPROVED: '#3B82F6',
    EXECUTING: '#8B5CF6', SUCCESS: '#10B981', FAIL: '#EF4444'
  };

  constructor(private svc: ChangeService) {}

  ngOnInit() {
    this.svc.getDashboardStats().subscribe({
      next: s => { this.stats = s; this.loading = false; },
      error: () => { this.error = 'Không thể tải dữ liệu dashboard.'; this.loading = false; }
    });
  }

  /** Donut: success rate ring (2 segments) */
  get rateSegments(): DonutSegment[] {
    const rate = this.stats?.successRate ?? 0;
    return [
      { label: 'Thành công', value: rate,       color: '#10B981' },
      { label: 'Thất bại',   value: 100 - rate, color: '#EF4444' }
    ].filter(s => s.value > 0);
  }

  get rateCenterOverride(): string {
    return (this.stats?.successRate ?? 0).toFixed(0) + '%';
  }

  /** Donut: status breakdown từ recent changes */
  get recentStatusSegments(): DonutSegment[] {
    if (!this.stats?.recentChanges?.length) return [];
    const counts: Record<string, number> = {};
    for (const c of this.stats.recentChanges) {
      counts[c.status] = (counts[c.status] || 0) + 1;
    }
    const order = ['SUCCESS', 'EXECUTING', 'APPROVED', 'PENDING', 'DRAFT', 'FAIL'];
    return order
      .filter(s => counts[s] > 0)
      .map(s => ({
        label: this.STATUS_LABELS[s] || s,
        value: counts[s],
        color: this.STATUS_COLORS[s] || '#94A3B8'
      }));
  }

  get recentTotal(): number {
    return this.stats?.recentChanges?.length ?? 0;
  }

  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  shortId(id?: string): string {
    return id ? id.substring(0, 8).toUpperCase() : '—';
  }
}
