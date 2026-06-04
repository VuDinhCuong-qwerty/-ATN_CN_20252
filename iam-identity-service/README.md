# iam-identity-service

Microservice quản lý Users, Roles, Permissions cho **IAM Banking System** (Internal Auth Server).

## Port

| Profile | Port |
|---------|------|
| Local (dev) | `8080` |
| Docker | `8081` |

## Tech Stack

- Spring Boot 3.2.5 / Java 21
- Oracle DB (ojdbc11, DDL mode `validate`)
- Redis (Caffeine cache + RedisTemplate)
- Kafka SASL/PLAIN (3 brokers, programmatic config)
- JWT resource server (hiện **TẮT** để dev — `anyRequest().permitAll()`)

## Quick Start

```bash
# Run locally (dev profile)
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Build JAR
mvn clean package -DskipTests

# Build & run via Docker Compose (requires external iam-network)
mvn clean package -DskipTests && docker compose up -d
```

## API Endpoints

Base path: `/iam-identity-service`  
Convention: chỉ dùng **GET** và **POST**. Không dùng path variable — tham số qua `@RequestParam`.

---

### Giai đoạn 1 — User CRUD & Profile

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/users` | Tạo user mới (ADMIN) |
| GET | `/users` | Danh sách user (filter: username, employeeCode, departmentId, status) |
| GET | `/users/detail` | Chi tiết user (`?userId=&employeeCode=`) |
| POST | `/users/profile` | Admin sửa thông tin hành chính (`?employeeCode=`) |
| POST | `/users/personal` | User tự sửa thông tin cá nhân (`?employeeCode=`) |
| GET | `/users/addresses` | Danh sách địa chỉ (`?employeeCode=`) |
| POST | `/users/addresses` | Tạo / cập nhật địa chỉ (`?employeeCode=&type=`) |

---

### Giai đoạn 2 — Lifecycle

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/users/leave` | Nghỉ tạm thời (`?userId=&employeeCode=`) |
| POST | `/users/return` | Trở lại sau nghỉ (`?userId=&employeeCode=`) |
| POST | `/users/offboard` | Thôi việc — revoke toàn bộ quyền (`?userId=&employeeCode=`) |
| POST | `/users/onboard` | Onboard lại (`?userId=&employeeCode=`) |
| POST | `/users/transfer` | Chuyển bộ phận (`?userId=&employeeCode=`) |

---

### Giai đoạn 3 — Bảo mật

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/users/change-password` | Đổi mật khẩu (userId + employeeCode trong body) |
| POST | `/users/reset-password` | Reset mật khẩu (userId + employeeCode trong body) |

---

### Giai đoạn 4 — Phân quyền

#### Role Management

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/users/roles` | Danh sách role ACTIVE của user (`?employeeCode=&page=&size=`) |
| POST | `/users/roles` | Gán role cho user (`?employeeCode=`, body: `{"roleCode":"..."}`) |
| POST | `/users/roles/revoke` | Thu hồi role (`?employeeCode=&roleCode=`) — cascade revoke default permissions |

#### App Permission

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/users/{employeeCode}/app-permissions` | Danh sách app permission ACTIVE |
| POST | `/users/{employeeCode}/app-permissions/revoke` | Thu hồi app permissions (batch, cascade resources) |

#### Resource Permission

| Method | Path | Mô tả |
|--------|------|-------|
| GET | `/users/{employeeCode}/resource-permissions` | Danh sách resource permission ACTIVE |
| POST | `/users/resource-permissions/revoke` | Thu hồi resource permissions (batch, `?employeeCode=`) |

#### Permission Request Flow

| Method | Path | Mô tả |
|--------|------|-------|
| POST | `/permission-requests` | Tạo request xin quyền (DRAFT hoặc OFFICIAL) |
| POST | `/permission-requests/submit` | Submit DRAFT → OFFICIAL |
| GET | `/permission-requests` | Danh sách requests (filter: status, requester, reviewer, from, to) |
| GET | `/permission-requests/detail` | Chi tiết request (`?requestId=`) |
| POST | `/permission-requests/approve` | CAB duyệt → grant quyền thực sự |
| POST | `/permission-requests/reject` | CAB từ chối |
| POST | `/permission-requests/cancel` | User huỷ request (`?requestId=`) |

---

## Kafka Topics

| Topic | Khi nào publish | Payload |
|-------|----------------|---------|
| `CREATE-SUCCESS-USER-NOTIFY` | Tạo user / Onboard | `UserCreatedNotificationPayload` |
| `DEFAULT-GRANT-PERMISSION-USER` | Tạo user / Onboard / Transfer / Assign role | `UserCreatedPermissionPayload` |
| `USER-CHANGED-PASSWORD` | Đổi / Reset mật khẩu | `UserPasswordChangedPayload` |
| `REQUEST-PERMISSION-NOTIFY` | Submit OFFICIAL request | `PermissionRequestCreatedPayload` |
| `APPROVE-PERMISSION-NOTIFY` | CAB approve hoặc reject | `PermissionApprovedPayload` |
| `REVOKED-PERMISSION-NOTIFY` | Revoke app/resource permission | `AppPermissionRevokePayload` |

> Kafka publish nằm ngoài `@Transactional`, bọc `try-catch` riêng — DB commit không phụ thuộc Kafka.

## Key Design Decisions

- **Soft-delete toàn bộ**: STATUS = `INACTIVE` / `REVOKED` / `DELETED`, không xoá vật lý.
- **Leave vs Offboard**: Leave dùng `INACTIVE_FROM/TO_DATE` (STATUS vẫn ACTIVE); Offboard set STATUS = REVOKED vĩnh viễn.
- **Grant quyền qua luồng**: không có endpoint grant trực tiếp — tất cả đi qua `createPermissionRequest` → `approvePermissionRequest`.
- **`grantSource`** trên `AUTH_APP_PERMISSION` và `AUTH_USER_RESOURCE`: `'request'` (CAB approve), `'system'` (Kafka consumer), `null` (legacy). Permissions `grantSource='request'` được miễn trừ khi cascade revoke role.
- **JWT hiện TẮT**: `SecurityConfig` đang `anyRequest().permitAll()`. Khi bật lại, uncomment `oauth2ResourceServer` trong `SecurityConfig`.
