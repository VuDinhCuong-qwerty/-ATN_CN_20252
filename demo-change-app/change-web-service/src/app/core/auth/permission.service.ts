import { Injectable } from '@angular/core';
import { TokenStoreService } from './token-store.service';

const PREFIX = 'change-mgmt/change-request:';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  constructor(private tokenStore: TokenStoreService) {}

  has(action: string): boolean {
    const info = this.tokenStore.getUserInfo();
    if (!info?.permissions) return false;
    return (info.permissions as string[]).includes(PREFIX + action);
  }

  get info(): any { return this.tokenStore.getUserInfo() || {}; }
  get username(): string { return this.info.username || ''; }
  get employeeCode(): string { return this.info.employeeCode || ''; }
  get displayName(): string { return this.info.displayName || this.username; }
  get role(): string { return this.info.role || ''; }
}
