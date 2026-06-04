import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-callback',
  imports: [CommonModule],
  template: `
    <div class="d-flex justify-content-center align-items-center" style="height:80vh">
      <div class="text-center">
        <div class="spinner-border text-primary mb-3" role="status"></div>
        <p class="text-muted">Đang xác thực...</p>
        <p *ngIf="error" class="text-danger mt-2">{{ error }}</p>
      </div>
    </div>
  `
})
export class CallbackComponent implements OnInit {
  error = '';

  constructor(private route: ActivatedRoute, private authService: AuthService) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      const code = params['code'];
      const state = params['state'];
      const err = params['error'];

      if (err) {
        this.error = `Lỗi: ${err} — ${params['error_description'] ?? ''}`;
        return;
      }
      if (code && state) {
        this.authService.exchangeCode(code, state);
      } else {
        this.error = 'Thiếu code hoặc state trong callback URL';
      }
    });
  }
}
