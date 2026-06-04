import { HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from '../auth/auth.service';
import { TokenStoreService } from '../auth/token-store.service';

const isTokenEndpoint = (url: string) => url.includes('/auth/token');

const withBearer = (req: HttpRequest<any>, token: string) =>
  req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const store = inject(TokenStoreService);
  const auth  = inject(AuthService);

  const shouldAttach = req.url.startsWith('/api/') && !isTokenEndpoint(req.url);
  const token = store.getToken();
  const outReq = (shouldAttach && token) ? withBearer(req, token) : req;

  return next(outReq).pipe(
    catchError(err => {
      // Only retry 401 on API calls (not the token endpoint itself)
      if (err.status !== 401 || !req.url.startsWith('/api/') || isTokenEndpoint(req.url)) {
        return throwError(() => err);
      }

      const refreshToken = store.getRefreshToken();
      if (!refreshToken) {
        auth.logout();
        return throwError(() => err);
      }

      return auth.refreshToken().pipe(
        switchMap(res => {
          store.setToken(res.access_token);
          if (res.refresh_token) store.setRefreshToken(res.refresh_token);
          return next(withBearer(req, res.access_token));
        }),
        catchError(refreshErr => {
          auth.logout();
          return throwError(() => refreshErr);
        })
      );
    })
  );
};
