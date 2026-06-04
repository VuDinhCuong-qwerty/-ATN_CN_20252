import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: 'login',   loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: '',        redirectTo: '/login', pathMatch: 'full' },
  { path: 'callback', loadComponent: () => import('./pages/callback/callback.component').then(m => m.CallbackComponent) },
  { path: 'changes', canActivate: [authGuard], loadComponent: () => import('./pages/change-list/change-list.component').then(m => m.ChangeListComponent) },
  { path: 'changes/new',      canActivate: [authGuard], loadComponent: () => import('./pages/change-form/change-form.component').then(m => m.ChangeFormComponent) },
  { path: 'changes/:id/edit', canActivate: [authGuard], loadComponent: () => import('./pages/change-form/change-form.component').then(m => m.ChangeFormComponent) },
  { path: 'changes/:id',      canActivate: [authGuard], loadComponent: () => import('./pages/change-detail/change-detail.component').then(m => m.ChangeDetailComponent) },
  { path: '**', redirectTo: '/changes' }
];
