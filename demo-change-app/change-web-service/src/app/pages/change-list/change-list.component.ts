import { Component, OnInit } from '@angular/core';
import { RouterLink, ActivatedRoute } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { ChangeService } from '../../shared/services/change.service';
import { PermissionService } from '../../core/auth/permission.service';
import { ChangeRequest } from '../../shared/models/change.model';

@Component({
  selector: 'app-change-list',
  standalone: true,
  imports: [RouterLink, FormsModule],
  templateUrl: './change-list.component.html',
  styleUrl: './change-list.component.css'
})
export class ChangeListComponent implements OnInit {
  changes: ChangeRequest[] = [];
  loading = false;
  error = '';

  // Filters — maps 1-to-1 with API query params
  filterStatus        = '';
  filterCreatedByCode = '';
  filterFromDate      = '';
  filterToDate        = '';

  // Pagination
  currentPage = 0;
  totalPage   = 0;
  totalElement = 0;
  readonly PAGE_SIZE = 10;

  readonly STATUS_LABELS: Record<string, string> = {
    DRAFT: 'Bản nháp', PENDING: 'Chờ duyệt',
    APPROVED: 'Đã duyệt', EXECUTING: 'Đang golive',
    SUCCESS: 'Thành công', FAIL: 'Thất bại'
  };

  constructor(
    private svc: ChangeService,
    public perm: PermissionService,
    private route: ActivatedRoute
  ) {}

  ngOnInit() {
    const status = this.route.snapshot.queryParamMap.get('status');
    if (status) this.filterStatus = status;
    this.load();
  }

  load(page = 0) {
    this.loading = true;
    this.error = '';
    this.currentPage = page;

    const params: Record<string, string> = {
      page: String(page),
      size: String(this.PAGE_SIZE)
    };
    if (this.filterStatus)        params['status']         = this.filterStatus;
    if (this.filterCreatedByCode.trim()) params['createdByCode'] = this.filterCreatedByCode.trim();
    if (this.filterFromDate)      params['fromDate']       = this.filterFromDate;
    if (this.filterToDate)        params['toDate']         = this.filterToDate;

    this.svc.getChanges(params).subscribe({
      next: res => {
        // res is already unwrapped (.data) — PageResponse shape
        this.changes      = res?.content     ?? [];
        this.totalPage    = res?.totalPage   ?? 0;
        this.totalElement = res?.totalElement ?? 0;
        this.currentPage  = res?.currentPage ?? page;
        this.loading = false;
      },
      error: () => { this.error = 'Không thể tải danh sách. Vui lòng thử lại.'; this.loading = false; }
    });
  }

  search() { this.load(0); }

  reset() {
    this.filterStatus        = '';
    this.filterCreatedByCode = '';
    this.filterFromDate      = '';
    this.filterToDate        = '';
    this.load(0);
  }

  prevPage() { if (this.currentPage > 0) this.load(this.currentPage - 1); }
  nextPage() { if (this.currentPage < this.totalPage - 1) this.load(this.currentPage + 1); }

  get pages(): number[] {
    return Array.from({ length: this.totalPage }, (_, i) => i);
  }

  shortId(id?: string) { return id ? id.substring(0, 8).toUpperCase() : '—'; }

  fmtDate(dt?: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric',
      hour: '2-digit', minute: '2-digit'
    });
  }

  fmtDateShort(dt?: string) {
    if (!dt) return '—';
    return new Date(dt).toLocaleString('vi-VN', {
      day: '2-digit', month: '2-digit', year: 'numeric'
    });
  }
}
