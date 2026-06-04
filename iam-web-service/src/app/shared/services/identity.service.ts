import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

const BASE = '/api/identity';

@Injectable({ providedIn: 'root' })
export class IdentityService {
  constructor(private http: HttpClient) {}

  // Users
  createUser(body: any): Observable<any> {
    return this.http.post(`${BASE}/users`, body);
  }

  getUsers(params?: any): Observable<any> {
    return this.http.get(`${BASE}/users`, { params });
  }

  onboardUser(userId: number, employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/onboard`, body, { params: { userId, employeeCode } });
  }

  offboardUser(userId: number, employeeCode: string): Observable<any> {
    return this.http.post(`${BASE}/users/offboard`, null, { params: { userId, employeeCode } });
  }

  leaveUser(userId: number, employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/leave`, body, { params: { userId, employeeCode } });
  }

  returnUser(userId: number, employeeCode: string): Observable<any> {
    return this.http.post(`${BASE}/users/return`, null, { params: { userId, employeeCode } });
  }

  transferUser(userId: number, employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/transfer`, body, { params: { userId, employeeCode } });
  }

  // Permissions
  getAppPermissions(employeeCode: string, params?: any): Observable<any> {
    return this.http.get(`${BASE}/users/${employeeCode}/app-permissions`, { params });
  }

  getResourcePermissions(employeeCode: string, params?: any): Observable<any> {
    return this.http.get(`${BASE}/users/${employeeCode}/resource-permissions`, { params });
  }

  // Permission requests
  createPermissionRequest(body: any): Observable<any> {
    return this.http.post(`${BASE}/permission-requests`, body);
  }

  submitPermissionRequest(requestId: string, submiter: string, submitCode: string): Observable<any> {
    return this.http.post(`${BASE}/permission-requests/submit`, { requestId, submiter, submitCode });
  }

  getPermissionRequests(params?: any): Observable<any> {
    return this.http.get(`${BASE}/permission-requests`, { params });
  }

  getPermissionRequestDetail(requestId: number): Observable<any> {
    return this.http.get(`${BASE}/permission-requests/detail`, { params: { requestId } });
  }

  approveRequest(body: any): Observable<any> {
    return this.http.post(`${BASE}/permission-requests/approve`, body);
  }

  rejectRequest(body: any): Observable<any> {
    return this.http.post(`${BASE}/permission-requests/reject`, body);
  }

  updatePermissionRequest(body: any): Observable<any> {
    return this.http.post(`${BASE}/permission-requests/update`, body);
  }

  cancelRequest(requestId: number): Observable<any> {
    return this.http.post(`${BASE}/permission-requests/cancel`, null, {
      params: { requestId }
    });
  }

  // Roles
  getUserRoles(employeeCode: string): Observable<any> {
    return this.http.get(`${BASE}/users/roles`, { params: { employeeCode } });
  }

  assignRole(employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/roles`, body, { params: { employeeCode } });
  }

  revokeRole(employeeCode: string, roleCode: string): Observable<any> {
    return this.http.post(`${BASE}/users/roles/revoke`, null, {
      params: { employeeCode, roleCode }
    });
  }

  // User detail & update
  getUserDetail(userId: number, employeeCode: string): Observable<any> {
    return this.http.get(`${BASE}/users/detail`, { params: { userId, employeeCode } });
  }

  updateUserProfile(employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/profile`, body, { params: { employeeCode } });
  }

  updateUserPersonal(employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/personal`, body, { params: { employeeCode } });
  }

  // Credentials
  changePassword(body: any): Observable<any> {
    return this.http.post(`${BASE}/users/change-password`, body);
  }

  resetPassword(body: any): Observable<any> {
    return this.http.post(`${BASE}/users/reset-password`, body);
  }

  // Permission revoke
  revokeAppPermission(employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/${employeeCode}/app-permissions/revoke`, body);
  }

  revokeResourcePermission(employeeCode: string, body: any): Observable<any> {
    return this.http.post(`${BASE}/users/resource-permissions/revoke`, body, { params: { employeeCode } });
  }

  // Location
  getProvinces(name?: string): Observable<any> {
    const params: any = {};
    if (name) params.name = name;
    return this.http.get(`${BASE}/users/provinces`, { params });
  }

  getWards(provinceCode: number, name?: string): Observable<any> {
    const params: any = { provinceCode };
    if (name) params.name = name;
    return this.http.get(`${BASE}/users/wards`, { params });
  }
}
