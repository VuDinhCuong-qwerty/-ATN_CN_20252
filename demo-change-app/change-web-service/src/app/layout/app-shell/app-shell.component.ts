import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { CommonModule } from '@angular/common';
import { PermissionService } from '../../core/auth/permission.service';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app-shell.component.html',
  styleUrl: './app-shell.component.css'
})
export class AppShellComponent {
  collapsed = false;
  changeExpanded = true;
  approvalExpanded = false;
  executionExpanded = false;

  constructor(public perm: PermissionService, private auth: AuthService) {}

  toggle() { this.collapsed = !this.collapsed; }
  logout() { this.auth.logout(); }
}
