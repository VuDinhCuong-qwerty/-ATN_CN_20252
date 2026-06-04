import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = '/api/app';

@Injectable({ providedIn: 'root' })
export class AppApiService {
  constructor(private http: HttpClient) {}

  getRoles(): Observable<any> {
    return this.http.get(`${BASE}/reference/roles`);
  }

  getDepartments(): Observable<any> {
    return this.http.get(`${BASE}/reference/departments`);
  }

  getPositions(): Observable<any> {
    return this.http.get(`${BASE}/reference/positions`, { params: { status: 'ACTIVE' } });
  }

  getApplications(params?: any): Observable<any> {
    return this.http.get(`${BASE}/applications`, { params });
  }

  getApplicationsForSelect(): Observable<any> {
    return this.http.get(`${BASE}/applications`, { params: { status: 'ACTIVE', size: '999', page: '0' } });
  }

  getResourcesForSelect(appId: number): Observable<any> {
    return this.http.get(`${BASE}/applications/${appId}/resources`, { params: { size: '999', page: '0' } });
  }

  getAppDetail(id: number): Observable<any> {
    return this.http.get(`${BASE}/applications/${id}`);
  }

  getResources(appId: number, params?: any): Observable<any> {
    return this.http.get(`${BASE}/applications/${appId}/resources`, { params });
  }

  getClients(params?: any): Observable<any> {
    return this.http.get(`${BASE}/clients`, { params });
  }

  getClientDetail(clientId: string): Observable<any> {
    return this.http.get(`${BASE}/clients/${clientId}`);
  }

  createClient(body: any): Observable<any> {
    return this.http.post(`${BASE}/clients`, body);
  }

  updateClient(id: number, body: any): Observable<any> {
    return this.http.post(`${BASE}/clients/${id}/update`, body);
  }

  resetClientSecret(id: number): Observable<any> {
    return this.http.post(`${BASE}/clients/${id}/secret/reset`, {});
  }

  manageClientScopes(id: number, body: { action: string; scopes: string[] }): Observable<any> {
    return this.http.post(`${BASE}/clients/${id}/scopes`, body);
  }

  getAuthFlows(appId: number): Observable<any> {
    return this.http.get(`${BASE}/applications/${appId}/flows`);
  }

  createFlow(appId: number, body: { alias: string; description?: string }): Observable<any> {
    return this.http.post(`${BASE}/applications/${appId}/flows`, body);
  }

  getAuthFlowDetail(appId: number, flowId: number): Observable<any> {
    return this.http.get(`${BASE}/applications/${appId}/flows/${flowId}`);
  }

  getDefaultAppPerms(params?: any): Observable<any> {
    return this.http.get(`${BASE}/default-permissions/applications`, { params });
  }

  createDefaultAppPerms(body: { items: any[] }): Observable<any> {
    return this.http.post(`${BASE}/default-permissions/applications/batch`, body);
  }

  updateDefaultAppPermStatus(body: { items: { id: number; status: string }[] }): Observable<any> {
    return this.http.post(`${BASE}/default-permissions/applications/batch/status`, body);
  }

  getDefaultResourcePerms(params?: any): Observable<any> {
    return this.http.get(`${BASE}/default-permissions/resources`, { params });
  }

  createDefaultResourcePerms(body: { items: any[] }): Observable<any> {
    return this.http.post(`${BASE}/default-permissions/resources/batch`, body);
  }

  updateDefaultResourcePerms(body: { items: any[] }): Observable<any> {
    return this.http.post(`${BASE}/default-permissions/resources/batch/update`, body);
  }

  createApplication(body: any): Observable<any> {
    return this.http.post(`${BASE}/applications`, body);
  }

  updateApplication(id: number, body: any): Observable<any> {
    return this.http.post(`${BASE}/applications/${id}/update`, body);
  }

  toggleAppStatus(id: number, status: string): Observable<any> {
    return this.http.post(`${BASE}/applications/${id}/status`, { status });
  }

  getResourceDetail(appId: number, resourceId: number): Observable<any> {
    return this.http.get(`${BASE}/applications/${appId}/resources/${resourceId}`);
  }

  createResources(appId: number, body: any): Observable<any> {
    return this.http.post(`${BASE}/applications/${appId}/resources/batch`, body);
  }

  updateResource(appId: number, resourceId: number, body: any): Observable<any> {
    return this.http.post(`${BASE}/applications/${appId}/resources/${resourceId}/update`, body);
  }
}
