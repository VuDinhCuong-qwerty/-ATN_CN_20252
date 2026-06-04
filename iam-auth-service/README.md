# IAM Auth Service — v1.0

OAuth2 Authorization Server + OIDC Identity Provider built on Spring Boot 3.5 / Java 21.

> **v1.0** — Authorization Code + PKCE, Client Credentials, multi-step MFA engine with method switching, OIDC UserInfo, token introspection, key rotation.

---

## Overview

| | |
|---|---|
| **Version** | 1.0 |
| **Protocol** | OAuth 2.0 (RFC 6749), PKCE (RFC 7636), OIDC Core 1.0 |
| **Token signing** | ES256 (ECDSA P-256), keys rotate daily |
| **Storage** | Oracle DB (source of truth) · Redis (codes, tokens, sessions) · Caffeine (auth flow cache) |
| **Async** | Kafka (audit events, OTP notifications) |
| **Java** | 21 (virtual threads ready) |
| **Spring Boot** | 3.5.13 |

---

## Quick Start

### Prerequisites

- Java 21+
- Maven 3.9+
- Oracle DB (schema provisioned via `AUTH_PKG` stored procedures)
- Redis 7+
- Kafka 3.9+ *(optional — comment out `spring.kafka.*` to disable)*

### Run locally

```bash
# 1. Configure environment (copy and edit)
cp src/main/resources/application.properties application-local.properties
# Edit DB / Redis / Kafka connection strings

# 2. Build
mvn clean package -DskipTests

# 3. Run
mvn spring-boot:run
```

Service starts on **http://localhost:8080**.

### Docker

```bash
# Build image
docker build -t iam-auth-service .

# Run with dependencies on the iam-network
docker-compose up -d
```

`docker-compose.yml` expects an external Docker network named `iam-network` and external Oracle / Redis / Kafka containers.

---

## Environment Variables

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `auth_user1` | Oracle schema user |
| `DB_PASSWORD` | *(required)* | Oracle password |
| `REDIS_HOST` | `localhost` | Redis hostname |
| `REDIS_PASSWORD` | *(required)* | Redis password |
| `KAFKA_SERVERS` | `localhost:9092` | Kafka bootstrap servers |
| `SESSION_TOKEN_SECRET` | *(required)* | HMAC-SHA256 secret for session tokens — minimum 32 characters |
| `AUTH_ISSUER` | `http://localhost:8080/ms-internal-iam` | JWT `iss` claim |
| `AUTH_BASE_URL` | `http://localhost:8080` | Base URL for redirect URIs |
| `MFA_OTP_EXPIRY` | `300` | OTP TTL in seconds |
| `AUTH_MAX_FAILED` | `5` | Failed login attempts before lockout |
| `AUTH_LOCKOUT_DURATION` | `30` | Lockout duration in minutes |

> **Production checklist:**  
> - Set `SESSION_TOKEN_SECRET` to a cryptographically random string (≥ 32 chars)  
> - Set `AUTH_ISSUER` to the public HTTPS URL  
> - Use separate Oracle credentials with least-privilege  
> - Never commit `application.properties` with real credentials

---

## Endpoints

All endpoints are prefixed with `/ms-internal-iam/auth`.

### OAuth2 / OIDC

| Method | Path | Description |
|---|---|---|
| `GET` | `/authorize` | Authorization Code flow entry point |
| `POST` | `/token` | Token issuance (all grant types) |
| `POST` | `/token/revoke` | Token revocation (RFC 7009) |
| `POST` | `/token/introspect` | Token introspection (RFC 7662) |
| `GET` | `/userinfo` | OIDC UserInfo endpoint |
| `GET` | `/jwks` | JWK Set for signature verification |
| `GET` | `/.well-known/openid-configuration` | OIDC Discovery document |

### Session / MFA

| Method | Path | Description |
|---|---|---|
| `POST` | `/login` | Submit an MFA step (username/password, OTP, …) |
| `POST` | `/switch-method` | Validate current token; render method-selection page |
| `POST` | `/switch-method/confirm` | Confirm selected method; redirect to its login page |
| `POST` | `/logout` | Revoke SSO session + refresh tokens |

### Internal (Thymeleaf pages)

| Method | Path | Description |
|---|---|---|
| `GET` | `/internal/login-action` | Renders any MFA step page (theme + action-type param) |
| `GET` | `/internal/error` | Error page (code = 400 / 403 / 404) |

---

## Supported Grant Types

| Grant Type | Status | Use Case |
|---|---|---|
| `authorization_code` | Implemented | Browser-facing apps; PKCE required for public clients |
| `client_credentials` | Implemented | Service-to-service machine tokens |
| `refresh_token` | Pending | Token renewal without re-login — business logic researched |

---

## Token Format

### Access Token (`TokenForUser`)

Issued by the `authorization_code` grant. Signed with **ES256**.

```json
{
  "jti": "<uuid>",
  "iss": "http://auth.example.com/ms-internal-iam",
  "sub": "<userId>",
  "aud": "<clientId>",
  "iat": 1700000000,
  "exp": 1700003600,
  "type": "Bearer",
  "username": "john.doe",
  "email": "john@example.com",
  "displayName": "John Doe",
  "mobile": "+84900000000",
  "appId": 1,
  "serviceCode": "order-service",
  "clientId": "my-app",
  "scopes": ["openid", "profile", "read"],
  "role": "STAFF",
  "permissions": [
    "order-service/orders:read",
    "order-service/orders:write"
  ]
}
```

### Service Token (`TokenForService`)

Issued by the `client_credentials` grant.

```json
{
  "jti": "<uuid>",
  "iss": "...",
  "sub": "<clientId>",
  "iat": 1700000000,
  "exp": 1700003600,
  "type": "SERVICE",
  "clientId": "payment-service",
  "clientName": "Payment Service",
  "scopes": ["internal"]
}
```

### ID Token (`IdTokenClaims`)

Issued alongside the access token when scope includes `openid`.

```json
{
  "jti": "<uuid>",
  "iss": "...",
  "sub": "<userId>",
  "aud": "<clientId>",
  "iat": 1700000000,
  "exp": 1700003600,
  "nonce": "<client-nonce>",
  "name": "John Doe",
  "preferred_username": "john.doe",
  "email": "john@example.com",
  "phone_number": "+84900000000"
}
```

Scope-gated claims: `name`/`preferred_username` ← `profile`; `email` ← `email`; `phone_number` ← `phone`.

---

## Security Architecture

### Session Security

- `authSessionId` is stored **server-side only** in `HttpSession`. The browser never receives it.
- Each login/MFA HTML form contains a **single-use HMAC-SHA256 `sessionToken`** (generated by `SessionTokenUtil`). After the form is submitted, the token is invalidated — replaying the form POST returns an error.

### Signing Key Rotation

- JWK key pairs are generated in Oracle DB (`AUTH_SIGNING_KEY` table).
- A cron job rotates keys **daily at 12:00** (`cron.job.key.rotate`).
- Old keys are cleaned up **5 minutes later** (`cron.job.key.cleanup`).
- All keys are served at `/jwks` — resource servers can cache and re-fetch on unknown `kid`.

### Authorization Codes

- Stored in Redis with a **60-second TTL**.
- Marked used **atomically** (Lua script) — race-condition safe; a code can only be exchanged once.

### Refresh Tokens

- **Opaque** (64 random bytes, Base64url-encoded).
- Stored in Redis with key `auth:rt:<token>` and TTL = `REFRESH_TOKEN_TTL` from client config.
- An index key `auth:rt:idx:<userId>:<clientId>` enables **bulk revocation** (e.g. on logout).

---

## Auth Flow Engine

Authentication steps are modelled as a **tree** (`AuthFlow` / `FlowNode`) stored in Oracle and cached in-memory (Caffeine) per `appId`.

Each node maps to an `Authenticator` (e.g. `UsernamePassword`, `OtpEmail`). At runtime, `AuthenticatorRegistry` selects the implementation by node type and executes it.

**Method switching** — sibling nodes (same parent) represent alternative methods for the same step. The user can switch between them mid-flow via `POST /switch-method` without losing their auth session.

Adding a new MFA type = implementing `MFAProvider` and registering it — no changes to the engine core.

---

## Permission Model

```
AUTH_APPLICATION.SERVICE_CODE  →  app service slug    e.g. "order-service"
AUTH_RESOURCE.RESOURCE_CODE    →  resource slug        e.g. "orders"
AUTH_USER_RESOURCE.ACTION      →  CSV of actions       e.g. "read,write"

Permission string in token:    "order-service/orders:read"
```

Access control layers:
1. **`AUTH_APP_PERMISSION`** — user is allowed to access the application at all
2. **`AUTH_USER_RESOURCE`** — which resources and actions the user holds (ACTIVE, non-expired)
3. **`AUTH_USER_ROLE`** — one role per user (e.g. `STAFF`, `ADMIN`)

User profile + role + permissions are fetched in **a single Oracle stored-procedure call** (`AUTH_PKG.get_permission`).

---

## Build Commands

```bash
# Build (skip tests)
mvn clean package -DskipTests

# Build with tests
mvn clean verify

# Run a specific test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName

# Build Docker image
docker build -t iam-auth-service .

# Start with external dependency stack
docker-compose up -d
```

---

## Known Gaps (v1.0)

| Gap | Impact | Status |
|---|---|---|
| `refresh_token` grant not implemented | Cannot renew access tokens without re-login | Business logic researched; implementation planned for v1.1 |
| OIDC `nonce` not forwarded from `/authorize` | `id_token` always missing the `nonce` claim | Fix: add `@RequestParam(value="nonce", required=false)` to `GET /authorize` and wire into `AuthorizeRequest` |

---

## Project Structure

```
src/main/java/com/iam/auth/
├── controller/          REST endpoints + Thymeleaf MVC pages
├── service/             Interfaces; impls in service/impl/
├── engine/
│   ├── authorizer/      Grant type implementations (AuthorizationCode, ClientCredentials, RefreshToken)
│   ├── authenticator/   Auth step implementations (UsernamePassword, OtpEmail, …)
│   └── token/           Token payload POJOs (TokenForUser, TokenForService, IdTokenClaims)
├── domain/              JPA entities (Oracle)
├── repository/          Spring Data JPA repos + AuthRepository (stored procs)
├── dto/
│   ├── request/         Inbound DTOs
│   ├── response/        Outbound DTOs
│   └── pojo/            Internal POJOs (Client, AuthCode, RefreshTokenData, …)
├── mfa/                 MFAProvider plugin interface
├── kafka/               Event publishing
├── job/                 Scheduled tasks (key rotation, cleanup)
└── utils/               SessionTokenUtil (HMAC), helpers
```
