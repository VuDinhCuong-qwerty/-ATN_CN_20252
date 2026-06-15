import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ChangeService } from '../../shared/services/change.service';
import { PermissionService } from '../../core/auth/permission.service';
import { ChangeListItem } from '../../shared/models/change.model';

@Component({
  selector: 'app-approval',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './approval.component.html',
  styleUrl: './approval.component.css'
})
export class ApprovalComponent implements OnInit {
  activeTab: 'queue' | 'history' = 'queue';

  queueItems: ChangeListItem[] = [];
  historyItems: ChangeListItem[] = [];
  loadingQueue = false;
  loadingHistory = false;
  error = '';

  filterSubmitter = '';
  filterDateFrom = '';
  filterDateTo = '';

  readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  constructor(private svc: ChangeService, public perm: PermissionService) {
    if (!perm.has('change-approval', 'approve')) this.activeTab = 'history';
  }

  ngOnInit() {
    if (this.perm.has('change-approval', 'approve')) this.loadQueue();
    this.loadHistory();
  }

  loadQueue() {
    this.loadingQueue = true;
    this.svc.getChanges({ status: 'PENDING' }).subscribe({
      next: data => { this.queueItems = data?.content ?? data ?? []; this.loadingQueue = false; },
      error: () => { this.error = 'Không thể tải hàng chờ duyệt.'; this.loadingQueue = false; }
    });
  }

  loadHistory() {
    const params: Record<string, string> = {};
    if (this.filterSubmitter) params['createdBy'] = this.filterSubmitter;
    if (this.filterDateFrom) params['from'] = this.filterDateFrom;
    if (this.filterDateTo) params['to'] = this.filterDateTo;
    this.loadingHistory = true;
    this.svc.getChanges({ ...params }).subscribe({
      next: data => {
        const all: ChangeListItem[] = data?.content ?? data ?? [];
        this.historyItems = all.filter((c: ChangeListItem) => ['APPROVED', 'EXECUTING', 'SUCCESS', 'FAIL', 'DRAFT'].includes(c.status));
        this.loadingHistory = false;
      },
      error: () => { this.loadingHistory = false; }
    });
  }

  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
