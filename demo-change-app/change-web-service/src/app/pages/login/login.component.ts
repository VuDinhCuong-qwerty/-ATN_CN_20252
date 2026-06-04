import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';
import { TokenStoreService } from '../../core/auth/token-store.service';

@Component({
  selector: 'app-login',
  standalone: true,
  template: `
    <div class="login-page">
      <div class="deco deco-1"></div>
      <div class="deco deco-2"></div>

      <div class="login-card">
        <div class="card-top">
          <div class="logo-box">
            <i class="bi bi-lightning-charge-fill"></i>
          </div>
          <h1>Change Manager</h1>
          <p>Hệ thống quản lý Change & Go-Live</p>
        </div>
        <div class="card-mid">
          <p class="hint">Đăng nhập bằng tài khoản IAM Banking để tiếp tục sử dụng hệ thống.</p>
          <button class="btn-login" (click)="login()">
            <i class="bi bi-shield-lock-fill"></i>
            Đăng nhập với IAM
          </button>
        </div>
        <div class="card-bot">
          <i class="bi bi-bank2"></i>
          IAM Banking System
        </div>
      </div>
    </div>
  `,
  styles: [`
    .login-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #4C1D95 0%, #6D28D9 40%, #7C3AED 70%, #9333EA 100%);
      position: relative;
      overflow: hidden;
    }
    .deco { position: absolute; border-radius: 50%; background: rgba(255,255,255,0.06); pointer-events: none; }
    .deco-1 { width: 480px; height: 480px; top: -160px; right: -120px; }
    .deco-2 { width: 320px; height: 320px; bottom: -100px; left: -80px; }

    .login-card {
      background: #FFFFFF;
      border-radius: 20px;
      width: 100%;
      max-width: 400px;
      box-shadow: 0 24px 64px rgba(0,0,0,0.22);
      overflow: hidden;
      z-index: 1;
      position: relative;
    }

    .card-top {
      padding: 40px 32px 28px;
      background: linear-gradient(160deg, #F5F3FF, #EDE9FE);
      text-align: center;
      border-bottom: 1px solid #DDD6FE;
    }
    .logo-box {
      width: 68px; height: 68px;
      border-radius: 18px;
      background: linear-gradient(135deg, #6D28D9, #8B5CF6);
      display: flex; align-items: center; justify-content: center;
      margin: 0 auto 18px;
      box-shadow: 0 8px 24px rgba(109,40,217,0.35);
    }
    .logo-box i { font-size: 30px; color: white; }
    .card-top h1 { font-size: 22px; font-weight: 700; color: #1E1B4B; margin-bottom: 6px; }
    .card-top p  { font-size: 13px; color: #7C3AED; font-weight: 500; }

    .card-mid { padding: 32px; }
    .hint { font-size: 14px; color: #6B7280; text-align: center; margin-bottom: 24px; line-height: 1.7; }
    .btn-login {
      width: 100%;
      padding: 14px 24px;
      background: linear-gradient(135deg, #6D28D9, #7C3AED);
      color: white;
      border: none;
      border-radius: 12px;
      font-size: 15px;
      font-weight: 600;
      font-family: 'Inter', sans-serif;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 10px;
      transition: all 0.2s;
      box-shadow: 0 4px 16px rgba(109,40,217,0.35);
    }
    .btn-login:hover { transform: translateY(-2px); box-shadow: 0 8px 28px rgba(109,40,217,0.45); }
    .btn-login i { font-size: 18px; }

    .card-bot {
      padding: 14px;
      text-align: center;
      font-size: 12px;
      color: #9CA3AF;
      background: #FAFAFA;
      border-top: 1px solid #F0ECF9;
      display: flex;
      align-items: center;
      justify-content: center;
      gap: 6px;
    }
    .card-bot i { font-size: 14px; }
  `]
})
export class LoginComponent implements OnInit {
  constructor(
    private authService: AuthService,
    private tokenStore: TokenStoreService,
    private router: Router
  ) {}

  ngOnInit() {
    if (this.tokenStore.isLoggedIn()) {
      this.router.navigate(['/changes']);
    }
  }

  login() { this.authService.login(); }
}
