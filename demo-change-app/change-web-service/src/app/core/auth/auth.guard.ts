import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { TokenStoreService } from './token-store.service';

export const authGuard: CanActivateFn = () => {
  const tokenStore = inject(TokenStoreService);
  if (tokenStore.isLoggedIn()) return true;
  return inject(Router).createUrlTree(['/login']);
};
