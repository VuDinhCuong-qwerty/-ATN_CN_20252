import { Routes } from '@angular/router';
import { LoginComponent } from './features/login/login.component';
import { CallbackComponent } from './features/callback/callback.component';
import { UsersComponent } from './features/users/users.component';
import { UserDetailComponent } from './features/users/detail/user-detail.component';
import { UserCreateComponent } from './features/users/create/user-create.component';
import { UserLifecycleComponent } from './features/users/lifecycle/user-lifecycle.component';
import { UserRolesComponent } from './features/users/roles/user-roles.component';
import { UserCredentialComponent } from './features/users/credentials/user-credential.component';
import { PermissionsComponent } from './features/permissions/permissions.component';
import { PermissionRequestCreateComponent } from './features/permissions/create/perm-request-create.component';
import { PermissionRequestDetailComponent } from './features/permissions/detail/perm-request-detail.component';
import { AppListComponent } from './features/config/apps/app-list.component';
import { AppDetailComponent } from './features/config/apps/detail/app-detail.component';
import { AppCreateComponent } from './features/config/apps/create/app-create.component';
import { ResourceListComponent } from './features/config/resources/resource-list.component';
import { ClientListComponent } from './features/config/clients/client-list.component';
import { ClientDetailComponent } from './features/config/clients/detail/client-detail.component';
import { FlowListComponent } from './features/config/flows/flow-list.component';
import { DefaultPermListComponent } from './features/config/default-perms/default-perm-list.component';
import { authGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: 'login', component: LoginComponent },
  { path: '', redirectTo: 'users', pathMatch: 'full' },
  { path: 'callback', component: CallbackComponent },
  { path: 'users', component: UsersComponent, canActivate: [authGuard] },
  { path: 'users/detail', component: UserDetailComponent, canActivate: [authGuard] },
  { path: 'users/create', component: UserCreateComponent, canActivate: [authGuard] },
  { path: 'users/lifecycle', component: UserLifecycleComponent, canActivate: [authGuard] },
  { path: 'users/permissions', component: PermissionsComponent, canActivate: [authGuard] },
  { path: 'users/permissions/create', component: PermissionRequestCreateComponent, canActivate: [authGuard] },
  { path: 'users/permissions/detail', component: PermissionRequestDetailComponent, canActivate: [authGuard] },
  { path: 'users/roles', component: UserRolesComponent, canActivate: [authGuard] },
  { path: 'users/credentials', component: UserCredentialComponent, canActivate: [authGuard] },
  { path: 'config/apps', component: AppListComponent, canActivate: [authGuard] },
  { path: 'config/apps/create', component: AppCreateComponent, canActivate: [authGuard] },
  { path: 'config/apps/detail', component: AppDetailComponent, canActivate: [authGuard] },
  { path: 'config/resources', component: ResourceListComponent, canActivate: [authGuard] },
  { path: 'config/clients', component: ClientListComponent, canActivate: [authGuard] },
  { path: 'config/clients/detail', component: ClientDetailComponent, canActivate: [authGuard] },
  { path: 'config/flows', component: FlowListComponent, canActivate: [authGuard] },
  { path: 'config/default-perms', component: DefaultPermListComponent, canActivate: [authGuard] },
];
