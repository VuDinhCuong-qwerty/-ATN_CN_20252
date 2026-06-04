import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenStoreService } from '../auth/token-store.service';

// Token endpoint phải được bỏ qua để không vòng lặp vô hạn
const isTokenEndpoint = (url: string) => url.includes('/auth/token');

const cloneWithBearer = (req: HttpRequest<any>, token: string) =>
  req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const tokenStore = inject(TokenStoreService);
  const authService = inject(AuthService);

  // Không thêm token cho token endpoint (tránh vòng lặp)
  const shouldAttach = req.url.startsWith('/api/') && !isTokenEndpoint(req.url);
  const token = tokenStore.getToken();
  const authedReq = (shouldAttach && token) ? cloneWithBearer(req, token) : req;

  return next(authedReq).pipe(
    catchError(err => {
      // Chỉ xử lý 401 từ /api/ (không phải token endpoint)
      if (err.status !== 401 || !req.url.startsWith('/api/') || isTokenEndpoint(req.url)) {
        return throwError(() => err);
      }

      const refreshToken = tokenStore.getRefreshToken();
      if (!refreshToken) {
        authService.logout();
        return throwError(() => err);
      }

      // Refresh token rồi retry request gốc
      return authService.refreshToken().pipe(
        switchMap(res => {
          tokenStore.setToken(res.access_token);
          if (res.refresh_token) tokenStore.setRefreshToken(res.refresh_token);
          return next(cloneWithBearer(req, res.access_token));
        }),
        catchError(refreshErr => {
          // Refresh thất bại (refresh token hết hạn hoặc bị revoke) → logout
          authService.logout();
          return throwError(() => refreshErr);
        })
      );
    })
  );
};
