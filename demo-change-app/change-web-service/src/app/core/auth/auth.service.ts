import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Router } from '@angular/router';
import { TokenStoreService } from './token-store.service';
import { environment } from '../../../environments/environment';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly AUTH_URL    = environment.authUrl;
  private readonly TOKEN_URL   = environment.tokenUrl;
  private readonly LOGOUT_URL  = environment.logoutUrl;
  private readonly CLIENT_ID   = environment.clientId;
  private readonly REDIRECT_URI = environment.redirectUri;

  constructor(
    private http: HttpClient,
    private tokenStore: TokenStoreService,
    private router: Router
  ) {}

  login() {
    const state = this.generateRandom();
    const codeVerifier = this.generateRandom(64);
    sessionStorage.setItem('pkce_verifier', codeVerifier);
    sessionStorage.setItem('pkce_state', state);

    this.generateCodeChallenge(codeVerifier).then(challenge => {
      const params = new URLSearchParams({
        response_type: 'code',
        client_id: this.CLIENT_ID,
        redirect_uri: this.REDIRECT_URI,
        scope: 'openid profile',
        state,
        code_challenge: challenge,
        code_challenge_method: 'S256'
      });
      window.location.href = `${this.AUTH_URL}?${params.toString().replace(/\+/g, '%20')}`;
    });
  }

  exchangeCode(code: string, state: string) {
    const storedState  = sessionStorage.getItem('pkce_state');
    const codeVerifier = sessionStorage.getItem('pkce_verifier');

    if (state !== storedState) {
      console.error('PKCE state mismatch', { received: state, stored: storedState });
      this.router.navigate(['/login']);
      return;
    }

    const body = new URLSearchParams({
      grant_type: 'authorization_code',
      client_id: this.CLIENT_ID,
      redirect_uri: this.REDIRECT_URI,
      code,
      code_verifier: codeVerifier!
    });

    this.http.post<any>(this.TOKEN_URL, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    }).subscribe({
      next: res => {
        const userInfo = this.parseJwt(res.access_token);
        this.tokenStore.setToken(res.access_token);
        if (res.refresh_token) this.tokenStore.setRefreshToken(res.refresh_token);
        this.tokenStore.setUserInfo(userInfo);
        sessionStorage.removeItem('pkce_verifier');
        sessionStorage.removeItem('pkce_state');
        this.router.navigate(['/changes']);
      },
      error: err => {
        console.error('Token exchange failed', err);
        this.router.navigate(['/login']);
      }
    });
  }

  refreshToken(): Observable<any> {
    const refreshToken = this.tokenStore.getRefreshToken();
    const body = new URLSearchParams({
      grant_type: 'refresh_token',
      client_id: this.CLIENT_ID,
      refresh_token: refreshToken!
    });
    return this.http.post<any>(this.TOKEN_URL, body.toString(), {
      headers: { 'Content-Type': 'application/x-www-form-urlencoded' }
    });
  }

  logout() {
    const token = this.tokenStore.getToken();
    this.tokenStore.clear();
    if (token) {
      this.http.post(this.LOGOUT_URL, null, {
        headers: { Authorization: `Bearer ${token}` }
      }).subscribe({ error: () => {} });
    }
    this.router.navigate(['/login']);
  }

  parseJwt(token: string): any {
    try {
      const base64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64));
    } catch {
      return {};
    }
  }

  private generateRandom(length = 32): string {
    const array = new Uint8Array(length);
    crypto.getRandomValues(array);
    return btoa(String.fromCharCode(...array))
      .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  }

  private async generateCodeChallenge(verifier: string): Promise<string> {
    const data = new TextEncoder().encode(verifier);
    const hash = await crypto.subtle.digest('SHA-256', data);
    return btoa(String.fromCharCode(...new Uint8Array(hash)))
      .replace(/\+/g, '-').replace(/\//g, '_').replace(/=/g, '');
  }
}
