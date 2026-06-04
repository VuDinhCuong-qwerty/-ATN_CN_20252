import { Injectable } from '@angular/core';
import { TokenStoreService } from './token-store.service';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  // Giai đoạn 1 (dev): true → toàn bộ UI hiển thị, không cần JWT
  // Giai đoạn 2 (auth): đổi thành false → đọc permissions thật từ JWT
  readonly DEV_MODE = false;

  private readonly SERVICE_CODE = 'iam-service';

  constructor(private tokenStore: TokenStoreService) {}

  private getPermissions(): string[] {
    if (this.DEV_MODE) return [];
    const userInfo = this.tokenStore.getUserInfo();
    return userInfo?.permissions ?? [];
  }

  // "iam-service/user:read" → { resource: 'user', action: 'read' }
  has(resource: string, action: string): boolean {
    if (this.DEV_MODE) return true;
    const target = `${this.SERVICE_CODE}/${resource}:${action}`;
    return this.getPermissions().includes(target);
  }

  // Có bất kỳ action nào của resource đó không
  hasResource(resource: string): boolean {
    if (this.DEV_MODE) return true;
    const prefix = `${this.SERVICE_CODE}/${resource}:`;
    return this.getPermissions().some(p => p.startsWith(prefix));
  }

  // Kiểm tra bất kỳ permission nào trong danh sách (format: "resource:action")
  hasAny(...perms: string[]): boolean {
    if (this.DEV_MODE) return true;
    return perms.some(p => {
      const [resource, action] = p.split(':');
      return this.has(resource, action);
    });
  }

  // Có bất kỳ permission nào thuộc nhóm resource[] không
  hasAnyResource(...resources: string[]): boolean {
    if (this.DEV_MODE) return true;
    return resources.some(r => this.hasResource(r));
  }

  get userInfo(): any {
    if (this.DEV_MODE) {
      return { username: 'dev_admin', role: 'ADMIN', employeeCode: 'DEV001', sub: '0' };
    }
    return this.tokenStore.getUserInfo();
  }
}
