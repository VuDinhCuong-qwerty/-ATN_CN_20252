import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class TokenStoreService {

  setToken(token: string) { sessionStorage.setItem('token', token); }
  getToken(): string | null { return sessionStorage.getItem('token'); }

  setRefreshToken(token: string) { sessionStorage.setItem('refresh_token', token); }
  getRefreshToken(): string | null { return sessionStorage.getItem('refresh_token'); }

  setUserInfo(info: any) { sessionStorage.setItem('userinfo', JSON.stringify(info)); }
  getUserInfo(): any {
    const raw = sessionStorage.getItem('userinfo');
    return raw ? JSON.parse(raw) : null;
  }

  isLoggedIn(): boolean { return !!this.getToken(); }

  clear() {
    sessionStorage.removeItem('token');
    sessionStorage.removeItem('refresh_token');
    sessionStorage.removeItem('userinfo');
  }
}
