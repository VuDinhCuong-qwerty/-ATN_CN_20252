import { Component, HostListener, OnInit } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router, NavigationEnd } from '@angular/router';
import { filter } from 'rxjs/operators';
import { TokenStoreService } from './core/auth/token-store.service';
import { AuthService } from './core/auth/auth.service';
const HIDE_HEADER = new Set(['/', '/login', '/callback']);

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  template: `
    @if (showHeader) {
      <header class="app-header">
        <div class="header-inner">

          <a class="header-logo" routerLink="/changes">
            <i class="bi bi-lightning-charge-fill"></i>
            <span>Change Manager</span>
          </a>

          <!-- Nav + avatar dồn bên phải -->
          <div class="header-right">
            <nav class="header-nav">
              <a class="nav-tab" routerLink="/changes"
                 routerLinkActive="active"
                 [routerLinkActiveOptions]="{exact: true}">
                Danh sách Changes
              </a>

              <a class="nav-tab" routerLink="/changes/new"
                 routerLinkActive="active"
                 [routerLinkActiveOptions]="{exact: true}">
                Tạo change mới
              </a>

            </nav>

            <!-- Avatar circle -->
            <div class="header-user-wrapper">
            <button class="user-avatar-btn" (click)="toggleMenu()" [title]="displayName">
              <span class="ua-letter">{{ avatarLetter }}</span>
            </button>
            @if (menuOpen) {
              <div class="user-dropdown">
                <div class="ud-info">
                  <div class="ud-name">{{ displayName }}</div>
                  <div class="ud-role">{{ role }}</div>
                </div>
                <hr class="ud-hr">
                <button class="ud-logout" (click)="logout()">
                  <i class="bi bi-box-arrow-right"></i>
                  Đăng xuất
                </button>
              </div>
            }
            </div><!-- /header-user-wrapper -->
          </div><!-- /header-right -->

        </div>
      </header>
    }

    <main [class.has-header]="showHeader">
      <router-outlet></router-outlet>
    </main>
  `,
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  showHeader = false;
  menuOpen = false;
  displayName = '';
  role = '';

  constructor(
    private router: Router,
    private tokenStore: TokenStoreService,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.router.events.pipe(filter(e => e instanceof NavigationEnd))
      .subscribe((e: any) => {
        const path = (e.urlAfterRedirects || '').split('?')[0];
        this.showHeader = !HIDE_HEADER.has(path);
        this.menuOpen = false;
        if (this.showHeader) this.refreshUserInfo();
      });
  }

  private refreshUserInfo() {
    const info = this.tokenStore.getUserInfo();
    this.displayName = info?.displayName || info?.username || 'Người dùng';
    this.role = info?.role || '';
  }

  get avatarLetter(): string {
    return (this.displayName || '?').charAt(0).toUpperCase();
  }

  toggleMenu() { this.menuOpen = !this.menuOpen; }

  @HostListener('document:click', ['$event'])
  onDocClick(e: MouseEvent) {
    if (!(e.target as HTMLElement).closest('.header-user-wrapper')) {
      this.menuOpen = false;
    }
  }

  logout() {
    this.menuOpen = false;
    this.authService.logout();
  }
}
