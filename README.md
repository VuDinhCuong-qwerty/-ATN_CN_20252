# Hệ thống Phân quyền dành cho Lập trình viên Ngân hàng

> **Đồ án tốt nghiệp** | Vũ Đình Cường | HUST 2025–2026  
> GVHD: TS. Trịnh Anh Phúc

---

## 1. Bài toán

Phòng IT ngân hàng vận hành nhiều hệ thống nội bộ nhạy cảm. Mỗi hệ thống có các chức năng với mức độ rủi ro khác nhau — một lệnh go-live sai có thể làm gián đoạn toàn bộ dịch vụ thanh toán; một tài khoản DevOps bị lộ có thể mở ra toàn bộ source code nội bộ.

Câu hỏi đặt ra:

> **Ai được làm gì? Cấp quyền theo quy trình nào? Thu hồi khi nào và như thế nào?**

Hệ thống IAM này trả lời câu hỏi đó bằng mô hình thống nhất:

```
Role × Position  →  Quyền mặc định tự động cấp khi onboard
                 →  Quyền đặc biệt phải qua luồng phê duyệt CAB
                 →  Thu hồi tự động khi chuyển bộ phận hoặc nghỉ việc
                 →  Hiệu lực tức thì — không chờ token hết hạn
```

---

## 2. Kiến trúc hệ thống

### 2.1 Các service

| Service | Port | Stack | Vai trò |
|---------|------|-------|---------|
| `iam-auth-service` | 8888 | Spring Boot 3.5 · Oracle · Redis · Kafka | OAuth2 Authorization Server + OIDC — đăng nhập, MFA, cấp JWT (ES256) |
| `iam-identity-service` | 8081 | Spring Boot 3.2 · Oracle · Kafka | Quản lý User / Role / Permission / Vòng đời nhân sự |
| `iam-app-service` | 8082 | Spring Boot 3.2 · Oracle | Quản lý Application, OAuth2 Client, Auth Flow, Default Permission |
| `iam-notify-service` | 8083 | Spring Boot 3.2 · Kafka · Gmail | Kafka consumer → gửi email thông báo |
| `iam-gateway` | 8080 | Spring Boot 3.2 · Redis | API Gateway — verify JWT, kiểm tra quyền, chuyển tiếp request |
| `iam-web-service` | 4200 | Angular 19 | IAM Admin Portal — giao diện quản trị toàn bộ hệ thống |
| `demo-change-app` | 8085 | Spring Boot 3.2 + Angular 17 | Ứng dụng demo: Change & Go-Live Management |
| `job-schedule` | — | Spring Boot 3.2 | Scheduled jobs — đồng bộ dữ liệu tỉnh/xã |

**Hạ tầng:** Oracle DB (schema `auth_user1`) · Redis 7 · Kafka 3 KRaft (3 broker, SASL/PLAIN)

### 2.2 Sơ đồ tổng thể

```
┌────────────────────────────────────────────┐
│              NGƯỜI DÙNG                    │
│   IAM Admin Portal · Change App Browser   │
└──────────────┬─────────────────────────────┘
               │ OAuth2 PKCE
               ▼
      ┌─────────────────┐
      │  iam-auth-svc   │   OAuth2 AS + OIDC
      │  :8888          │   MFA Engine (OTP Email)
      └────────┬────────┘   JWT ES256
               │ Bearer Token
               ▼
      ┌─────────────────┐
      │   iam-gateway   │   Verify JWT
      │   :8080         │   Permission check
      └────────┬────────┘   Token exchange
               │
      ┌────────┼─────────────────────┐
      ▼        ▼                     ▼
  identity  app-service         change-app
   :8081     :8082               :8085
      │
      ▼ Kafka Events
  notify-svc
   :8083  → Email (Gmail SMTP)
```

### 2.3 Luồng request qua Gateway

```
Angular → POST /api/identity/users
  → iam-gateway:8080
  → [1] JwtAuthFilter        verify Bearer token ES256
  → [2] PermissionFilter     route → required permission → check JWT claims
  → [3] UserContextFilter    inject X-User-Id, X-User-Role, X-Employee-Code
  → [4] TokenExchangeFilter  swap user token → client_credentials (TokenForService)
  → forward → iam-identity-service:8081
```

---

## 3. Mô hình phân quyền

### 3.1 Permission string trong JWT

Mỗi quyền được mã hóa thành một chuỗi theo format:

```
{serviceCode}/{resourceCode}:{action}

Ví dụ:
  "iam-service/user:create"
  "change-mgmt/change-request:approve"
  "change-mgmt/change-request:execute"
```

Khi đăng nhập thành công, auth-service gọi Oracle stored proc `AUTH_PKG.get_permission(userId, appId)` → trả toàn bộ quyền của user với app đó → ký vào `permissions` claim của JWT.

### 3.2 Hai loại quyền

| Loại | Cơ chế | Khi nào cấp |
|------|--------|-------------|
| **System** | `AUTH_DEFAULT_RESOURCE_PERMISSION` | Tự động cấp khi onboard / chuyển vị trí (role × position) |
| **Request** | `AUTH_PERMISSION_REQUEST` — CAB duyệt | Nhân viên gửi yêu cầu → CAB phê duyệt → cấp thêm |

> Quyền loại `REQUEST` (grantSource = `'request'`) **không bị thu hồi** khi user đổi role — chỉ thu hồi khi admin revoke thủ công.

### 3.3 Thu hồi quyền tức thì

Khi thu hồi quyền, hệ thống publish Kafka event → iam-auth-service:
- Invalidate SSO session theo `sessionId + appId`
- Revoke refresh token theo `userId + appId`

User đang đăng nhập sẽ nhận **401 ngay tại request tiếp theo** — không chờ JWT hết hạn.

---

## 4. Tổ chức nhân sự

### Roles — 3 vai trò cố định

| Role | Ý nghĩa |
|------|---------|
| `STAFF` | Nhân viên IT — truy cập tài nguyên theo vị trí công tác |
| `CAB` | Trưởng nhóm / Quản lý *(Change Advisory Board)* — phê duyệt yêu cầu quyền và change request |
| `ADMIN` | Quản trị viên IAM — quản trị toàn bộ hệ thống |

### Positions — 6 vị trí công tác phòng IT ngân hàng

| Vị trí | Mã | Role |
|--------|----|------|
| Kỹ thuật viên IT Cấp 1 | `IT_L1` | STAFF |
| Kỹ thuật viên IT Cấp 2 | `IT_L2` | STAFF |
| Quản trị viên CBS | `SYSADMIN` | STAFF |
| Kỹ sư vận hành Thanh toán | `PAYMENT_OPS` | STAFF |
| Trưởng phòng IT | `IT_MANAGER` | CAB |
| Giám đốc An ninh Thông tin | `CISO` | CAB |

---

## 5. Ứng dụng demo — Change & Go-Live Management

**Mã dịch vụ:** `change-mgmt` | **Thư mục:** `demo-change-app/` | **Port:** 8085

Hệ thống quản lý change request nội bộ phòng IT ngân hàng: từ lập kế hoạch, phê duyệt CAB, thực hiện go-live, đến ghi nhận kết quả. Mọi thao tác đều được kiểm soát bằng quyền IAM được ký trong JWT.

**Tích hợp IAM:** OAuth2 PKCE → JWT → Spring Boot Resource Server parse `permissions` claim trực tiếp, không qua gateway.

### Resources và Actions

| Resource | Actions | Ý nghĩa |
|----------|---------|---------|
| `change-request` | `view` | Xem danh sách và chi tiết change request |
| | `create` | Tạo mới change request (DRAFT) |
| | `update` | Chỉnh sửa DRAFT + cập nhật trạng thái checklist |
| | `execute` | Bắt đầu go-live (APPROVED → EXECUTING) và ghi nhận kết quả |
| | `approve` | Phê duyệt / từ chối change request (CAB only) |

### Ma trận phân quyền mặc định

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

### Vòng đời change request

```
DRAFT → PENDING → APPROVED → EXECUTING → SUCCESS
                ↑                      → FAIL
       (bất kỳ CAB reject → DRAFT)
```

**Logic phê duyệt:** Unanimous — tất cả CAB trong danh sách approvers phải đồng ý mới chuyển sang APPROVED.

---

## 6. Kịch bản nghiệp vụ

### Kịch bản 1 — Onboard tự động cấp quyền
> **Nguyễn Văn A** được tuyển vào vị trí `SYSADMIN`.

```
Admin tạo tài khoản → Gán role: STAFF, position: SYSADMIN
  → Kafka: DEFAULT-GRANT-PERMISSION-USER
  → Consumer tra bảng mặc định (STAFF × SYSADMIN):
      change-mgmt/change-request:view,create,update
      iam-service/user:read  ...
  → Nguyễn Văn A nhận email tài khoản tạm
  → Đăng nhập Change App: thấy đúng menu quyền, không thấy nút [Phê duyệt]
```

Không cần admin can thiệp thủ công — toàn bộ do hệ thống tự xử lý.

---

### Kịch bản 2 — Xin quyền go-live, CAB duyệt
> **Trần Thị B** (`SYSADMIN`) cần thực hiện go-live patch CBS khẩn cấp.

```
Trần Thị B tạo Permission Request trong IAM Portal:
  → App: change-mgmt | Resource: change-request | Action: execute
  → Lý do: "Patch CBS khẩn cấp xử lý lỗi batch EOD tối nay"
  → DRAFT → Gửi → OFFICIAL

IT_MANAGER nhận email → xét duyệt → Approve
  → Hệ thống cấp: change-mgmt/change-request:execute (grantSource = 'request')
  → Trần Thị B đăng nhập lại → JWT mới → nút [Bắt đầu Go-Live] xuất hiện
```

---

### Kịch bản 3 — Chuyển bộ phận, thu hồi và cấp quyền mới
> **Lê Văn C** chuyển từ `SYSADMIN` sang `PAYMENT_OPS`.

```
Admin transfer trong IAM Portal → position mới: PAYMENT_OPS
  → Thu hồi quyền System (SYSADMIN)
  → Cấp quyền mặc định mới (PAYMENT_OPS) — nếu trùng thì giữ nguyên
  → Quyền execute từ REQUEST → không bị thu hồi
  → Kafka event → iam-auth-service invalidate session hiện tại
  → Lê Văn C bị đăng xuất, JWT mới phản ánh vị trí mới
```

---

### Kịch bản 4 — Offboard, chặn truy cập tức thì
> **Phạm Thị D** nghỉ việc đột ngột.

```
Admin offboard Phạm Thị D:
  → Status: ACTIVE → INACTIVE
  → Thu hồi toàn bộ AUTH_APP_PERMISSION + AUTH_USER_RESOURCE
  → Kafka event → revoke toàn bộ session + refresh token

Kết quả tức thì:
  → Đang dùng Change App → 401 Unauthorized ngay lập tức
  → Mọi cổng hệ thống bị đóng, không chờ token hết hạn
```

---

### Kịch bản 5 — Change request đầy đủ vòng đời

```
IT_L2 tạo Change Request:
  → Điền thông tin, git link, thời gian go-live
  → Thêm Go-Live Jobs (MERGE → BUILD → DEPLOY theo thứ tự)
  → Thêm Checklist 3 giai đoạn (PRE / DURING / ROLLBACK)
  → Thêm Change Team + danh sách CAB approvers
  → Gửi duyệt: DRAFT → PENDING

IT_MANAGER approve + CISO approve → PENDING → APPROVED

Người có execute (từ Permission Request):
  → [Bắt đầu Go-Live] → APPROVED → EXECUTING
  → Từng thành viên cập nhật checklist item được gán
  → [Ghi nhận kết quả]:
      tất cả SUCCESS → EXECUTING → SUCCESS
      có FAIL       → EXECUTING → FAIL
```

---

## 7. Kỹ thuật

### 7.1 OAuth2 PKCE Flow

```
[Browser]                   [iam-auth-service:8888]        [Oracle DB]
    │── GET /authorize?PKCE ──────────────────────>│
    │<── Thymeleaf login page ────────────────────│
    │── POST /login ──────────────────────────────>│── validate ──>│
    │                                              │               │
    │   [MFA OTP Email nếu được cấu hình]         │               │
    │── POST /login (OTP step) ───────────────────>│── Redis OTP ──│
    │<── 302 /callback?code= ─────────────────────│  (60s TTL)    │
    │── POST /token {code, code_verifier} ────────>│── get_perms ──>│
    │<── {access_token, refresh_token, id_token} ─│               │
```

**Access token claims (ES256):**
```json
{
  "sub": "123",
  "username": "cuongvd",
  "role": "STAFF",
  "serviceCode": "change-mgmt",
  "permissions": [
    "change-mgmt/change-request:view",
    "change-mgmt/change-request:create"
  ]
}
```

### 7.2 Kafka Topics

| Topic | Publisher | Consumer | Hành động |
|-------|-----------|----------|-----------|
| `CREATE-SUCCESS-USER-NOTIFY` | identity | notify | Email chào mừng + tài khoản tạm |
| `DEFAULT-GRANT-PERMISSION-USER` | identity | identity | Auto-cấp quyền mặc định (role × position) |
| `USER-CHANGED-PASSWORD` | identity | auth | Revoke toàn bộ session + refresh token |
| `REQUEST-PERMISSION-NOTIFY` | identity | notify | Email thông báo yêu cầu quyền mới |
| `APPROVE-PERMISSION-NOTIFY` | identity | notify | Email kết quả CAB duyệt |
| `REVOKED-PERMISSION-NOTIFY` | identity | auth | Revoke session + token theo appId |
| `CLIENT-SECRET-RESET-NOTIFY` | app | auth | Revoke refresh token theo clientId |

> Kafka events publish **ngoài `@Transactional`** — DB commit không phụ thuộc Kafka.

### 7.3 MFA Flow Engine

Auth flow được cấu hình dạng **cây** trong Oracle, cache Caffeine per `appId`. Mỗi node là một phương thức xác thực:

```
FlowNode(USERNAME_PASSWORD) [root]
  └── FlowNode(OTP_EMAIL)   [leaf]
```

Thêm phương thức MFA mới = implement interface `Authenticator`, đăng ký Spring bean — không sửa engine.

### 7.4 Phạm vi kiểm soát IAM

| Tầng | IAM kiểm soát? | Ví dụ |
|------|---------------|-------|
| **App-level** | ✅ Có | Không có `AUTH_APP_PERMISSION` → không đăng nhập được app |
| **Resource + Action** | ✅ Có | `change-mgmt/change-request:approve` trong JWT → nút Phê duyệt hiện |
| **Row-level / Data-scope** | ❌ Không | Logic "chỉ thấy change request do mình tạo" nằm trong app |

> Row-level security là trách nhiệm của từng ứng dụng — thiết kế chuẩn (Keycloak, Okta, Auth0 đều như vậy).

---

## 8. Cách chạy

### Yêu cầu
- Docker Desktop đang chạy
- Oracle DB (schema `auth_user1`, `change_user1`) đang chạy và đã seed dữ liệu

### Khởi động toàn bộ hệ thống

```bash
cd iam-cluster-docker

# Khởi động: Redis + Kafka + IAM Services + Change App
demo-start.cmd          # Windows
```

Hoặc chạy thủ công:

```bash
docker compose \
  -f docker-compose.yml \
  -f docker-compose.redis.yml \
  -f docker-compose.kafka.yml \
  -f docker-compose.kafka-ui.yml \
  -f docker-compose.iam-auth-service.yml \
  -f docker-compose.iam-identity-service.yml \
  -f docker-compose.iam-app-service.yml \
  -f docker-compose.iam-gateway.yml \
  -f docker-compose.iam-notify-service.yml \
  -f docker-compose.demo-change-app.yml \
  up -d
```

### Services sau khi khởi động

| URL | Service |
|-----|---------|
| `http://localhost:8888` | IAM Auth (đăng nhập OAuth2) |
| `http://localhost:8080` | IAM Gateway |
| `http://localhost:8085` | Change & Go-Live Management App |
| `http://localhost:8090` | Kafka UI (giám sát message) |

### Thêm IAM Admin Portal (tùy chọn)

```bash
docker compose -f docker-compose.yml -f docker-compose.iam-web-service.yml up -d
# → http://localhost:4200
```

### Dừng tất cả

```bash
demo-stop.cmd           # Windows
```

### Build image từ source

```bash
# Mỗi Spring Boot service
cd <service-directory>
mvn clean package -DskipTests
docker build -t <service-name> .

# Angular (iam-web-service)
cd iam-web-service
npm install && ng build
docker build -t iam-web-service .
```

---

## 9. Cấu trúc thư mục

```
v3/
├── iam-auth-service/          OAuth2 AS + OIDC (Spring Boot 3.5, port 8888)
├── iam-identity-service/      User/Role/Permission CRUD (Spring Boot 3.2, port 8081)
├── iam-app-service/           App/Client/Flow config (Spring Boot 3.2, port 8082)
├── iam-notify-service/        Email notifications via Kafka (Spring Boot 3.2, port 8083)
├── iam-gateway/               API Gateway (Spring Boot 3.2, port 8080)
├── iam-web-service/           IAM Admin Portal (Angular 19, port 4200)
├── job-schedule/              Scheduled sync jobs (Spring Boot 3.2)
├── demo-change-app/
│   ├── change-app-service/    Change Management API (Spring Boot 3.2, port 8085)
│   └── change-web-service/    Change Management UI (Angular 17, bundled into 8085)
├── iam-cluster-docker/        Docker Compose overlays
│   ├── docker-compose.yml                  Base network + volumes
│   ├── docker-compose.kafka.yml            Kafka 3-broker KRaft
│   ├── docker-compose.redis.yml            Redis 7
│   ├── docker-compose.kafka-ui.yml         Kafka UI (port 8090)
│   ├── docker-compose.iam-auth-service.yml
│   ├── docker-compose.iam-identity-service.yml
│   ├── docker-compose.iam-app-service.yml
│   ├── docker-compose.iam-gateway.yml
│   ├── docker-compose.iam-notify-service.yml
│   ├── docker-compose.iam-web-service.yml
│   ├── docker-compose.demo-change-app.yml
│   ├── demo-start.cmd                      Khởi động toàn bộ demo (Windows)
│   ├── demo-stop.cmd                       Dừng toàn bộ demo (Windows)
│   └── config/                             kafka-jaas.conf, elasticsearch.yml ...
└── docs/                      Tài liệu thiết kế và kịch bản demo
```
