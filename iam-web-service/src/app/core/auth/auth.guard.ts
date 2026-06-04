import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStoreService } from './token-store.service';
import { PermissionService } from './permission.service';

export const authGuard: CanActivateFn = () => {
  const tokenStore = inject(TokenStoreService);
  const perm = inject(PermissionService);
  const router = inject(Router);

  if (perm.DEV_MODE || tokenStore.isLoggedIn()) return true;

  return router.createUrlTree(['/login']);
};
