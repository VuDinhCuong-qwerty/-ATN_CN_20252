import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { ChangeRequest, UserSearchResult } from '../models/change.model';
import { environment } from '../../../environments/environment';

interface ApiResp<T> { status: number; data: T; errorDesc?: string; }

@Injectable({ providedIn: 'root' })
export class ChangeService {
  private readonly base = environment.apiBase;

  constructor(private http: HttpClient) {}

  getChanges(params?: Record<string, string>): Observable<any> {
    let p = new HttpParams();
    if (params) Object.entries(params).forEach(([k, v]) => { if (v) p = p.set(k, v); });
    return this.http.get<ApiResp<any>>(`${this.base}/api/changes`, { params: p }).pipe(map(r => r.data));
  }

  getChangeDetail(id: number): Observable<ChangeRequest> {
    return this.http.get<ApiResp<ChangeRequest>>(`${this.base}/api/changes/${id}`).pipe(map(r => r.data));
  }

  createChange(body: any): Observable<any> {
    return this.http.post<ApiResp<any>>(`${this.base}/api/changes`, body).pipe(map(r => r.data));
  }

  updateChange(id: number, body: any): Observable<any> {
    return this.http.put<ApiResp<any>>(`${this.base}/api/changes/${id}`, body).pipe(map(r => r.data));
  }

  submitChange(id: number): Observable<any> {
    return this.http.post<ApiResp<any>>(`${this.base}/api/changes/${id}/submit`, {}).pipe(map(r => r.data));
  }

  approveChange(id: number, note: string): Observable<any> {
    return this.http.post<ApiResp<any>>(`${this.base}/api/changes/${id}/approve`, { note }).pipe(map(r => r.data));
  }

  rejectChange(id: number, note: string): Observable<any> {
    return this.http.post<ApiResp<any>>(`${this.base}/api/changes/${id}/reject`, { note }).pipe(map(r => r.data));
  }

  executeChange(id: number): Observable<any> {
    return this.http.post<ApiResp<any>>(`${this.base}/api/changes/${id}/execute`, {}).pipe(map(r => r.data));
  }

  finalizeResult(id: number): Observable<any> {
    return this.http.post<ApiResp<any>>(`${this.base}/api/changes/${id}/result`, {}).pipe(map(r => r.data));
  }

  updateChecklistItemStatus(changeId: number, itemId: number, taskStatus: string): Observable<any> {
    return this.http.post<ApiResp<any>>(
      `${this.base}/api/changes/${changeId}/checklist/${itemId}/status`,
      { taskStatus }
    ).pipe(map(r => r.data));
  }

  searchUser(username: string): Observable<UserSearchResult[]> {
    return this.http.get<ApiResp<any>>(`${this.base}/api/users/search`, { params: { username } }).pipe(
      map(r => r.data?.content ?? [])
    );
  }
}
