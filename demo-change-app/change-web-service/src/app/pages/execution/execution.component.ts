import { Component, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { ChangeService } from '../../shared/services/change.service';
import { ChangeListItem } from '../../shared/models/change.model';

@Component({
  selector: 'app-execution',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './execution.component.html',
  styleUrl: './execution.component.css'
})
export class ExecutionComponent implements OnInit {
  activeTab: 'ready' | 'executing' | 'done' = 'ready';

  readyItems: ChangeListItem[] = [];
  executingItems: ChangeListItem[] = [];
  doneItems: ChangeListItem[] = [];

  loadingReady = false;
  loadingExecuting = false;
  loadingDone = false;
  error = '';

  readonly STATUS_LABELS: Record<string, string> = {
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  constructor(private svc: ChangeService) {}

  ngOnInit() {
    this.loadAll();
  }

  loadAll() {
    this.loadReady();
    this.loadExecuting();
    this.loadDone();
  }

  loadReady() {
    this.loadingReady = true;
    this.svc.getChanges({ status: 'APPROVED' }).subscribe({
      next: data => { this.readyItems = data?.content ?? data ?? []; this.loadingReady = false; },
      error: () => { this.error = 'Không thể tải dữ liệu.'; this.loadingReady = false; }
    });
  }

  loadExecuting() {
    this.loadingExecuting = true;
    this.svc.getChanges({ status: 'EXECUTING' }).subscribe({
      next: data => { this.executingItems = data?.content ?? data ?? []; this.loadingExecuting = false; },
      error: () => { this.loadingExecuting = false; }
    });
  }

  loadDone() {
    this.loadingDone = true;
    this.svc.getChanges({}).subscribe({
      next: data => {
        const all: ChangeListItem[] = data?.content ?? data ?? [];
        this.doneItems = all.filter((c: ChangeListItem) => c.status === 'SUCCESS' || c.status === 'FAIL');
        this.loadingDone = false;
      },
      error: () => { this.loadingDone = false; }
    });
  }

  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('vi-VN', { day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit' });
  }
}
