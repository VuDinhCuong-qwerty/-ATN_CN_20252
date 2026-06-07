# IAM Banking System v3 — Tài liệu Khám phá & Kế hoạch

> Tài liệu này tổng hợp toàn bộ thông tin được khám phá từ codebase. Dùng làm nền tảng để viết tài liệu kỹ thuật chi tiết.

---

## 1. CẤU TRÚC DỰ ÁN

### 1.1 Tổng quan kiến trúc

```
IAM Banking System v3
├── iam-auth-service/          # Spring Boot 3.5, port 8888 — OAuth2 AS + OIDC IdP
├── iam-identity-service/      # Spring Boot 3.2, port 8081 — User/Role/Permission CRUD
├── iam-app-service/           # Spring Boot 3.2, port 8082 — App/Client/Flow config
├── iam-notify-service/        # Spring Boot 3.2, port 8083 — Kafka consumer + Email
├── iam-gateway/               # Spring Cloud Gateway, port 8080 — API Gateway
├── iam-web-service/           # Angular 17, port 4200 — Admin portal UI
├── ldap-server/               # Spring Boot + ApacheDS, port 10389 — LDAP gateway
├── job-schedule/              # Spring Boot — Cron jobs (region sync)
├── demo-change-app/           # Spring Boot 3.2 (8085) + Angular 17 (4201) — Demo app
└── iam-cluster-docker/        # Docker Compose infra (Kafka, Redis, ELK, GitLab)
```

### 1.2 Stack công nghệ

| Lớp | Công nghệ |
|---|---|
| Ngôn ngữ | Java 21 (backend), TypeScript (frontend) |
| Framework backend | Spring Boot 3.2.5 / 3.5.13 |
| Framework frontend | Angular 17 (standalone components) |
| Database | Oracle 21c PDB (`AUTHPDB1`, schema `auth_user1`) |
| Cache | Redis 7.2 |
| Message broker | Apache Kafka 3 (KRaft, 3 broker, SASL/PLAIN) |
| Giao thức xác thực | OAuth2 + OIDC (Authorization Code + PKCE, Client Credentials, Refresh Token) |
| Ký token | ES256 (ECDSA P-256) |
| Email | Gmail SMTP + Thymeleaf HTML templates |
| LDAP | ApacheDS embedded |
| Logging | Fluent Bit → Elasticsearch + Kibana |
| Container | Docker + Docker Compose |
| Build | Maven (backend), Angular CLI (frontend) |

### 1.3 Mô tả các service

| Service | Port | Công nghệ | Vai trò |
|---|---|---|---|
| **iam-auth-service** | 8888 | Spring Boot 3.5 | OAuth2 Authorization Server, OIDC IdP, MFA engine |
| **iam-identity-service** | 8081 | Spring Boot 3.2 | Quản lý user, lifecycle, credential, phân quyền |
| **iam-app-service** | 8082 | Spring Boot 3.2 | Cấu hình app, OAuth2 client, auth flow, default permission |
| **iam-notify-service** | 8083 | Spring Boot 3.2 | Kafka consumer + gửi email thông báo |
| **iam-gateway** | 8080 | Spring Cloud Gateway | API gateway: JWT validate, permission check, token exchange |
| **iam-web-service** | 4200 | Angular 17 | Admin portal: quản lý IAM toàn hệ thống |
| **ldap-server** | 10389 | Spring Boot + ApacheDS | LDAP gateway → Oracle (GitLab, Kibana, Jira...) |
| **job-schedule** | — | Spring Boot | Cron job: sync tỉnh/xã từ region service (daily 00:00) |
| **demo-change-app** | 8085/4201 | Spring Boot + Angular | Demo app nội bộ: Change & Go-Live Management |

### 1.4 Cấu trúc package chuẩn (backend)

```
com.iam.{service}/
├── controller/        # REST controllers (4–9 controllers/service)
├── service/           # Business logic (interface + impl)
├── domain/            # JPA entities (30+ entities/service)
├── dto/
│   ├── request/       # Request DTOs (20+ per service)
│   ├── response/      # Response DTOs (30+ per service)
│   └── pojo/          # Internal POJOs (stored proc results...)
├── repository/
│   ├── jpa/           # JPA repositories + native SQL
│   └── cache/         # Caffeine in-memory cache
├── kafka/
│   ├── event/         # BaseEvent<T> wrapper
│   ├── payload/       # Event payload POJOs
│   ├── producer/      # KafkaTemplate publishers
│   └── consumer/      # @KafkaListener handlers
├── config/            # SecurityConfig, KafkaConfig, RedisConfig
├── exception/         # BusinessException, GlobalExceptionHandler
└── utils/             # Helper utilities
```

### 1.5 Cơ sở dữ liệu — Oracle schema `auth_user1`

**Nhóm User:**

| Bảng | Mô tả |
|---|---|
| `AUTH_USER` | Thông tin đăng nhập (username, email, password BCrypt, status: ACTIVE/INACTIVE/DELETED) |
| `AUTH_USER_PROFILE` | Hồ sơ cá nhân (fullName, employeeCode, positionCode, departmentId, emailPersonal, avatarUrl) |
| `AUTH_USER_ADDRESS` | Địa chỉ (type: PERMANENT/TEMPORARY/BIRTH, unique per user per type) |
| `AUTH_POSITION` | 6 vị trí: IT_L1, IT_L2, SYSADMIN, PAYMENT_OPS, IT_MANAGER, CISO |
| `AUTH_DEPARTMENT` | Cây phòng ban (hierarchical, parentId, status) |
| `AUTH_PROVINCE` | Tỉnh/thành phố (code PK, name, divisionType) |
| `AUTH_WARD` | Xã/phường (code PK, provinceCode FK, name, divisionType) |

**Nhóm Auth & Permission:**

| Bảng | Mô tả |
|---|---|
| `AUTH_ROLE` | 3 role cố định: STAFF, CAB, ADMIN |
| `AUTH_USER_ROLE` | Gán role cho user (status: ACTIVE/REVOKED) |
| `AUTH_APPLICATION` | Đăng ký ứng dụng (serviceCode unique, type: INTERNAL/THIRD_PARTY_LDAP) |
| `AUTH_RESOURCE` | Tài nguyên trong app (resourceCode, actions CSV, status) |
| `AUTH_APP_PERMISSION` | User → App access (status: ACTIVE/REVOKED, grantSource: REQUEST/SYSTEM) |
| `AUTH_USER_RESOURCE` | User → Resource → Actions (status: ACTIVE/REVOKED, grantSource) |
| `AUTH_DEFAULT_APP_PERMISSION` | Role × Position → App (cấu hình quyền mặc định) |
| `AUTH_DEFAULT_RESOURCE` | Role × Position → Resource + Actions |

**Nhóm OAuth2 & Token:**

| Bảng | Mô tả |
|---|---|
| `AUTH_CLIENT` | OAuth2 client (clientId String, clientSecret nullable, type: public/confidential) |
| `AUTH_CLIENT_OAUTH` | Config OAuth2 chi tiết (grantTypes, redirectUris, TTLs, PKCE required) |
| `AUTH_CLIENT_METHOD` | Phương thức auth per client (methodId FK, config JSON) |
| `AUTH_METHOD` | Catalog phương thức hệ thống (USERNAME_PASSWORD, OTP_EMAIL, TOTP...) |

**Nhóm Auth Flow & Session:**

| Bảng | Mô tả |
|---|---|
| `AUTH_FLOW` | Định nghĩa luồng auth (alias, appId, status, isBuildIn) |
| `AUTH_FLOW_EXECUTION` | Các node trong flow (requirement, isDefault, parentNodeId, methodId) |
| `AUTH_USER_SESSION` | SSO session (userId, status, acrLevel, expiresAt, ipAddress, userAgent) |
| `AUTH_CLIENT_SESSION` | Session theo client |
| `AUTH_REFRESH_TOKEN` | Refresh token opaque (token, userId, clientId, status, expiresAt) |
| `AUTH_SIGNING_KEY` | JWT signing keys (kid UUID, PEM private/public, status: ACTIVE/PASSIVE/DISABLED) |

**Nhóm CAB Permission Request:**

| Bảng | Mô tả |
|---|---|
| `AUTH_REQUEST_HEADER` | Yêu cầu phân quyền (requested_by, reviewed_by employeeCode, status: DRAFT/OFFICIAL/APPROVED/REJECTED/CANCELLED) |
| `AUTH_REQUEST_DETAIL` | Chi tiết yêu cầu (app_id, resource_id, actions CSV, status: ACTIVE/INACTIVE) |

**Demo Change App schema `change_user1`:**

| Bảng | Mô tả |
|---|---|
| `CHG_CHANGE_REQUEST` | Bản ghi change (status: DRAFT/PENDING/APPROVED/EXECUTING/SUCCESS/FAIL) |
| `CHG_GOLIVE_JOB` | Tasks tuần tự (MERGE → BUILD → DEPLOY) |
| `CHG_CHECKLIST_ITEM` | Pre/during/rollback checklist (status per item) |
| `CHG_TEAM_MEMBER` | Thành viên team change |
| `CHG_APPROVER` | CAB approval tracking |

---

## 2. CÁC TÍNH NĂNG CHÍNH

### 2.1 OAuth2 Authorization Server & OIDC (iam-auth-service)

| Tính năng | Mô tả |
|---|---|
| **Authorization Code + PKCE** | Luồng chính cho Angular apps (public client) |
| **Client Credentials Grant** | Service-to-service: gateway lấy service token |
| **Refresh Token Grant** | Tự động gia hạn, hỗ trợ scope downgrade |
| **OIDC** | discovery endpoint, userinfo, id_token với nonce |
| **JWKS** | Phục vụ public key cho resource servers verify JWT |
| **Token Revocation** (RFC 7009) | Blacklist JTI trong Redis |
| **Token Introspection** (RFC 7662) | Kiểm tra token hợp lệ |
| **MFA Engine** | Cây FlowNode pluggable, Authenticator interface |
| **Method Switching** | Chuyển MFA method giữa chừng (sibling nodes) |
| **SSO Session** | SSO_SESSION cookie httpOnly, reuse cross-client |
| **Signing Key Rotation** | Daily cron ES256 (ECDSA P-256), PASSIVE keys giữ 120 ngày |

**Các endpoint chính (base: `/ms-internal-iam/auth`):**

| Method | Path | Mô tả |
|---|---|---|
| GET | `/authorize` | Authorization endpoint (PKCE, SSO check) |
| POST | `/token` | Token endpoint (authorization_code, client_credentials, refresh_token) |
| POST | `/token/revoke` | Revoke token (RFC 7009) |
| POST | `/token/introspect` | Introspect token (RFC 7662) |
| GET | `/userinfo` | OIDC UserInfo |
| GET | `/jwks` | JWK Set (ACTIVE + PASSIVE keys) |
| GET | `/.well-known/openid-configuration` | OIDC Discovery |
| POST | `/login` | Submit MFA step |
| POST | `/switch-method` | Khởi tạo chuyển MFA method |
| POST | `/switch-method/confirm` | Xác nhận chuyển method |
| POST | `/logout` | Logout + revoke SSO session |

### 2.2 Quản lý User (iam-identity-service)

**Base path: `/iam-identity-service`**

| Nhóm | Tính năng | Endpoint |
|---|---|---|
| **User CRUD** | Tạo user (tự sinh username/password) | POST `/users` |
| | Danh sách (5 filter + pagination) | GET `/users` |
| | Chi tiết user | GET `/users/detail` |
| | Cập nhật profile (ADMIN) | POST `/users/profile` |
| | Cập nhật thông tin cá nhân (self) | POST `/users/personal` |
| **Địa chỉ** | Xem địa chỉ (PERMANENT/TEMPORARY/BIRTH) | GET `/users/addresses` |
| | Upsert địa chỉ + autocomplete tỉnh/xã | POST `/users/addresses` |
| | Tra cứu tỉnh | GET `/users/provinces?name=` |
| | Tra cứu xã | GET `/users/wards?provinceCode=&name=` |
| **Lifecycle** | Nghỉ phép (Leave) | POST `/users/leave` |
| | Trở lại (Return) | POST `/users/return` |
| | Thôi việc (Offboard) | POST `/users/offboard` |
| | Tiếp nhận lại (Onboard) | POST `/users/onboard` |
| | Luân chuyển (Transfer) | POST `/users/transfer` |
| **Credential** | Đổi mật khẩu (self) | POST `/users/change-password` |
| | Reset mật khẩu (ADMIN) | POST `/users/reset-password` |
| **Role** | Xem roles | GET `/users/roles` |
| | Gán role | POST `/users/roles` |
| | Thu hồi role | POST `/users/roles/revoke` |
| **App Permission** | Xem quyền ứng dụng | GET `/users/{empCode}/app-permissions` |
| | Thu hồi quyền ứng dụng | POST `/users/{empCode}/app-permissions/revoke` |
| **Resource Permission** | Xem quyền tài nguyên | GET `/users/{empCode}/resource-permissions` |
| | Thu hồi quyền tài nguyên | POST `/users/resource-permissions/revoke` |
| **CAB Workflow** | Tạo yêu cầu | POST `/permission-requests` |
| | Gửi chính thức | POST `/permission-requests/submit` |
| | Cập nhật draft | POST `/permission-requests/update` |
| | Xem danh sách | GET `/permission-requests` |
| | Xem chi tiết | GET `/permission-requests/detail` |
| | Duyệt | POST `/permission-requests/approve` |
| | Từ chối | POST `/permission-requests/reject` |
| | Hủy | POST `/permission-requests/cancel` |

### 2.3 Cấu hình Ứng dụng (iam-app-service)

**Base path: `/iam-app-service`**

| Nhóm | Tính năng | Mô tả |
|---|---|---|
| **Reference** | Roles, Departments (cây), Positions | Lookup data cho dropdown |
| **Application** | CRUD + toggle status | serviceCode unique, type INTERNAL/THIRD_PARTY_LDAP |
| **Resource** | Batch create, inline edit, actions array | Soft-delete, actions là List<String> |
| **OAuth2 Client** | CRUD, reset secret, manage scopes | Plaintext secret 1-time reveal khi tạo |
| **Client Method** | Batch configure per app | Gán phương thức auth cho app |
| **Auth Flow** | Tạo cây auth flow (BFS, cycle detection) | Update → invalidate Caffeine cache |
| **Default Permission** | Role × Position → App/Resource | Batch create, pill toggle ON/OFF, preview |

### 2.4 Email Notification (iam-notify-service)

| Sự kiện | Template | Gửi đến |
|---|---|---|
| User mới được tạo | `user-created.html` | emailPersonal (JPA lookup) |
| Đổi mật khẩu | `password-changed.html` | email công việc (JdbcTemplate lookup) |
| Admin reset mật khẩu | `password-reset.html` | email công việc |
| Submit permission request | `permission-request.html` (reviewer) + `permission-submitted.html` (requester) | CAB + requester |
| CAB duyệt/từ chối | `permission-approved.html` | requester |

Tất cả email đều CC `vudinhcuong8404@gmail.com` (audit trail). Inline logo (CID).

### 2.5 API Gateway (iam-gateway)

Filter chain theo thứ tự:

| Order | Filter | Mô tả |
|---|---|---|
| 0 (per-route) | PermissionCheckFilterFactory | Parse JWT `permissions` claim → check required permission → 403 nếu thiếu |
| 1 (global) | UserContextFilter | Extract JWT claims → inject X-User-Id, X-Employee-Code, X-Username, X-User-Role |
| 2 (global) | TokenExchangeFilter | Thay user JWT bằng service token (client_credentials) trước khi forward |

### 2.6 Admin Portal Angular (iam-web-service)

19 routes với AuthGuard. Các màn hình chính:

| Route | Tính năng |
|---|---|
| `/users` | Danh sách user (5 filter) + FAB tạo mới (ADMIN) |
| `/users/detail` | Chi tiết + inline edit + selfProfile mode |
| `/users/create` | Full-page form tạo user mới |
| `/users/lifecycle` | 4 tab: Tiếp nhận / Thôi việc / Nghỉ phép / Luân chuyển |
| `/users/permissions` | 3 tab: Danh sách quyền / Yêu cầu / Duyệt (CAB) |
| `/users/roles` | Gán/thu hồi role theo empCode |
| `/users/credentials` | Reset password |
| `/config/apps` | CRUD Applications |
| `/config/resources` | CRUD Resources per app |
| `/config/clients` | CRUD OAuth2 Clients (detail 3-tab) |
| `/config/flows` | Auth Flow tree view + tạo mới |
| `/config/default-perms` | Default Permission (pill toggle App tab, timestamps Resource tab) |

### 2.7 LDAP Gateway

- Service-encoded DN: `uid=USER,ou={serviceCode},ou=users,dc=iam,dc=bank,dc=vn`
- 1 instance phục vụ N ứng dụng (GitLab, Kibana, Jira...)
- BIND: BCrypt verify password + check `AUTH_APP_PERMISSION`
- SEARCH: Trả LDAP entries từ Oracle (uid, cn, mail, memberOf, description)
- Groups: `cn={serviceCode},ou=groups,...` cho Kibana role mapping

### 2.8 Demo Change & Go-Live Management

- Change Request: DRAFT → PENDING → APPROVED → EXECUTING → SUCCESS/FAIL
- CAB unanimous approval (tất cả CAB phải approve)
- Go-Live Jobs: MERGE → BUILD → DEPLOY sequential
- Checklist pre/during/rollback
- PKCE OAuth2 với iam-auth-service, permissions: `change-mgmt/change-request:{action}`

---

## 3. CÁC LUỒNG NGHIỆP VỤ

### 3.1 Đăng nhập OAuth2 PKCE (từ Angular)

```
[Browser — Angular]
1. User click "Đăng nhập"
2. AuthService.login():
   - Generate codeVerifier (64 bytes random) + state (32 bytes)
   - codeChallenge = Base64url(SHA-256(codeVerifier))
   - Lưu verifier + state vào sessionStorage
3. Redirect → GET /ms-internal-iam/auth/authorize
   ?client_id=iam-demo-portal
   &response_type=code
   &redirect_uri=http://localhost:4200/callback
   &code_challenge=<hash>
   &code_challenge_method=S256
   &scope=openid profile
   &state=<random>

[iam-auth-service]
4. Validate: client tồn tại, redirect_uri khớp, code_challenge hợp lệ
5. Check SSO_SESSION cookie → nếu ACTIVE → skip login → bước 9
6. Load auth flow từ Caffeine cache (TTL 24h) hoặc Oracle
7. Render Thymeleaf login page (root node: USERNAME_PASSWORD)
8. Tạo sessionToken = HMAC-SHA256(SECRET, authSessionId:jsessionId:timestamp)
   Lưu vào HttpSession (single-use)

[Browser — Thymeleaf login page]
9. User nhập username + password
10. POST /login {sessionToken, type:"USERNAME_PASSWORD", payload:{username, password}}

[iam-auth-service]
11. Validate + invalidate sessionToken (single-use, replay-proof)
12. UserNameAuthenticator.validate():
    - Tìm user theo username
    - BCrypt.verify(password, storedHash)
    - Check status = ACTIVE
13. Nếu flow có node tiếp theo (OTP) → render OTP page + sessionToken mới
14. Nếu leaf node → generate authorization code:
    - Random code (opaque)
    - Lưu Redis: auth:code:<code> → {clientId, userId, redirectUri, scopes, codeChallenge, nonce} (TTL: 60s)
15. 302 redirect → http://localhost:4200/callback?code=<code>&state=<state>

[Browser — Angular /callback]
16. AuthService.exchangeCode()
    POST /token (application/x-www-form-urlencoded):
      grant_type=authorization_code
      code=<code>
      client_id=iam-demo-portal
      code_verifier=<stored verifier>
      redirect_uri=http://localhost:4200/callback

[iam-auth-service — 8-step validation]
17. validateAuthCode: Lua script Redis → atomic read-then-delete (1-time use)
18. crossCheck: redirect_uri exact match, scopes subset
19. validatePKCE: SHA-256(verifier) === stored challenge
20. validateUser: AUTH_USER.STATUS = ACTIVE, có AUTH_APP_PERMISSION cho app
21. generateAccessToken (TokenForUser):
    - ES256 JWT, TTL 1h
    - Claims: {jti, iss, sub:userId, aud:clientId, iat, exp, username, email,
               displayName, appId, serviceCode, clientId, role, permissions:[...]}
22. generateRefreshToken: opaque token, lưu Oracle AUTH_REFRESH_TOKEN
23. generateIdToken: OIDC JWT nếu scope includes "openid"
24. Response: {access_token, refresh_token, id_token, expires_in, token_type:"Bearer"}

[Browser — Angular]
25. TokenStoreService.setToken() + setRefreshToken() + setUserInfo()
26. Navigate → /users
```

### 3.2 MFA — Multi-Factor Authentication

```
Auth flow tree (tải từ AUTH_FLOW + AUTH_FLOW_EXECUTION):
  root: USERNAME_PASSWORD (REQUIRED)
    ├── OTP_EMAIL  (REQUIRED, isDefault=true)
    └── OTP_SMS    (ALTERNATIVE)

Bước 1 — Username/Password:
  UserNameAuthenticator.validate()
  → BCrypt verify + check ACTIVE
  → nodeStatus[USERNAME_PASSWORD] = SUCCESS
  → Có children → advance

Bước 2 — OTP_EMAIL (isDefault):
  OtpAuthenticator.prepare() → gửi OTP (hiện là stub: "12345678")
  → Render otp-email.html + sessionToken mới
  → User nhập OTP → POST /login {type:"OTP_EMAIL", payload:{otp:"12345678"}}
  → OtpAuthenticator.validate() → SUCCESS
  → Leaf node → generate authorization code

Method Switching (chuyển sang OTP_SMS):
  POST /switch-method {sessionToken}
  → Validate token, render method selection page
  POST /switch-method/confirm {nodeId: OTP_SMS_NODE_ID, sessionToken}
  → Advance to OTP_SMS node, render SMS page
```

### 3.3 API Request qua Gateway

```
[Angular]
  POST /api/identity/users/leave
  Authorization: Bearer <access_token>

[iam-gateway — filter chain theo order]
  Order 0 — PermissionCheckFilterFactory:
    → Extract JWT (parse claims, NO DB call)
    → Check: permissions claim chứa "iam-service/user-lifecycle:leave"?
    → Nếu không → 403 FORBIDDEN {"error":"insufficient_scope"}

  Order 1 — UserContextFilter:
    → Verify JWT ES256 (JWKS cache từ iam-auth-service)
    → Inject headers:
       X-User-Id: <userId>
       X-Employee-Code: <employeeCode>
       X-Username: <username>
       X-User-Role: ADMIN

  Order 2 — TokenExchangeFilter:
    → POST /ms-internal-iam/auth/token
       {grant_type:client_credentials, client_id:iam-gateway, client_secret:...}
    → Nhận TokenForService (không có role/permissions)
    → Thay Authorization header bằng service token

[iam-identity-service — port 8081]
  → SecurityConfig: oauth2ResourceServer JWT validate (iss, exp, sig)
  → RequestContextFilter: X-headers → ThreadLocal RequestContext
  → UserLifecycleController.leave()
  → UserLifecycleServiceImpl.leave() → Oracle UPDATE + Kafka publish
  → Return ApiResponse<UserStatusResponse>
```

### 3.4 Token Refresh Tự động (Angular Interceptor)

```
[auth.interceptor.ts]
  Mọi request /api/* → attach "Authorization: Bearer <token>"
  Response 401 và URL không phải /auth/token:
    → authService.refreshToken()
       POST /api/auth/token
         grant_type=refresh_token
         refresh_token=<stored>
         client_id=iam-demo-portal
    → Thành công: lưu access_token mới, retry request gốc
    → Thất bại: authService.logout() → clear store → navigate /login
```

### 3.5 Tạo User Mới

```
[ADMIN — Angular /users/create]
  Fill form: fullName, dob, email, phone, positionCode, departmentId,
             roleIds[] (multi-select chip), địa chỉ 3 loại (autocomplete tỉnh/xã)
  POST /api/identity/users

[iam-identity-service — UserInfoServiceImpl.createUser()]
  1. CheckInfor.checkDuplicatedDataUser() — username/email unique
  2. GenDataService.genUsername(fullName):
     - Chuyển fullName → lowercase latin (bỏ dấu)
     - Format: <tên><chữ cái đầu họ+đệm> + số nếu trùng
     - VD: "Vũ Đình Cường" → "cuongvd"
  3. GenDataService.genPassword() — random 8+ chars
  4. BCryptPasswordEncoder.encode(password)
  5. @Transactional (SavedService.createUser()):
     - INSERT AUTH_USER (username, email, password, status=ACTIVE)
     - INSERT AUTH_USER_PROFILE (employeeCode, fullName, positionCode, departmentId)
     - INSERT AUTH_USER_ADDRESS (nếu có địa chỉ)
     - INSERT AUTH_USER_ROLE (mỗi roleId trong danh sách)

  Kafka publish (ngoài @Transactional, wrapped try-catch):
  6. Topic CREATE-SUCCESS-USER-NOTIFY → payload:
     {userId, fullName, username, email, tempPassword, joinDate}
  7. Topic DEFAULT-GRANT-PERMISSION-USER → payload:
     {userId, roles:[...roleIds], positionCode, departmentId}

[iam-identity-service — GrantPermissionConsumer]
  8. @KafkaListener DEFAULT-GRANT-PERMISSION-USER (manual ack):
     - Lookup AUTH_DEFAULT_APP_PERMISSION (role × position → apps)
     - INSERT AUTH_APP_PERMISSION cho mỗi app (grantSource=SYSTEM)
     - Lookup AUTH_DEFAULT_RESOURCE (role × position → resources)
     - INSERT AUTH_USER_RESOURCE cho mỗi resource (grantSource=SYSTEM)
     - ack.acknowledge()

[iam-notify-service — NotifyConsumer]
  9. @KafkaListener CREATE-SUCCESS-USER-NOTIFY:
     - JPA lookup emailPersonal từ AUTH_USER_PROFILE (by userId)
     - EmailService.sendUserCreatedEmail()
       → Thymeleaf render user-created.html
       → JavaMailSender.send() → Gmail SMTP
       → CC: vudinhcuong8404@gmail.com
     - ack.acknowledge()
```

### 3.6 CAB Approval — Permission Request

```
[STAFF — Angular /users/permissions/create]
  1. Điền form: reviewer (CAB empCode), lý do, apps + resources cần xin
  2. Lưu nháp → POST /permission-requests
     {type:DRAFT, requesterId, requester, requesterCode, reviewer, reviewerCode, reason, apps, resources}
     → INSERT AUTH_REQUEST_HEADER (status=DRAFT)
     → INSERT AUTH_REQUEST_DETAIL (app/resource per item)
  3. Gửi → POST /permission-requests/submit {requestId}
     → UPDATE AUTH_REQUEST_HEADER status = OFFICIAL
     → Kafka: REQUEST-PERMISSION-NOTIFY
       payload: {requestId, requesterCode, reviewerCode, reason, requestedAt}

[iam-notify-service]
  4. Email CAB reviewer (permission-request.html) + requester (permission-submitted.html)

[CAB — Angular /users/permissions Tab "Duyệt yêu cầu"]
  5. Xem request (filter reviewed_by = current CAB)
  6a. Approve → POST /permission-requests/approve {requestId, note}
      → UPDATE AUTH_REQUEST_HEADER status = APPROVED
      → Đọc AUTH_REQUEST_DETAIL → INSERT AUTH_APP_PERMISSION (grantSource='request')
      → INSERT AUTH_USER_RESOURCE (grantSource='request')
      → Kafka: APPROVE-PERMISSION-NOTIFY (action=APPROVED)
  6b. Reject → POST /permission-requests/reject {requestId, note}
      → UPDATE status = REJECTED
      → Kafka: APPROVE-PERMISSION-NOTIFY (action=REJECTED)

[iam-notify-service]
  7. Email requester (permission-approved.html — kết quả APPROVED hoặc REJECTED)
```

### 3.7 Thu hồi Quyền (Revoke Permission)

```
[ADMIN — Angular]
  POST /users/{empCode}/app-permissions/revoke
  {appIds: [1, 2], reason: "..."}

[iam-identity-service]
  1. UPDATE AUTH_APP_PERMISSION status=REVOKED WHERE appId IN (...)
     (nếu revoke theo role cascade: bỏ qua grantSource='request')
  2. UPDATE AUTH_USER_RESOURCE status=REVOKED WHERE app_id IN (...)
  3. Kafka: REVOKED-PERMISSION-NOTIFY
     {userId, employeeCode, revokedAppIds, eventType: REVOKED_APP_PERMISSION}

[iam-auth-service — PermissionRevokedConsumer]
  4. Revoke AUTH_CLIENT_SESSION WHERE appId IN (revokedAppIds)
  5. Revoke AUTH_REFRESH_TOKEN WHERE userId = ? AND appId IN (...)
  6. User nhận 401 trên request tiếp theo → interceptor gọi refresh → refresh fail → logout
  7. Re-login → permissions claim mới (không còn quyền bị revoke)
```

### 3.8 User Lifecycle

```
OFFBOARD (Thôi việc):
  POST /users/offboard?userId&employeeCode
  1. UPDATE AUTH_USER status = INACTIVE
  2. UPDATE AUTH_USER_ROLE status = REVOKED (tất cả roles)
  3. UPDATE AUTH_APP_PERMISSION: ghi inactiveFromDate = now
     (soft — dữ liệu lịch sử còn, không hard delete)
  4. UPDATE AUTH_USER_RESOURCE: ghi inactiveFromDate = now

ONBOARD (Tiếp nhận lại sau thôi việc):
  POST /users/onboard {roleIds:[], positionCode, departmentId, joinDate}
  1. UPDATE AUTH_USER status = ACTIVE
  2. INSERT AUTH_USER_ROLE (roleIds mới, multi-select)
  3. UPDATE AUTH_APP_PERMISSION: clear inactiveDates (nếu có perm cũ)
  4. Kafka: DEFAULT-GRANT-PERMISSION-USER → grant default perms cho roles mới

LEAVE (Nghỉ phép):
  POST /users/leave {fromDate, toDate}
  UPDATE AUTH_USER_PROFILE onLeave = true (flag)
  (user vẫn ACTIVE, chỉ đánh dấu trạng thái)

RETURN (Trở lại sau nghỉ):
  POST /users/return
  UPDATE AUTH_USER_PROFILE onLeave = false

TRANSFER (Luân chuyển):
  POST /users/transfer {roleIds:[], positionCode mới, departmentId mới}
  1. REVOKE tất cả roles cũ
  2. INSERT roles mới
  3. UPDATE AUTH_USER_PROFILE positionCode, departmentId
  4. Kafka: REVOKED-PERMISSION-NOTIFY → revoke perms cũ → invalidate sessions
  5. Kafka: DEFAULT-GRANT-PERMISSION-USER → grant default perms theo roles+position mới
```

### 3.9 LDAP Authentication (GitLab / Kibana)

```
[GitLab CE — LDAP BIND]
  BIND DN = uid=CUONGVD,ou=gitlab-server,ou=users,dc=iam,dc=bank,dc=vn
  password = <plaintext>

[ldap-server — OracleAuthenticator]
  1. Extract username từ DN RDN: "CUONGVD"
  2. Extract serviceCode từ DN path: "gitlab-server"
  3. JDBC: SELECT * FROM AUTH_USER WHERE USERNAME = 'CUONGVD'
  4. Check STATUS = 'ACTIVE'
  5. BCryptPasswordEncoder.matches(inputPassword, storedHash)
  6. Check AUTH_APP_PERMISSION: userId có ACTIVE permission cho serviceCode?
  7. Success → LDAP Principal | Fail → InvalidCredentialsException

[GitLab CE — LDAP SEARCH (user listing)]
  SEARCH base = ou=gitlab-server,ou=users,dc=iam,dc=bank,dc=vn
  → OraclePartition.search()
  → UserService.findAllByService("gitlab-server")
  → JDBC: AUTH_USER JOIN AUTH_APP_PERMISSION WHERE serviceCode='gitlab-server'
  → Convert to LDAP Entry (uid, cn, mail, mobile, displayName, memberOf, description)

[Kibana — LDAP Groups search]
  SEARCH base = ou=groups,dc=iam,dc=bank,dc=vn
  → ApplicationService.findAllActive()
  → Return: cn=gitlab-server, cn=kibana, cn=jira, ...
  → description attribute chứa resource permission strings (dùng cho Kibana role mapping)
```

### 3.10 Signing Key Rotation

```
Daily 12:00 — RotateKeyJob:
  1. KeyPairGenerator.getInstance("EC") → EC P-256 key pair
  2. INSERT AUTH_SIGNING_KEY (kid=UUID, algorithm=ES256,
     publicKey=PEM, privateKey=PEM, status=ACTIVE,
     validFrom=now, validUntil=now+120days)
  3. UPDATE old ACTIVE key → status = PASSIVE
     (vẫn dùng được để verify token cũ)

Daily 12:05 — CleanupKeyJob:
  4. SELECT WHERE status=PASSIVE AND validUntil < NOW()
  5. UPDATE status = DISABLED
  6. Evict từ Caffeine local cache (private key cache + public key cache)

JWKS endpoint (/jwks):
  → Query ACTIVE + PASSIVE keys → trả public key (PEM → JWKS EC format)
  → Resource servers cache JWKS, re-fetch khi gặp unknown kid trong JWT header
```

---

## 4. CÁC TÁC NHÂN (ACTORS)

### 4.1 Tác nhân người dùng (Human Actors)

| Tác nhân | Mô tả | Role | Quyền chính |
|---|---|---|---|
| **ADMIN** | Quản trị viên hệ thống | ADMIN | Toàn quyền: quản lý user, config app, phân quyền, lifecycle |
| **CAB** | Change Advisory Board — Hội đồng duyệt quyền | CAB | Duyệt/từ chối permission request; xem thông tin user |
| **STAFF** | Nhân viên thông thường | STAFF | Xem thông tin cá nhân; gửi yêu cầu phân quyền |
| **3rd Party Admin** | Admin hệ thống GitLab/Kibana | N/A (LDAP auth) | Xác thực qua LDAP với IAM credentials |

### 4.2 Tác nhân hệ thống (System Actors)

| Tác nhân | Service | Vai trò |
|---|---|---|
| **OAuth2 Client Public** | Angular (iam-web, demo-change) | PKCE authorization_code flow |
| **OAuth2 Client Confidential** | iam-gateway | client_credentials → service token |
| **LDAP Client** | GitLab CE, Kibana | BIND + SEARCH qua LDAP 10389 |
| **Kafka Consumer — notify** | iam-notify-service | Tiêu thụ event → gửi email |
| **Kafka Consumer — grant** | iam-identity-service | Tiêu thụ DEFAULT-GRANT → cấp quyền mặc định |
| **Kafka Consumer — auth** | iam-auth-service | Tiêu thụ REVOKE/PASSWORD/FLOW → invalidate cache |
| **Cron — Key Rotation** | iam-auth-service | RotateKeyJob + CleanupKeyJob (daily 12:00/12:05) |
| **Cron — Region Sync** | job-schedule | SyncRegionJob (daily 00:00 — sync tỉnh/xã) |

### 4.3 Ma trận Quyền (Role × Resource × Action)

| Resource | Action | STAFF | CAB | ADMIN |
|---|---|---|---|---|
| user | read | ✓ | ✓ | ✓ |
| user | create, update | — | — | ✓ |
| user-lifecycle | leave, return, onboard, offboard, transfer | — | — | ✓ |
| user-credential | reset-password | — | — | ✓ |
| user-permission | read | ✓ | ✓ | ✓ |
| user-permission | create (request) | ✓ | — | ✓ |
| user-permission | approve, reject | — | ✓ | ✓ |
| user-permission | revoke | — | — | ✓ |
| role | read, assign, revoke | — | — | ✓ |
| application | read, create, update | — | — | ✓ |
| client | read, create, update | — | — | ✓ |
| auth-flow | read, create | — | — | ✓ |
| default-permission | read, create | — | — | ✓ |

### 4.4 Vị trí (Position) và ý nghĩa

| Code | Tên | Role mặc định | Nhóm CAB |
|---|---|---|---|
| IT_L1 | Kỹ thuật viên cấp 1 | STAFF | Không |
| IT_L2 | Kỹ thuật viên cấp 2 | STAFF | Không |
| SYSADMIN | Quản trị hệ thống | STAFF | Không |
| PAYMENT_OPS | Vận hành thanh toán | STAFF | Không |
| IT_MANAGER | Trưởng phòng IT | CAB | Có |
| CISO | Giám đốc An ninh thông tin | CAB | Có |

### 4.5 Permission Format

```
{serviceCode}/{resourceCode}:{action}

Ví dụ:
  iam-service/user:read
  iam-service/user-lifecycle:onboard
  iam-service/application:create
  change-mgmt/change-request:approve
  log-app-service/iam-system-logs:edit

Lưu trong JWT claims:
  "permissions": [
    "iam-service/user:read",
    "iam-service/user:create",
    "iam-service/user-lifecycle:leave",
    ...
  ]
```

---

## 5. KAFKA EVENT MAP

| Topic | Publisher | Consumer(s) | Trigger | Payload key fields |
|---|---|---|---|---|
| `CREATE-SUCCESS-USER-NOTIFY` | iam-identity | iam-notify | Tạo user mới | userId, username, email, tempPassword |
| `DEFAULT-GRANT-PERMISSION-USER` | iam-identity | iam-identity (GrantPermission) | Tạo / Onboard / Transfer / Assign role | userId, roles, positionCode, departmentId |
| `USER-CHANGED-PASSWORD` | iam-identity | iam-auth (revoke), iam-notify (email) | Đổi/Reset mật khẩu | userId, email, eventType: CHANGE\|RESET |
| `REQUEST-PERMISSION-NOTIFY` | iam-identity | iam-notify | Submit OFFICIAL | requestId, requesterCode, reviewerCode |
| `APPROVE-PERMISSION-NOTIFY` | iam-identity | iam-notify | CAB duyệt/từ chối | requestId, requestedBy, reviewedBy, action |
| `REVOKED-PERMISSION-NOTIFY` | iam-identity | iam-auth (revoke token), iam-notify | Thu hồi app/resource perm | userId, revokedAppIds, eventType |
| `CLIENT-SECRET-RESET-NOTIFY` | iam-app | iam-notify | Reset OAuth2 client secret | clientId, clientName, resetBy |
| `FLOW-EXECUTION-UPDATED` | iam-app | iam-auth (invalidate Caffeine cache) | Cập nhật auth flow | appId, flowId, alias |

---

## 6. KẾ HOẠCH TÀI LIỆU CHI TIẾT (Replan — 2026-06-07)

### Trọng tâm đồ án

**Đề tài:** Xây dựng hệ thống phân quyền cho nhân viên IT ngân hàng

**Bài toán cốt lõi:** Trong một ngân hàng, nhân viên IT có nhiều vị trí khác nhau (IT_L1, IT_L2, SYSADMIN, PAYMENT_OPS, IT_MANAGER, CISO). Mỗi vị trí cần truy cập vào tập hợp hệ thống khác nhau với mức độ khác nhau. Khi nhân viên thay đổi vị trí, nghỉ việc, hoặc luân chuyển — quyền truy cập phải thay đổi theo tự động và an toàn.

**Trái tim của hệ thống:**
- `iam-auth-service` — Xác thực (OAuth2/OIDC) + phát hành JWT mang claims phân quyền
- `iam-identity-service` — Quản lý vòng đời nhân viên + các luồng phân quyền
- `iam-app-service` — Cấu hình hệ thống (app, tài nguyên, quyền mặc định)
- `iam-web-service` — Giao diện quản trị tập trung

**3 app demo minh họa bài toán phân quyền:**

| App | Loại | Tích hợp | Mục đích minh họa |
|---|---|---|---|
| `demo-change-app` | App nội bộ | OAuth2 PKCE + JWT | Nhân viên dùng IAM token để truy cập, gateway kiểm tra quyền từng API |
| `GitLab CE` | App bên thứ 3 | LDAP (port 10389) | IAM đóng vai LDAP server — GitLab dùng IAM credentials + kiểm tra quyền truy cập |
| `Kibana + ES + Fluent Bit` | App bên thứ 3 (log) | LDAP + Kibana roles | Tương tự GitLab, kết hợp role mapping theo LDAP group + xem log hệ thống |

### Quy tắc viết mỗi luồng nghiệp vụ

Mỗi file luồng phải có đủ 3 phần:

1. **Tình huống (Scenario)** — Bối cảnh cụ thể, ai tham gia, tại sao luồng này được kích hoạt
2. **Trạng thái các đối tượng** — Bảng trạng thái trước/sau của từng entity (AUTH_USER, AUTH_USER_ROLE, AUTH_APP_PERMISSION, v.v.)
3. **Luồng theo thời gian** — Từng bước chi tiết: hệ thống nào làm gì, dữ liệu nào thay đổi, Kafka event nào được publish/consume

### Cấu trúc thư mục tài liệu

```
docs/
├── planning.md                           ← [✅ file này] Knowledge base từ codebase
│
├── 00-overview.md                        ← Tổng quan bài toán + kiến trúc tổng thể
├── 01-permission-model.md                ← Mô hình phân quyền (CORE) — đọc trước tiên
├── 02-actors-and-states.md              ← Tác nhân + bảng trạng thái đối tượng
│
│   ── PHẦN A: VÒNG ĐỜI NHÂN VIÊN (Employee Lifecycle) ──
├── 03-flow-new-employee.md              ← Luồng: Nhân viên mới + cấp quyền tự động
├── 04-flow-leave-return.md              ← Luồng: Nghỉ phép + Trở lại
├── 05-flow-offboard-onboard.md          ← Luồng: Thôi việc + Tiếp nhận lại
├── 06-flow-transfer.md                  ← Luồng: Luân chuyển vị trí
│
│   ── PHẦN B: QUẢN LÝ QUYỀN TƯỜNG MINH (Permission Management) ──
├── 07-flow-permission-request.md        ← Luồng: Yêu cầu quyền bổ sung (CAB approval)
├── 08-flow-permission-revoke.md         ← Luồng: Thu hồi quyền
├── 09-default-permission-config.md      ← Cơ chế quyền mặc định (role × position)
│
│   ── PHẦN C: KIỂM SOÁT TRUY CẬP (Access Control at Runtime) ──
├── 10-flow-login-token.md               ← Luồng: Đăng nhập → JWT mang claims quyền
├── 11-flow-gateway-permission-check.md  ← Luồng: Gateway kiểm tra quyền mỗi API call
│
│   ── PHẦN D: ỨNG DỤNG DEMO (Minh họa bài toán thực tế) ──
├── 12-app-change-management.md         ← App nội bộ: demo-change-app (OAuth2 PKCE)
├── 13-app-gitlab-ldap.md               ← App 3rd party: GitLab qua LDAP
├── 14-app-kibana-ldap.md               ← App 3rd party: Kibana log qua LDAP
│
│   ── PHẦN E: THAM CHIẾU KỸ THUẬT ──
├── 15-database-schema.md               ← Bảng DB + ERD + stored procedures
├── 16-api-reference.md                 ← REST endpoints đầy đủ
└── 17-system-config.md                 ← Hướng dẫn cấu hình app/resource/client/flow
```

### Thứ tự ưu tiên viết

| Ưu tiên | File | Lý do |
|---|---|---|
| 🔴 1 | `01-permission-model.md` | Nền tảng lý thuyết — đọc trước tất cả |
| 🔴 2 | `00-overview.md` | Bức tranh tổng thể cho người đọc |
| 🔴 3 | `02-actors-and-states.md` | Định nghĩa các tác nhân + trạng thái dùng trong mọi luồng |
| 🔴 4 | `03-flow-new-employee.md` | Luồng quan trọng nhất — kích hoạt toàn bộ chuỗi quyền |
| 🔴 5 | `07-flow-permission-request.md` | Luồng CAB approval — đặc thù ngân hàng |
| 🟡 6 | `05-flow-offboard-onboard.md` | Lifecycle quan trọng: thu hồi + cấp lại quyền |
| 🟡 7 | `06-flow-transfer.md` | Luân chuyển — thay đổi quyền phức tạp nhất |
| 🟡 8 | `10-flow-login-token.md` | Cơ chế xác thực + quyền trong token |
| 🟡 9 | `11-flow-gateway-permission-check.md` | Kiểm soát truy cập tại runtime |
| 🟡 10 | `12-app-change-management.md` | Demo app nội bộ |
| 🟡 11 | `13-app-gitlab-ldap.md` | Demo LDAP app |
| 🟢 12 | `04-flow-leave-return.md` | Luồng đơn giản hơn |
| 🟢 13 | `08-flow-permission-revoke.md` | Thu hồi quyền trực tiếp |
| 🟢 14 | `09-default-permission-config.md` | Config reference |
| 🟢 15 | `14-app-kibana-ldap.md` | Tương tự GitLab |
| 🟢 16 | `15-database-schema.md` | Reference kỹ thuật |
| 🟢 17 | `16-api-reference.md` | Reference đầy đủ |
| 🟢 18 | `17-system-config.md` | Config guide |

### Định dạng chuẩn cho mỗi file

```markdown
# [Tên luồng/chủ đề]

## 1. Tình huống (Scenario)
[Bối cảnh cụ thể: ai, khi nào, tại sao]

## 2. Tác nhân tham gia
[Bảng: Tác nhân | Vai trò | Hệ thống]

## 3. Trạng thái các đối tượng
[Bảng trước/sau: Entity | Trường | Trước | Sau]

## 4. Luồng theo thời gian
[Step-by-step: mỗi bước ghi rõ hệ thống, action, dữ liệu thay đổi]

## 5. Sơ đồ (Mermaid)
[Sequence diagram hoặc flowchart]

## 6. Ghi chú & Edge Cases
[Các trường hợp đặc biệt, ràng buộc nghiệp vụ]
```

---

*Khám phá codebase thực hiện: 2026-06-07. Tổng số service đọc: 9.*
*Replan: 2026-06-07 — Tập trung vào bài toán phân quyền nhân viên IT ngân hàng (đề tài ĐATN).*
