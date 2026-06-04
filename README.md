# Hệ thống Quản lý Định danh và Quyền truy cập — IAM Banking

---

## 1. Bài toán

Phòng IT ngân hàng vận hành nhiều hệ thống nội bộ nhạy cảm. Mỗi hệ thống có các chức năng với mức độ rủi ro khác nhau — một lệnh go-live sai có thể làm gián đoạn toàn bộ dịch vụ thanh toán; một account DevOps bị lộ có thể mở ra toàn bộ source code nội bộ.

Câu hỏi đặt ra:

> **Ai được làm gì? Cấp quyền theo quy trình nào? Thu hồi khi nào và như thế nào?**

Hệ thống IAM này trả lời câu hỏi đó bằng mô hình thống nhất:

```
Role × Position  →  Quyền mặc định tự động cấp khi onboard
                 →  Quyền đặc biệt phải qua luồng phê duyệt CAB
                 →  Thu hồi tự động khi chuyển bộ phận hoặc nghỉ việc
                 →  Hiệu lực tức thì — không chờ token hết hạn
```

Hệ thống được minh chứng qua **ba ứng dụng nội bộ thực tế**, mỗi ứng dụng tích hợp IAM theo một cách khác nhau.

---

## 2. Kiến trúc hệ thống

### 2.1 Các service IAM

| Service | Port | Stack | Vai trò |
|---------|------|-------|---------|
| `iam-auth-service` | 8888 | Spring Boot 3.5 · Oracle · Redis | OAuth2 Authorization Server + OIDC — đăng nhập, MFA, cấp JWT (ES256) |
| `iam-identity-service` | 8081 | Spring Boot 3.2 · Oracle · Kafka | Quản lý User / Role / Permission / Vòng đời nhân sự |
| `iam-app-service` | 8082 | Spring Boot 3.2 · Oracle | Quản lý Application, OAuth2 Client, Auth Flow, Default Permission |
| `iam-notify-service` | 8083 | Spring Boot 3.2 · Kafka · Gmail | Kafka consumer → gửi email thông báo |
| `iam-gateway` | 8080 | Spring Boot 3.2 · Redis | API Gateway — verify JWT, kiểm tra quyền, chuyển tiếp request |
| `ldap-server` | 10389 | Spring Boot · ApacheDS | LDAP Gateway — cho phép app bên thứ 3 xác thực qua IAM |
| `iam-web-service` | 4200 | Angular 19 | IAM Admin Portal — giao diện quản trị toàn bộ hệ thống |

**Hạ tầng:** Oracle DB (schema `auth_user1`) · Redis 7 · Kafka 3 KRaft (3 broker, SASL/PLAIN) · Elasticsearch + Kibana + Fluent Bit

### 2.2 Sơ đồ tổng thể

```
┌─────────────────────────────────────────────────────────────────┐
│                        NGƯỜI DÙNG                               │
│   Browser / GitLab Client / Kibana Browser                      │
└────────────┬────────────────┬──────────────────┬────────────────┘
             │ PKCE           │ LDAP             │ LDAP
             ▼                ▼                  ▼
    ┌─────────────────┐  ┌──────────┐   ┌──────────────┐
    │  iam-auth-svc   │  │ldap-srvr │   │ldap-server   │
    │  OAuth2 AS/OIDC │  │GitLab CE │   │Kibana/ES LDAP│
    │  MFA Engine     │  │port 8929 │   │port 10389    │
    └────────┬────────┘  └──────────┘   └──────────────┘
             │ JWT                              │
             ▼                                  │
    ┌─────────────────┐                         │
    │   iam-gateway   │◄────── Angular ─────────┘
    │   port 8080     │       Portal
    └────────┬────────┘
             │ TokenForService
     ┌───────┼────────────────────────┐
     ▼       ▼                        ▼
 identity  app-service            change-app
  :8081     :8082                   :8085
     │
     ▼ Kafka
 notify-svc        Fluent Bit → Elasticsearch → Kibana
  :8083             (logs)         :9200         :5601
```

---

## 3. Mô hình phân quyền

### 3.1 Permission string trong JWT

Mỗi quyền được mã hóa thành một chuỗi theo format:

```
{serviceCode}/{resourceCode}:{action}

Ví dụ:
  "change-mgmt/change-request:approve"
  "log-app-service/iam-system-logs:view"
  "iam-service/user:create"
```

Khi đăng nhập thành công, auth-service gọi Oracle stored proc `AUTH_PKG.get_permission(userId, appId)` → trả toàn bộ quyền của user với app đó → ký vào `permissions` claim của JWT.

### 3.2 Hai loại quyền

| Loại | Cơ chế | Khi nào cấp |
|------|--------|-------------|
| **System** | `AUTH_DEFAULT_RESOURCE_PERMISSION` | Tự động cấp khi onboard hoặc chuyển vị trí (role × position) |
| **Request** | `AUTH_PERMISSION_REQUEST` (CAB duyệt) | Nhân viên gửi yêu cầu → CAB phê duyệt → cấp thêm |

**Điểm quan trọng:** Quyền loại `REQUEST` (grantSource = `'request'`) **không bị thu hồi** khi user đổi role — chỉ thu hồi khi admin revoke thủ công.

### 3.3 Thu hồi quyền tức thì

Khi thu hồi quyền, hệ thống publish Kafka event → iam-auth-service consumer:
- Invalidate SSO session theo `sessionId + appId`
- Revoke refresh token theo `userId + appId`

User đang đăng nhập sẽ nhận 401 ngay tại request tiếp theo — không chờ JWT hết hạn.

---

## 4. Tổ chức nhân sự

### Roles — 3 vai trò cố định

| Role | Ý nghĩa | Quyền đặc thù |
|------|---------|---------------|
| `STAFF` | Nhân viên IT | Truy cập tài nguyên theo vị trí công tác |
| `CAB` | Trưởng nhóm / Quản lý *(Change Advisory Board)* | Phê duyệt yêu cầu quyền + phê duyệt change request |
| `ADMIN` | Quản trị viên IAM | Quản trị toàn bộ hệ thống |

### Positions — 6 vị trí công tác phòng IT ngân hàng

| Vị trí | Mã | Role | Mô tả công việc |
|--------|----|------|-----------------|
| Kỹ thuật viên IT Cấp 1 | `IT_L1` | STAFF | Tiếp nhận và xử lý sự cố cơ bản |
| Kỹ thuật viên IT Cấp 2 | `IT_L2` | STAFF | Xử lý sự cố phức tạp, submit change request |
| Quản trị viên CBS | `SYSADMIN` | STAFF | Vận hành Core Banking — batch, patch, parameter |
| Kỹ sư vận hành Thanh toán | `PAYMENT_OPS` | STAFF | Vận hành cổng NAPAS/SWIFT, ATM |
| Trưởng phòng IT | `IT_MANAGER` | CAB | Phê duyệt change request và permission request |
| Giám đốc An ninh TT | `CISO` | CAB | Audit toàn bộ — chỉ đọc, không thao tác |

---

## 5. Ba ứng dụng demo tích hợp IAM

### App 1 — Change & Go-Live Management

**Mã dịch vụ:** `change-mgmt` | **Thư mục:** `demo-change-app/` | **Port:** 8085

Hệ thống quản lý change request nội bộ phòng IT ngân hàng: từ lập kế hoạch, phê duyệt CAB, thực hiện go-live, đến ghi nhận kết quả. Mọi thao tác thực hiện (go-live) và phê duyệt đều được kiểm soát bằng quyền IAM trong JWT.

**Tích hợp IAM:** OAuth2 PKCE → JWT user token → Spring Boot Resource Server parse `permissions` claim trực tiếp.

**Resource và Actions:**

| Resource | Actions | Ý nghĩa |
|----------|---------|---------|
| `change-request` | `view` | Xem danh sách và chi tiết change request |
| | `create` | Tạo mới change request (DRAFT) |
| | `update` | Chỉnh sửa DRAFT + cập nhật trạng thái checklist |
| | `execute` | Bắt đầu go-live (APPROVED → EXECUTING) và ghi nhận kết quả |
| | `approve` | Phê duyệt / từ chối change request (CAB only) |

**Ma trận phân quyền mặc định:**

| Vị trí | view | create | update | execute | approve |
|--------|:----:|:------:|:------:|:-------:|:-------:|
| `IT_L1` | ✓ | ✓ | ✓ | — | — |
| `IT_L2` | ✓ | ✓ | ✓ | — | — |
| `SYSADMIN` | ✓ | ✓ | ✓ | — | — |
| `PAYMENT_OPS` | ✓ | ✓ | ✓ | — | — |
| `IT_MANAGER` | ✓ | — | — | — | ✓ |
| `CISO` | ✓ | — | — | — | ✓ |
| `ADMIN` | ✓ | ✓ | ✓ | ✓ | ✓ |

> `execute` không cấp mặc định cho STAFF — muốn thực hiện go-live phải gửi Permission Request, CAB duyệt.

**Vòng đời change request:**

```
DRAFT → PENDING → APPROVED → EXECUTING → SUCCESS
                    ↑                  → FAIL
           (bất kỳ CAB reject → DRAFT)
```

**Logic phê duyệt:** Unanimous — tất cả CAB trong danh sách approvers phải đồng ý mới chuyển sang APPROVED.

---

### App 2 — Log Monitoring (Kibana)

**Mã dịch vụ:** `log-app-service` | **Thư mục:** `iam-cluster-docker/` | **Port:** 5601

Hệ thống xem log tập trung cho toàn bộ IAM services, truy cập qua Kibana. Kibana được cấu hình xác thực qua LDAP realm trỏ về `ldap-server` — nghĩa là **user dùng đúng tài khoản IAM để đăng nhập Kibana**, không cần tài khoản riêng.

**Tích hợp IAM:** LDAP authentication (không dùng JWT) — ES LDAP realm → `ldap-server:10389` → Oracle IAM DB.

**Luồng xác thực Kibana:**

```
User nhập username/password trên Kibana
  → ES LDAP realm → ldap-server:10389 BIND
  → ldap-server kiểm tra AUTH_APP_PERMISSION (user có quyền vào log-app-service?)
  → BIND thành công → ldap-server trả LDAP entry với description = danh sách permission strings
  → ES đọc metadata.description → so khớp ES role mapping
  → Gán Kibana role tương ứng
```

**Hai tầng kiểm soát truy cập:**

| Tầng | Cơ chế | Kết quả khi không có quyền |
|------|--------|---------------------------|
| App-level | `AUTH_APP_PERMISSION` cho `log-app-service` | BIND fail → "Invalid credentials" |
| Resource-level | `AUTH_USER_RESOURCE` cho `log-app-service/iam-system-logs` | Login OK nhưng Kibana báo "no permission" |

**Resource và Actions:**

| Resource | Action | Kibana Role | Quyền trong Kibana |
|----------|--------|-------------|-------------------|
| `iam-system-logs` | `view` | `kibana_iam_viewer` | Discover (read-only) + Dashboard (read-only) |
| `iam-system-logs` | `edit` | `kibana_iam_editor` | Discover + Dashboard + tạo/sửa Data View |

**Ma trận phân quyền mặc định:**

| Role / Vị trí | view (Discover) | edit (Data View) |
|---------------|:---------------:|:----------------:|
| STAFF (mọi vị trí) | ✓ | — |
| CAB (`IT_MANAGER`) | ✓ | — |
| CAB (`CISO`) | ✓ | ✓ |
| ADMIN | ✓ | ✓ |

**Index trong Elasticsearch:**
- `logs-iam*` — log của tất cả IAM services (auth, identity, app, gateway)
- `logs-change-app*` — log của demo-change-app

**Hạ tầng:**

```
Spring Boot services
  └─ logback-spring.xml → LogstashTcpSocketAppender → localhost:5044
Fluent Bit (Docker :5044)
  └─ FILTER rewrite_tag (iam.logs → logs-iam, change.logs → logs-change-app)
  └─ OUTPUT → Elasticsearch :9200
Kibana :5601
  └─ LDAP realm → ldap-server :10389
```

---

### App 3 — Version Control (GitLab CE)

**Mã dịch vụ:** `gitlab-server` | **Thư mục:** `iam-cluster-docker/` | **Port:** 8929

GitLab CE — hệ thống quản lý source code nội bộ. Nhân viên phòng IT đăng nhập GitLab bằng **đúng tài khoản IAM** (không cần tạo tài khoản GitLab riêng). IAM kiểm soát ai được phép đăng nhập GitLab; quyền trong GitLab (project member, role) do GitLab quản lý độc lập.

**Tích hợp IAM:** LDAP authentication — GitLab gửi LDAP query → `ldap-server:10389` → Oracle IAM DB.

**Luồng xác thực GitLab:**

```
User nhập username/password trên GitLab
  → GitLab LDAP client → ldap-server:10389
  → SEARCH: base=ou=gitlab-server,ou=users,dc=iam,dc=bank,dc=vn (filter: uid=username)
  → ldap-server: kiểm tra AUTH_APP_PERMISSION cho gitlab-server → chỉ trả user có quyền
  → BIND: ldap-server xác thực password qua Oracle AUTH_USER
  → BIND thành công → GitLab cho đăng nhập, tự tạo GitLab account nếu chưa có
```

**Service-encoded DN (Approach B):**

DN format: `uid={USERNAME},ou=gitlab-server,ou=users,dc=iam,dc=bank,dc=vn`

1 instance `ldap-server` phục vụ N ứng dụng — mỗi app chỉ cần cấu hình `base DN` khác nhau. Không hardcode per-app.

**Kiểm soát truy cập:**

| Tình huống | Kết quả |
|------------|---------|
| User có `AUTH_APP_PERMISSION` cho `gitlab-server` | Đăng nhập được GitLab |
| User không có quyền | LDAP SEARCH không trả kết quả → GitLab báo "Invalid credentials" |
| Admin offboard user trong IAM | AUTH_APP_PERMISSION bị thu hồi → không đăng nhập GitLab được ngay |

---

## 6. Kịch bản nghiệp vụ

### Kịch bản 1 — Onboard tự động cấp quyền
> **Nguyễn Văn A** được tuyển vào vị trí `SYSADMIN`.

```
Admin tạo tài khoản cho Nguyễn Văn A
  → Gán role: STAFF, position: SYSADMIN
  → Hệ thống publish Kafka: DEFAULT-GRANT-PERMISSION-USER
  → Consumer tra bảng mặc định (STAFF × SYSADMIN):

      Ứng dụng IAM Portal (iam-service):
        iam-service/user:read
        iam-service/user-lifecycle:read
        ...

      Change Management (change-mgmt):
        change-mgmt/change-request:view
        change-mgmt/change-request:create
        change-mgmt/change-request:update

      Log Monitoring (log-app-service):
        log-app-service/iam-system-logs:view

  → Nguyễn Văn A nhận email với tài khoản tạm và hướng dẫn đổi mật khẩu lần đầu
  → Đăng nhập IAM Portal: thấy menu tương ứng quyền
  → Đăng nhập GitLab: tự động tạo account GitLab, vào được dự án được assign
  → Đăng nhập Kibana: thấy Discover logs, không tạo được Data View
```

**Không cần admin can thiệp thủ công** — toàn bộ do hệ thống tự xử lý.

---

### Kịch bản 2 — Xin quyền go-live, CAB duyệt
> **Trần Thị B** (`SYSADMIN`) cần thực hiện go-live patch CBS khẩn cấp.

```
Trần Thị B tạo Permission Request trong IAM Portal:
  → App: change-mgmt | Resource: change-request | Action: execute
  → Lý do: "Patch CBS khẩn cấp xử lý lỗi batch EOD tối nay"
  → Trạng thái: DRAFT → Gửi → OFFICIAL

IT_MANAGER nhận email thông báo:
  → Xem yêu cầu trong IAM Portal, xét duyệt
  → Approve → trạng thái: APPROVED

Hệ thống tự cấp:
  → change-mgmt/change-request:execute (grantSource = 'request')
  → Trần Thị B đăng nhập lại → JWT mới chứa quyền execute
  → Nút [Bắt đầu Go-Live] xuất hiện trong Change Management app
```

**Quyền này có grantSource = REQUEST** — sau này khi IT_MANAGER đổi người, quyền của Trần Thị B vẫn còn cho đến khi admin thu hồi thủ công.

---

### Kịch bản 3 — Chuyển bộ phận, thu hồi và cấp quyền mới
> **Lê Văn C** chuyển từ `SYSADMIN` sang `PAYMENT_OPS`.

```
Admin thực hiện transfer trong IAM Portal:
  → Position cũ: SYSADMIN → Position mới: PAYMENT_OPS

Hệ thống tự động:
  → Thu hồi toàn bộ quyền System (cấp theo SYSADMIN):
      change-mgmt/change-request:view,create,update  ← giữ nguyên (giống PAYMENT_OPS)
      (nếu có quyền execute từ REQUEST → không bị thu hồi)

  → Kafka event → iam-auth-service invalidate session hiện tại của Lê Văn C
  → Lê Văn C bị đăng xuất, đăng nhập lại với JWT mới phản ánh vị trí mới

Trong GitLab / Kibana:
  → Vẫn đăng nhập được (AUTH_APP_PERMISSION không thay đổi)
  → Quyền trong GitLab do GitLab quản lý độc lập
```

---

### Kịch bản 4 — Offboard, chặn truy cập tức thì
> **Phạm Thị D** nghỉ việc đột ngột, cần chặn ngay tất cả hệ thống.

```
Admin offboard Phạm Thị D trong IAM Portal:
  → Trạng thái user: ACTIVE → INACTIVE
  → Thu hồi toàn bộ AUTH_APP_PERMISSION và AUTH_USER_RESOURCE
  → Kafka event → iam-auth-service revoke toàn bộ session + refresh token

Kết quả tức thì:
  → Phạm Thị D đang dùng Change Management → 401 Unauthorized ngay lập tức
  → Refresh token bị revoke → không lấy được token mới
  → Đăng nhập Kibana → BIND fail "Invalid credentials"
  → Đăng nhập GitLab → BIND fail "Invalid credentials"
  → Mọi cổng hệ thống bị đóng, không chờ token hết hạn tự nhiên
```

---

### Kịch bản 5 — Change request đầy đủ vòng đời
> **Nhóm IT** chuẩn bị go-live patch hệ thống thanh toán.

```
Lê Văn C (IT_L2) tạo Change Request:
  → Điền thông tin: tên, nội dung, git link, thời gian go-live
  → Thêm danh sách Go-Live Jobs (MERGE → BUILD → DEPLOY theo thứ tự)
  → Thêm Checklist 3 giai đoạn (PRE / DURING / ROLLBACK), gán từng bước cho người thực hiện
  → Thêm Change Team (kỹ thuật viên tham gia)
  → Thêm danh sách CAB approvers (IT_MANAGER + CISO)
  → Gửi duyệt (DRAFT → PENDING)

IT_MANAGER và CISO nhận email:
  → IT_MANAGER approve
  → CISO approve → tất cả CAB đã approve → PENDING → APPROVED

Lê Văn C (có execute permission từ request):
  → Click [Bắt đầu Go-Live] → APPROVED → EXECUTING

Từng thành viên team thực hiện checklist được gán:
  → Mỗi người chỉ thấy nút cập nhật trạng thái cho checklist item được gán cho mình
  → Click cập nhật: READY → SUCCESS / FAIL

Lê Văn C (người tạo change):
  → Click [Ghi nhận kết quả] → hệ thống kiểm tra tất cả checklist
  → Nếu toàn bộ SUCCESS → EXECUTING → SUCCESS
  → Nếu có FAIL → EXECUTING → FAIL
```

---

## 7. Kỹ thuật

### 7.1 Luồng xác thực OAuth2 PKCE

```
[Browser / Angular]              [iam-auth-service:8888]          [Oracle DB]
        │                                  │                           │
        │── GET /authorize?PKCE ──────────>│                           │
        │<── Thymeleaf login page ─────────│                           │
        │                                  │                           │
        │── POST /login ──────────────────>│── validate credentials ──>│
        │   {username, password}           │<── userId ────────────────│
        │                                  │                           │
        │   [Nếu cấu hình MFA OTP Email]  │                           │
        │── POST /login (OTP step) ───────>│── verify OTP cache ──────>│Redis
        │                                  │                           │
        │<── 302 /callback?code= ──────────│  (code → Redis 60s TTL)   │
        │                                  │                           │
        │── POST /token ──────────────────>│── AUTH_PKG.get_permission>│
        │   {code, code_verifier}          │<── profile+role+perms ────│
        │                                  │                           │
        │<── {access_token, refresh_token, id_token} ─────────────────│
```

**Access token claims (JWT, ký ES256):**
```json
{
  "sub": "123",
  "username": "cuongvd",
  "email": "cuong@bank.vn",
  "displayName": "Vũ Đình Cương",
  "appId": 1,
  "serviceCode": "change-mgmt",
  "clientId": "change-mgmt-web",
  "role": "STAFF",
  "permissions": [
    "change-mgmt/change-request:view",
    "change-mgmt/change-request:create",
    "change-mgmt/change-request:update"
  ]
}
```

### 7.2 Luồng request qua Gateway

```
Angular → POST /api/identity/users
  → iam-gateway:8080
  → [Filter 1] JwtAuthFilter: verify Bearer token → extract claims
  → [Filter 2] PermissionFilter: route map → yêu cầu "user:read" → check permissions claim
  → [Filter 3] UserContextFilter: set X-User-Id, X-User-Role, X-Employee-Code headers
  → [Filter 4] TokenExchangeFilter: client_credentials → TokenForService
  → forward → iam-identity-service:8081/iam-identity-service/users
```

**Route → Permission map (ví dụ):**

| Route | Quyền cần có |
|-------|-------------|
| `GET /iam-identity-service/users` | `user:read` |
| `POST /iam-identity-service/users/create` | `user:create` |
| `POST /iam-identity-service/users/lifecycle/onboard` | `user-lifecycle:onboard` |
| `POST /iam-identity-service/permission-requests` | `user-permission:request` |
| `POST /iam-app-service/applications` | `application:create` |

### 7.3 MFA Flow Engine

Auth flow được cấu hình dạng **cây** trong Oracle (`AUTH_FLOW`, `AUTH_FLOW_EXECUTION`), cache Caffeine per `appId`. Mỗi node là một phương thức xác thực:

```
FlowNode(USERNAME_PASSWORD) [root]
  ├── FlowNode(OTP_EMAIL)   [leaf — xác thực thành công]
  └── FlowNode(OTP_SMS)     [sibling — user có thể switch]
```

Thêm phương thức MFA mới = implement interface `Authenticator`, đăng ký Spring bean — không sửa engine.

**Switch method:** User đang nhập OTP Email có thể chuyển sang OTP SMS mid-flow mà không mất auth session.

### 7.4 Kafka Event-Driven

| Topic | Publisher | Consumer | Hành động |
|-------|-----------|----------|-----------|
| `CREATE-SUCCESS-USER-NOTIFY` | identity | notify | Email chào mừng + tài khoản tạm |
| `DEFAULT-GRANT-PERMISSION-USER` | identity | identity | Auto-cấp quyền mặc định (role × position) |
| `USER-CHANGED-PASSWORD` | identity | auth | Revoke toàn bộ session + refresh token |
| `REQUEST-PERMISSION-NOTIFY` | identity | notify | Email thông báo yêu cầu quyền mới |
| `APPROVE-PERMISSION-NOTIFY` | identity | notify | Email kết quả CAB duyệt |
| `REVOKED-PERMISSION-NOTIFY` | identity | auth | Revoke session + token theo appId |
| `CLIENT-SECRET-RESET-NOTIFY` | app | auth | Revoke refresh token theo clientId |

Kafka events publish **ngoài @Transactional** — DB commit không phụ thuộc Kafka. Nếu Kafka down, DB vẫn consistent.

### 7.5 Phạm vi kiểm soát

| Tầng | IAM kiểm soát? | Ví dụ |
|------|---------------|-------|
| **App-level** | ✅ Có | Không có `AUTH_APP_PERMISSION` → không đăng nhập được app |
| **Resource + Action** | ✅ Có | `change-mgmt/change-request:approve` trong JWT → nút [Phê duyệt] hiện; không có → ẩn |
| **Row-level / Data-scope** | ❌ Không | "Chỉ thấy change request do mình tạo" — logic này nằm trong app, IAM không can thiệp |

> Row-level security là trách nhiệm của từng ứng dụng — thiết kế chuẩn của IAM thực tế (Keycloak, Okta, Auth0 đều như vậy).

---

## 8. Cách chạy môi trường dev

### Yêu cầu trước
- Oracle DB (schema `auth_user1`, `change_user1`) đang chạy
- Redis và Kafka đang chạy (hoặc dùng docker compose)

```bash
# Khởi động infra (Redis + Kafka)
cd iam-cluster-docker
docker compose -f docker-compose.yml -f docker-compose.kafka.yml -f docker-compose.redis.yml up -d

# Khởi động Log Stack (Elasticsearch + Kibana + Fluent Bit) — nếu cần
docker compose -f docker-compose.yml -f docker-compose.kibana.yml -f docker-compose.fluent-bit.yml up -d

# Khởi động GitLab CE — nếu cần
docker compose -f docker-compose.yml -f docker-compose.gitlab.yml up -d
```

### Khởi động tất cả services

```
Double-click: v2\dev-start.cmd
```

Script tự động mở **8 terminal riêng biệt**, mỗi terminal chạy 1 service:

| Terminal | Service | Port |
|----------|---------|------|
| 1 | iam-auth-service | 8888 |
| 2 | iam-identity-service | 8081 |
| 3 | iam-app-service | 8082 |
| 4 | iam-gateway | 8080 |
| 5 | ldap-server | 10389 |
| 6 | iam-web-service (Angular) | 4200 |
| 7 | change-app-service | 8085 |
| 8 | change-web-service (Angular) | 4201 |

> `iam-notify-service` mặc định tắt trong script (comment) — bật lại khi cần test email.

### Tắt tất cả services

```
# Cách 1: Nhấn phím bất kỳ trong cửa sổ dev-start → nhập y → Enter
# Cách 2: Double-click v2\dev-stop.cmd
```

---

## 9. Cấu trúc thư mục

```
v2/
├── iam-auth-service/          OAuth2 AS + OIDC (Spring Boot 3.5)
├── iam-identity-service/      User/Role/Permission CRUD (Spring Boot 3.2)
├── iam-app-service/           App/Client/Flow config (Spring Boot 3.2)
├── iam-notify-service/        Email notifications (Spring Boot 3.2)
├── iam-gateway/               API Gateway (Spring Boot 3.2)
├── ldap-server/               LDAP Gateway — ApacheDS embedded (Spring Boot 3.2)
├── job-schedule/              Scheduled sync jobs
├── iam-web-service/           IAM Admin Portal (Angular 19)
├── demo-change-app/
│   ├── change-app-service/    Change Management API (Spring Boot 3.2, port 8085)
│   └── change-web-service/    Change Management UI (Angular 17, port 4201)
├── iam-cluster-docker/        Docker Compose overlays + config files
│   ├── docker-compose.yml     Base (network + volumes)
│   ├── docker-compose.kafka.yml
│   ├── docker-compose.redis.yml
│   ├── docker-compose.kibana.yml
│   ├── docker-compose.fluent-bit.yml
│   ├── docker-compose.gitlab.yml
│   └── config/                elasticsearch.yml, fluent-bit.conf, kafka-jaas.conf ...
├── redis/                     Standalone Redis compose (dev)
├── dev-start.cmd              Khởi động 8 services (Windows)
└── dev-stop.cmd               Dừng tất cả services (Windows)
```
