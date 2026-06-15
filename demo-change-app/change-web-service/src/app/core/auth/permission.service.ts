import { Injectable } from '@angular/core';
import { TokenStoreService } from './token-store.service';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly SERVICE_CODE = 'change-mgmt';

  constructor(private tokenStore: TokenStoreService) {}

  private getPermissions(): string[] {
    return (this.tokenStore.getUserInfo()?.permissions as string[]) ?? [];
  }

  /** Check single permission: has('change-request', 'create') */
  has(resource: string, action: string): boolean {
    return this.getPermissions().includes(`${this.SERVICE_CODE}/${resource}:${action}`);
  }

  /** Check if user has ANY action on a resource (for menu visibility) */
  hasResource(resource: string): boolean {
    const prefix = `${this.SERVICE_CODE}/${resource}:`;
    return this.getPermissions().some(p => p.startsWith(prefix));
  }

  /** Check any of multiple 'resource:action' strings */
  hasAny(...checks: string[]): boolean {
    return checks.some(c => {
      const [resource, action] = c.split(':');
      return this.has(resource, action);
    });
  }

  get info(): any        { return this.tokenStore.getUserInfo() || {}; }
  get username(): string { return this.info.username || ''; }
  get employeeCode(): string { return this.info.employeeCode || ''; }
  get displayName(): string  { return this.info.displayName || this.username; }
  get role(): string     { return this.info.role || ''; }
}
