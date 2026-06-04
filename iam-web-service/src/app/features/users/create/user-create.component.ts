import { Component, HostListener, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { IdentityService } from '../../../shared/services/identity.service';
import { AppApiService } from '../../../shared/services/app-api.service';

interface AddrState {
  enabled: boolean;
  provinceCode: any;
  provinceName: string;
  wardCode: any;
  wardName: string;
  detail: string;
  provinceSugg: any[];
  wardSugg: any[];
  showProvinceSugg: boolean;
  showWardSugg: boolean;
}

@Component({
  selector: 'app-user-create',
  imports: [CommonModule, FormsModule, RouterLink],
  templateUrl: './user-create.component.html',
  styleUrl: './user-create.component.css'
})
export class UserCreateComponent implements OnInit {
  form = {
    fullName: '', dob: '', gender: 'MALE',
    nationality: 'Việt Nam', ethnic: 'Kinh', religion: 'Không',
    avatarUrl: '',
    numberId: '', numberIdIssuedDate: '', numberIdIssuedPlace: '',
    departmentId: '' as any, position: '', joinDate: '',
    mobile: '', mail: '',
  };

  selectedRoleCodes: string[] = [];
  departments: any[] = [];
  flatDepartments: any[] = [];
  deptInputValue = '';
  deptSugg: any[] = [];
  showDeptSugg = false;
  positions: any[] = [];
  roles: any[] = [];

  private emptyAddr = (): AddrState => ({
    enabled: false,
    provinceCode: '', provinceName: '',
    wardCode: '', wardName: '',
    detail: '',
    provinceSugg: [], wardSugg: [],
    showProvinceSugg: false, showWardSugg: false,
  });

  addrs: Record<string, AddrState> = {
    temporary: this.emptyAddr(),
    permanent: this.emptyAddr(),
    birth:     this.emptyAddr(),
  };

  loading = false;
  message = '';
  messageType = '';

  private searchTimers: Record<string, any> = {};

  constructor(
    private router: Router,
    private identityService: IdentityService,
    private appApiService: AppApiService,
  ) {}

  ngOnInit() {
    this.appApiService.getDepartments().subscribe({
      next: r => {
        this.departments = r.data ?? [];
        this.flatDepartments = this.flattenDepts(this.departments);
      }
    });
    this.appApiService.getPositions().subscribe({ next: r => this.positions = r.data ?? [] });
    this.appApiService.getRoles().subscribe({ next: r => this.roles = r.data ?? [] });
  }

  private flattenDepts(nodes: any[], depth = 0): any[] {
    const result: any[] = [];
    for (const n of nodes) {
      const indent = depth === 0 ? '' : '  '.repeat(depth - 1) + '└ ';
      result.push({ id: n.id, code: n.code, name: n.name, displayName: indent + n.name, depth });
      if (n.children?.length) result.push(...this.flattenDepts(n.children, depth + 1));
    }
    return result;
  }

  // ── Department autocomplete ──────────────────────────────────────────────

  onDeptInput(event: Event) {
    const value = (event.target as HTMLInputElement).value;
    this.deptInputValue = value;
    this.form.departmentId = '';
    if (!value) { this.deptSugg = []; this.showDeptSugg = false; return; }
    const q = value.toLowerCase();
    this.deptSugg = this.flatDepartments.filter(d => d.name.toLowerCase().includes(q));
    this.showDeptSugg = true;
  }

  selectDept(dept: any) {
    this.form.departmentId = dept.id;
    this.deptInputValue = dept.name;
    this.showDeptSugg = false;
  }

  // ── Province autocomplete ────────────────────────────────────────────────

  onProvinceInput(type: string, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    const addr = this.addrs[type];
    if (!value) {
      addr.provinceCode = '';
      addr.provinceName = '';
      addr.wardCode = '';
      addr.wardName = '';
      addr.provinceSugg = [];
      addr.showProvinceSugg = false;
      return;
    }
    addr.showProvinceSugg = true;
    clearTimeout(this.searchTimers['prov_' + type]);
    this.searchTimers['prov_' + type] = setTimeout(() => {
      this.identityService.getProvinces(value).subscribe({
        next: r => { addr.provinceSugg = r.data ?? []; }
      });
    }, 300);
  }

  selectProvince(type: string, province: any) {
    const addr = this.addrs[type];
    addr.provinceCode = province.code;
    addr.provinceName = province.name;
    addr.showProvinceSugg = false;
    addr.wardCode = '';
    addr.wardName = '';
    addr.wardSugg = [];
    addr.showWardSugg = false;
  }

  // ── Ward autocomplete ────────────────────────────────────────────────────

  onWardInput(type: string, event: Event) {
    const value = (event.target as HTMLInputElement).value;
    const addr = this.addrs[type];
    if (!value) {
      addr.wardCode = '';
      addr.wardName = '';
      addr.wardSugg = [];
      addr.showWardSugg = false;
      return;
    }
    addr.showWardSugg = true;
    clearTimeout(this.searchTimers['ward_' + type]);
    this.searchTimers['ward_' + type] = setTimeout(() => {
      this.identityService.getWards(Number(addr.provinceCode), value).subscribe({
        next: r => { addr.wardSugg = r.data ?? []; }
      });
    }, 300);
  }

  selectWard(type: string, ward: any) {
    const addr = this.addrs[type];
    addr.wardCode = ward.code;
    addr.wardName = ward.name;
    addr.showWardSugg = false;
  }

  @HostListener('document:click')
  closeAllSugg() {
    this.showDeptSugg = false;
    Object.values(this.addrs).forEach(a => {
      a.showProvinceSugg = false;
      a.showWardSugg = false;
    });
  }

  // ── Roles ────────────────────────────────────────────────────────────────

  toggleRole(code: string) {
    const idx = this.selectedRoleCodes.indexOf(code);
    if (idx >= 0) this.selectedRoleCodes.splice(idx, 1);
    else this.selectedRoleCodes.push(code);
  }

  isRoleSelected(code: string) { return this.selectedRoleCodes.includes(code); }

  goBack() { this.router.navigate(['/users']); }

  // ── Submit ───────────────────────────────────────────────────────────────

  submit() {
    const f = this.form;
    if (!f.fullName || !f.dob || !f.numberId || !f.numberIdIssuedDate ||
        !f.numberIdIssuedPlace || !f.departmentId || !f.joinDate || !f.mobile || !f.mail) {
      this.showMsg('Vui lòng điền đủ các trường bắt buộc (*)', 'warning'); return;
    }
    if (this.selectedRoleCodes.length === 0) {
      this.showMsg('Vui lòng chọn ít nhất một role', 'warning'); return;
    }

    const toAddr = (a: AddrState) =>
      a.enabled && a.provinceCode && a.wardCode && a.detail
        ? { provinceCode: Number(a.provinceCode), wardCode: Number(a.wardCode), detail: a.detail }
        : null;

    const body: any = {
      fullName: f.fullName, dob: f.dob, gender: f.gender,
      nationality: f.nationality, ethnic: f.ethnic, religion: f.religion,
      numberId: f.numberId, numberIdIssuedDate: f.numberIdIssuedDate,
      numberIdIssuedPlace: f.numberIdIssuedPlace,
      departmentId: Number(f.departmentId),
      joinDate: f.joinDate, mobile: f.mobile, mail: f.mail,
      roles: this.selectedRoleCodes.map(code => ({ code })),
      temporaryAddress: toAddr(this.addrs['temporary']),
      permanentAddress: toAddr(this.addrs['permanent']),
      birthAddress: toAddr(this.addrs['birth']),
    };
    if (f.position) body.position = f.position;
    if (f.avatarUrl) body.avatarUrl = f.avatarUrl;

    this.loading = true;
    this.identityService.createUser(body).subscribe({
      next: res => {
        this.loading = false;
        this.showMsg(`Tạo user thành công! Username: ${res.data?.username ?? '—'}`, 'success');
        setTimeout(() => this.router.navigate(['/users']), 2500);
      },
      error: err => {
        this.loading = false;
        this.showMsg(`Lỗi: ${err.error?.message ?? 'Không thể tạo user'}`, 'danger');
      }
    });
  }

  private showMsg(msg: string, type: string) {
    this.message = msg; this.messageType = type;
    setTimeout(() => this.message = '', 7000);
  }
}
