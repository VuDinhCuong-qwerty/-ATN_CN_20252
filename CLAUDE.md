# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Services & Ports

| Service | Port | Stack | Role |
|---|---|---|---|
| `iam-auth-service` | 8888 | Spring Boot 3.5, Oracle, Redis, Kafka | OAuth2 AS + OIDC IdP, MFA engine |
| `iam-identity-service` | 8081 | Spring Boot 3.2, Oracle, Kafka | User/Role/Permission CRUD + lifecycle |
| `iam-app-service` | 8082 | Spring Boot 3.2, Oracle | App/Client/Flow/DefaultPermission config |
| `iam-gateway` | 8080 | Spring Boot 3.2, Redis | API Gateway — JWT verify + permission check + token exchange |
| `iam-notify-service` | 8083 | Spring Boot 3.2, Kafka, Gmail | Kafka consumer → email notifications |
| `ldap-server` | 10389 | Spring Boot, ApacheDS embedded | LDAP gateway for third-party apps (GitLab, Kibana) |
| `iam-web-service` | 4200 (dev) | Angular 19 | IAM Admin Portal |
| `demo-change-app` | 8085 / 4201 | Spring Boot 3.2 + Angular 17 | Demo: Change & Go-Live Management |

**Shared Oracle DB:** schema `auth_user1`, PDB `AUTHPDB1`. JPA is `ddl-auto=validate` — never let JPA manage DDL.

## Build & Run

```bash
# Build any Spring Boot service (run from service directory)
mvn clean package -DskipTests

# Run locally
mvn spring-boot:run

# Run identity-service with dev profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Run a single test class / method
mvn test -Dtest=ClassName
mvn test -Dtest=ClassName#methodName

# Docker image (from service directory)
docker build -t <service-name> .
```

### Start dev environment (Windows)

```
# Start infra (Kafka 3-broker + Redis)
cd iam-cluster-docker
docker compose -f docker-compose.yml -f docker-compose.kafka.yml -f docker-compose.redis.yml up -d

# Start all 8 services in separate terminals
Double-click: dev-start.cmd   (in v2/ or root)
# Stop all
Double-click: dev-stop.cmd
```

`iam-notify-service` is commented out by default in dev-start.cmd — uncomment when testing email.

### Log stack (optional)

```bash
docker compose -f docker-compose.yml -f docker-compose.kibana.yml -f docker-compose.fluent-bit.yml up -d
```

### Angular (iam-web-service)

```bash
cd iam-web-service
npm install
ng serve          # dev server at :4200
ng build          # production build
```

## Architecture

### Request flow (Angular → backend)

```
Angular → iam-gateway:8080
  → JwtAuthFilter          (verify ES256 Bearer token)
  → PermissionCheckFilter  (match route → required permission string, check JWT claims)
  → UserContextFilter      (inject X-User-Id, X-User-Role, X-Employee-Code headers)
  → TokenExchangeFilter    (swap user token for client_credentials service token)
  → forward to iam-identity-service:8081 or iam-app-service:8082
```

All routes are declared in `iam-gateway/src/.../config/RouteConfig.java`. Specific sub-paths (e.g. `*/update`, `*/revoke`) must appear before wildcard routes — Spring Cloud Gateway uses first-match.

### Permission model

```
Permission string: "{serviceCode}/{resourceCode}:{action}"
Examples: "iam-service/user:create", "change-mgmt/change-request:approve"
```

- `AUTH_APP_PERMISSION` — controls whether a user can access an application at all
- `AUTH_USER_RESOURCE` — controls which resources/actions (`action` stored as CSV in one row per user/resource, not one row per action)
- All permissions are packed into the JWT `permissions` claim by `AUTH_PKG.get_permission` (Oracle stored proc) at token issuance time

Revocation is **immediate**: Kafka event → iam-auth-service revokes session + refresh token → 401 on next request without waiting for JWT expiry.

### Auth flow engine (iam-auth-service)

MFA steps are a tree (`AuthFlow`/`FlowNode`) stored in Oracle, cached by Caffeine per `appId`. Each node delegates to an `Authenticator` bean looked up in `AuthenticatorRegistry`. Adding a new MFA method = implement `Authenticator` + register as a Spring bean. No engine changes needed.

Token signing: ES256 (ECDSA P-256). Keys live in `AUTH_SIGNING_KEY`, rotate daily, served at `/jwks`.

Authorization codes: Redis, 60-second TTL, atomic single-use via Lua script.

### Gateway filter chain (important constraints)

- Services receive a **TokenForService** (client_credentials) — no `role` claim. User's role arrives as `X-User-Role` header from the gateway.
- iam-identity-service and iam-app-service have `anyRequest().authenticated()` + `oauth2ResourceServer JWT` active.
- iam-auth-service has a **custom** `SecurityConfig` — it is the auth server, not a resource server.

### Kafka

Events are published **outside `@Transactional`** — DB commits never depend on Kafka. Pattern: try-publish in a separate `try-catch` after the DB transaction commits.

| Topic | Publisher | Consumer |
|---|---|---|
| `CREATE-SUCCESS-USER-NOTIFY` | identity | notify |
| `DEFAULT-GRANT-PERMISSION-USER` | identity | identity (auto-grant defaults) |
| `USER-CHANGED-PASSWORD` | identity | auth (revoke all sessions) |
| `REQUEST-PERMISSION-NOTIFY` | identity | notify |
| `APPROVE-PERMISSION-NOTIFY` | identity | notify |
| `REVOKED-PERMISSION-NOTIFY` | identity | auth (revoke session per appId) |
| `CLIENT-SECRET-RESET-NOTIFY` | app | auth (revoke refresh tokens by clientId) |

### LDAP gateway (ldap-server)

Uses service-encoded DNs: `uid={USERNAME},ou={appServiceCode},ou=users,dc=iam,dc=bank,dc=vn`. One instance serves N apps — each app configures a different base DN. LDAP auth checks `AUTH_APP_PERMISSION`; resource permissions are encoded in the LDAP entry's `description` attribute for Kibana ES role mapping.

### API conventions (iam-identity-service & iam-app-service)

- Only `GET` and `POST` — no `PUT`, `PATCH`, `DELETE`
- No path variables for business parameters — use `@RequestParam`
- All deletes are soft (`STATUS = INACTIVE/REVOKED/DELETED`)

## Detailed references

- `.claude/CLAUDE.md` — full Angular portal component map, CSS theme, service method signatures, known gaps
- `iam-auth-service/.claude/CLAUDE.md` — auth service layer structure, token payload schemas, Kafka consumers, stored proc details
- `demo-change-app/.claude/` — Change app permission matrix, personas, job execution design
- `iam-cluster-docker/` — all Docker Compose overlay files; base file declares `iam-internal-network` bridge network

---

## Quyển Đồ Án Tốt Nghiệp (ACTIVE WORK)

> Sinh viên: Vũ Đình Cường | GVHD: TS. Trịnh Anh Phúc | Deadline: ~1 tuần (06/2026)  
> Đề tài: **"Xây dựng Hệ thống Phân quyền dành cho Lập trình viên Ngân hàng"**

### Vị trí file

```
quyển/                         ← thư mục LaTeX
  DoAn.tex                     ← file master (compile từ đây)
  Chuong/
    1_Gioi_thieu.tex
    2_Khao_sat.tex             ← đang viết tiếp
    3_Cong_nghe.tex
    4_Ket_qua_thuc_nghiem.tex
    5_Giai_phap_dong_gop.tex
    6_Ket_luan.tex
  Hinhve/                      ← đặt PNG diagram vào đây

.claude/planning/              ← KẾ HOẠCH CHI TIẾT TỪNG CHƯƠNG
  README.md                    ← đọc đầu tiên: thứ tự thực thi + danh sách hình cần tạo
  chapter2.md                  ← 2.2.9✅ + 2.3 + 2.4
  chapter3.md                  ← Công nghệ (5 mục)
  chapter4.md                  ← Thiết kế & Xây dựng (4.1 → 4.5)
  chapter5.md                  ← Giải pháp & Đóng góp (5 đóng góp — QUAN TRỌNG NHẤT)
  chapter6.md                  ← Kết luận + Hướng phát triển
  chapter1_final.md            ← Làm cuối: fix 1.2 + viết 1.4 + lời cảm ơn + tóm tắt
```

### Thứ tự thực thi

```
[1] Hoàn thiện Chương 2   → quyển/Chuong/2_Khao_sat.tex
[2] Viết Chương 3         → quyển/Chuong/3_Cong_nghe.tex
[3] Viết Chương 4         → quyển/Chuong/4_Ket_qua_thuc_nghiem.tex
[4] Viết Chương 5         → quyển/Chuong/5_Giai_phap_dong_gop.tex
[5] Viết Chương 6         → quyển/Chuong/6_Ket_luan.tex
[6] Hoàn thiện Chương 1   → quyển/Chuong/1_Gioi_thieu.tex + các file phụ
```

### Quy trình làm việc từng mục

```
1. Claude đọc planning file của chương → đề xuất nội dung ý chính + Mermaid diagram code
2. User chỉnh sửa / xác nhận nội dung
3. Claude viết LaTeX hoàn chỉnh → Edit vào file .tex tương ứng
4. User paste Mermaid vào draw.io → export PNG → đặt vào quyển/Hinhve/
```

### Ràng buộc viết bài

- **LDAP** — KHÔNG đề cập trong ch.2/3/4/5; chỉ nhắc ở ch.6 mục "Hướng phát triển"
- **MFA engine** — KHÔNG đi sâu; chỉ nhắc qua ở ch.3 như yếu tố kỹ thuật đi kèm
- **Focus chính:** Phân quyền (RBAC, permission lifecycle, CAB approval workflow, gateway enforcement, demo-change-app)
- **Văn phong:** Khoa học, đoạn văn đầy đủ chủ-vị, KHÔNG dùng bullet trong nội dung chính
- **Diagram:** Claude sinh Mermaid code → user export PNG bằng draw.io hoặc mermaid.live

### Hiện trạng file .tex

| File | Trạng thái |
|---|---|
| `1_Gioi_thieu.tex` | 1.1✅ 1.2⚠️(cần sửa 1 đoạn: 3app→1app) 1.3✅ 1.4❌ |
| `2_Khao_sat.tex` | 2.1✅ 2.2.1–2.2.9✅ 2.3❌ 2.4✅ |
| `3_Cong_nghe.tex` | ✅ DONE — 3.1(RBAC+OAuth2/OIDC/PKCE+JWT) 3.2(Java/Spring+Oracle+Kafka) 3.3(Kết luận). Kiến trúc Microservices+EDA đã chuyển → 4.1.1 |
| `4_Ket_qua_thuc_nghiem.tex` | 4.1.1✅ DONE (a: Microservices+EDA lý thuyết+trade-off / b: 4 service IAM + 7 Kafka topic áp dụng thực tế) · 4.1.2⬜ · 4.1.3⬜ · 4.2–4.5⬜ template |
| `5_Giai_phap_dong_gop.tex` | ❌ Chỉ có template hướng dẫn |
| `6_Ket_luan.tex` | ❌ Chỉ có 2 dòng placeholder |
| `0_2_Loi_cam_on.tex` | ❌ Chỉ có comment hướng dẫn |
| `0_3/0_4_Tom_tat.tex` | ❌ Placeholder hoàn toàn |
| `0_5_Danh_muc_viet_tat.tex` | ⚠️ Có nhưng cần update (xóa EUD/GWT/HTML, thêm IAM/JWT/RBAC...) |
