import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { PermissionService } from '../../../core/auth/permission.service';
import { AppApiService } from '../../../shared/services/app-api.service';

interface FlatNode {
  node: any;
  depth: number;
  isLast: boolean;
  continues: boolean[];
}

@Component({
  selector: 'app-flow-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './flow-list.component.html',
  styleUrl: './flow-list.component.css'
})
export class FlowListComponent implements OnInit {
  applications: any[] = [];
  selectedAppId: any = '';
  flows: any[] = [];
  activeFlow: any = null;
  loading = false;
  loadingDetail = false;
  flatNodes: FlatNode[] = [];

  showCreateModal = false;
  createForm = { alias: '', description: '' };
  loadingCreate = false;
  createMessage = '';
  createMessageType = '';

  constructor(public perm: PermissionService, private appApiService: AppApiService) {}

  ngOnInit() {
    this.appApiService.getApplications({ size: 200 }).subscribe({
      next: res => {
        this.applications = res.data?.content ?? res.data ?? [];
        if (this.applications.length > 0) {
          this.selectedAppId = this.applications[0].id;
          this.loadFlows();
        }
      }
    });
  }

  loadFlows() {
    if (!this.selectedAppId) return;
    this.loading = true;
    this.activeFlow = null;
    this.flatNodes = [];
    this.appApiService.getAuthFlows(Number(this.selectedAppId)).subscribe({
      next: res => {
        this.loading = false;
        this.flows = res.data?.content ?? res.data ?? [];
        const active = this.flows.find(f => f.status === 'ACTIVE') ?? this.flows[0];
        if (active) this.loadFlowDetail(active.id);
      },
      error: () => { this.loading = false; }
    });
  }

  loadFlowDetail(flowId: number) {
    this.loadingDetail = true;
    this.appApiService.getAuthFlowDetail(Number(this.selectedAppId), flowId).subscribe({
      next: res => {
        this.loadingDetail = false;
        this.activeFlow = res.data ?? res;
        this.flatNodes = this.flattenTree(this.activeFlow.executions ?? [], 0, []);
      },
      error: () => { this.loadingDetail = false; }
    });
  }

  onAppChange() {
    this.loadFlows();
  }

  private flattenTree(nodes: any[], depth: number, continues: boolean[]): FlatNode[] {
    const result: FlatNode[] = [];
    for (let i = 0; i < nodes.length; i++) {
      const isLast = i === nodes.length - 1;
      result.push({ node: nodes[i], depth, isLast, continues });
      if (nodes[i].children?.length) {
        result.push(...this.flattenTree(nodes[i].children, depth + 1, [...continues, !isLast]));
      }
    }
    return result;
  }

  openCreate() {
    this.createForm = { alias: '', description: '' };
    this.createMessage = '';
    this.showCreateModal = true;
  }

  closeCreate() { this.showCreateModal = false; }

  submitCreate() {
    if (!this.createForm.alias.trim()) {
      this.createMessage = 'Alias là bắt buộc';
      this.createMessageType = 'warning';
      return;
    }
    this.loadingCreate = true;
    this.appApiService.createFlow(Number(this.selectedAppId), this.createForm).subscribe({
      next: () => {
        this.loadingCreate = false;
        this.showCreateModal = false;
        this.loadFlows();
      },
      error: err => {
        this.loadingCreate = false;
        this.createMessage = `Lỗi: ${err.error?.message ?? 'Không thể tạo flow'}`;
        this.createMessageType = 'danger';
      }
    });
  }

  reqClass(requirement: string): string {
    const map: Record<string, string> = {
      REQUIRED: 'req-REQUIRED',
      OPTIONAL: 'req-OPTIONAL',
      ALTERNATIVE: 'req-ALTERNATIVE',
      DISABLED: 'req-DISABLED',
    };
    return map[requirement] ?? 'req-DISABLED';
  }
}
