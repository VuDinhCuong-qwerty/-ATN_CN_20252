import { Routes } from '@angular/router';
import { authGuard } from './core/auth/auth.guard';
import { AppShellComponent } from './layout/app-shell/app-shell.component';

export const routes: Routes = [
  // Public routes (no shell)
  { path: 'login',    loadComponent: () => import('./pages/login/login.component').then(m => m.LoginComponent) },
  { path: 'callback', loadComponent: () => import('./pages/callback/callback.component').then(m => m.CallbackComponent) },

  // Protected routes (inside AppShell)
  {
    path: '',
    component: AppShellComponent,
    canActivate: [authGuard],
    children: [
      { path: '',           redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard',  loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent) },
      { path: 'changes',         loadComponent: () => import('./pages/change-list/change-list.component').then(m => m.ChangeListComponent) },
      { path: 'changes/new',     loadComponent: () => import('./pages/change-form/change-form.component').then(m => m.ChangeFormComponent) },
      { path: 'changes/:id/edit',loadComponent: () => import('./pages/change-form/change-form.component').then(m => m.ChangeFormComponent) },
      { path: 'changes/:id',     loadComponent: () => import('./pages/change-detail/change-detail.component').then(m => m.ChangeDetailComponent) },
      { path: 'approval',        loadComponent: () => import('./pages/approval/approval.component').then(m => m.ApprovalComponent) },
      { path: 'approval/queue',  loadComponent: () => import('./pages/approval/approval.component').then(m => m.ApprovalComponent) },
      { path: 'approval/history',loadComponent: () => import('./pages/approval/approval.component').then(m => m.ApprovalComponent) },
      { path: 'execution',       loadComponent: () => import('./pages/execution/execution.component').then(m => m.ExecutionComponent) },
      { path: 'reports',         loadComponent: () => import('./pages/reports/reports.component').then(m => m.ReportsComponent) },
    ]
  },

  { path: '**', redirectTo: '/dashboard' }
];
