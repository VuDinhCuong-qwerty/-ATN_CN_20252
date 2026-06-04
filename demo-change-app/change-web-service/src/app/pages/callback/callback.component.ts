import { Component, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-callback',
  standalone: true,
  template: `
    <div class="cb-wrap">
      <div class="cb-card">
        @if (!error) {
          <div class="cb-spinner"></div>
          <p class="cb-title">Đang xác thực...</p>
          <p class="cb-sub">Vui lòng đợi trong giây lát</p>
        } @else {
          <i class="bi bi-exclamation-triangle-fill" style="font-size:36px;color:#DC2626;margin-bottom:16px;display:block"></i>
          <p class="cb-title" style="color:#DC2626">Xác thực thất bại</p>
          <p class="cb-sub">{{ error }}</p>
          <a href="/login" style="margin-top:16px;display:inline-block;color:#7C3AED;font-size:13px">← Quay lại đăng nhập</a>
        }
      </div>
    </div>
  `,
  styles: [`
    .cb-wrap {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #4C1D95, #7C3AED);
    }
    .cb-card {
      background: white;
      border-radius: 18px;
      padding: 52px 48px;
      text-align: center;
      box-shadow: 0 20px 60px rgba(0,0,0,0.2);
      min-width: 300px;
    }
    .cb-spinner {
      width: 48px; height: 48px;
      border: 4px solid #EDE9FE;
      border-top-color: #7C3AED;
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin: 0 auto 20px;
    }
    @keyframes spin { to { transform: rotate(360deg); } }
    .cb-title { font-size: 16px; font-weight: 700; color: #1E1B4B; margin: 0 0 6px; }
    .cb-sub   { font-size: 13px; color: #9CA3AF; margin: 0; }
  `]
})
export class CallbackComponent implements OnInit {
  error = '';

  constructor(private route: ActivatedRoute, private authService: AuthService) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const code  = params['code'];
      const state = params['state'];
      const err   = params['error'];

      if (err) {
        this.error = `${err}: ${params['error_description'] ?? ''}`;
        return;
      }
      if (code && state) {
        this.authService.exchangeCode(code, state);
      }
      // Không redirect khi không có params — component sẽ bị destroy bởi router
    });
  }
}
