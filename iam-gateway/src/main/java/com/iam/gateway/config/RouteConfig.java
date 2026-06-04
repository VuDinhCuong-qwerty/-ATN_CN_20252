package com.iam.gateway.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import com.iam.gateway.filter.PermissionCheckFilterFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RouteConfig {

    private final PermissionCheckFilterFactory permissionCheck;

    @Value("${gateway.identity-uri}")
    private String identityUri;

    @Value("${gateway.app-uri}")
    private String appUri;

    @Bean
    public RouteLocator routes(RouteLocatorBuilder b) {
        return b.routes()

                // ── iam-identity-service: User CRUD ─────────────────────────────────
                .route("get-users", r -> r
                        .path("/iam-identity-service/users").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user:read")))
                        .uri(identityUri))

                .route("create-user", r -> r
                        .path("/iam-identity-service/users").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user:create")))
                        .uri(identityUri))

                .route("get-user-detail", r -> r
                        .path("/iam-identity-service/users/detail").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user:read")))
                        .uri(identityUri))

                .route("update-user-profile", r -> r
                        .path("/iam-identity-service/users/profile").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user:update")))
                        .uri(identityUri))

                .route("update-personal-info", r -> r
                        .path("/iam-identity-service/users/personal").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user:update")))
                        .uri(identityUri))

                .route("get-addresses", r -> r
                        .path("/iam-identity-service/users/addresses").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user:read")))
                        .uri(identityUri))

                .route("upsert-address", r -> r
                        .path("/iam-identity-service/users/addresses").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user:update")))
                        .uri(identityUri))

                .route("get-address", r -> r
                        .path("/iam-identity-service/users/provinces").and().method(GET)
                        .uri(identityUri))

                .route("get-address", r -> r
                        .path("/iam-identity-service/users/wards").and().method(GET)
                        .uri(identityUri))

                // ── iam-identity-service: User Lifecycle ────────────────────────────
                .route("user-leave", r -> r
                        .path("/iam-identity-service/users/leave").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-lifecycle:leave")))
                        .uri(identityUri))

                .route("user-return", r -> r
                        .path("/iam-identity-service/users/return").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-lifecycle:return")))
                        .uri(identityUri))

                .route("user-offboard", r -> r
                        .path("/iam-identity-service/users/offboard").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-lifecycle:offboard")))
                        .uri(identityUri))

                .route("user-onboard", r -> r
                        .path("/iam-identity-service/users/onboard").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-lifecycle:onboard")))
                        .uri(identityUri))

                .route("user-transfer", r -> r
                        .path("/iam-identity-service/users/transfer").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-lifecycle:transfer")))
                        .uri(identityUri))

                // ── iam-identity-service: User Credentials ──────────────────────────
                .route("change-password", r -> r
                        .path("/iam-identity-service/users/change-password").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-credential:change-password")))
                        .uri(identityUri))

                .route("reset-password", r -> r
                        .path("/iam-identity-service/users/reset-password").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-credential:reset-password")))
                        .uri(identityUri))

                // ── iam-identity-service: Roles ─────────────────────────────────────
                .route("get-roles", r -> r
                        .path("/iam-identity-service/users/roles").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/role:read")))
                        .uri(identityUri))

                .route("assign-role", r -> r
                        .path("/iam-identity-service/users/roles").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/role:assign")))
                        .uri(identityUri))

                .route("revoke-role", r -> r
                        .path("/iam-identity-service/users/roles/revoke").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/role:revoke")))
                        .uri(identityUri))

                // ── iam-identity-service: App Permissions ───────────────────────────
                // NOTE: /revoke must come before /* wildcard (first-match wins)
                .route("revoke-app-permission", r -> r
                        .path("/iam-identity-service/users/*/app-permissions/revoke").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:revoke")))
                        .uri(identityUri))

                .route("get-app-permissions", r -> r
                        .path("/iam-identity-service/users/*/app-permissions").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user-permission:read")))
                        .uri(identityUri))

                .route("get-resource-permissions", r -> r
                        .path("/iam-identity-service/users/*/resource-permissions").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user-permission:read")))
                        .uri(identityUri))

                .route("revoke-resource-permission", r -> r
                        .path("/iam-identity-service/users/resource-permissions/revoke").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:revoke")))
                        .uri(identityUri))

                // ── iam-identity-service: Permission Requests ───────────────────────
                // NOTE: specific sub-paths must come before the bare /permission-requests route
                .route("get-permission-request-detail", r -> r
                        .path("/iam-identity-service/permission-requests/detail").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user-permission:read")))
                        .uri(identityUri))

                .route("submit-permission-request", r -> r
                        .path("/iam-identity-service/permission-requests/submit").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:request")))
                        .uri(identityUri))

                .route("approve-permission-request", r -> r
                        .path("/iam-identity-service/permission-requests/approve").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:approve")))
                        .uri(identityUri))

                .route("reject-permission-request", r -> r
                        .path("/iam-identity-service/permission-requests/reject").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:approve")))
                        .uri(identityUri))

                .route("cancel-permission-request", r -> r
                        .path("/iam-identity-service/permission-requests/cancel").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:cancel")))
                        .uri(identityUri))

                .route("update-permission-request", r -> r
                        .path("/iam-identity-service/permission-requests/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:request")))
                        .uri(identityUri))

                .route("get-permission-requests", r -> r
                        .path("/iam-identity-service/permission-requests").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/user-permission:read")))
                        .uri(identityUri))

                .route("create-permission-request", r -> r
                        .path("/iam-identity-service/permission-requests").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/user-permission:request")))
                        .uri(identityUri))

                // ── iam-app-service: App Groups ──────────────────────────────────────
                .route("update-app-group", r -> r
                        .path("/iam-app-service/app-groups/*/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:update")))
                        .uri(appUri))

                .route("get-app-groups", r -> r
                        .path("/iam-app-service/app-groups").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/application:read")))
                        .uri(appUri))

                .route("create-app-group", r -> r
                        .path("/iam-app-service/app-groups").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:create")))
                        .uri(appUri))

                // ── iam-app-service: Applications ────────────────────────────────────
                // NOTE: specific paths (*/update, */status) before wildcard (/*) — first-match
                // wins
                .route("update-application", r -> r
                        .path("/iam-app-service/applications/*/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:update")))
                        .uri(appUri))

                .route("set-application-status", r -> r
                        .path("/iam-app-service/applications/*/status").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:update")))
                        .uri(appUri))

                .route("get-application-detail", r -> r
                        .path("/iam-app-service/applications/*").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/application:read")))
                        .uri(appUri))

                .route("get-applications", r -> r
                        .path("/iam-app-service/applications").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/application:read")))
                        .uri(appUri))

                .route("create-application", r -> r
                        .path("/iam-app-service/applications").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:create")))
                        .uri(appUri))

                // ── iam-app-service: Resources (nested under application) ─────────────
                // NOTE: batch and */update must come before wildcard (/*) — first-match wins
                .route("batch-create-resources", r -> r
                        .path("/iam-app-service/applications/*/resources/batch").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:create")))
                        .uri(appUri))

                .route("update-resource", r -> r
                        .path("/iam-app-service/applications/*/resources/*/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/application:update")))
                        .uri(appUri))

                .route("get-resource-detail", r -> r
                        .path("/iam-app-service/applications/*/resources/*").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/application:read")))
                        .uri(appUri))

                .route("get-resources", r -> r
                        .path("/iam-app-service/applications/*/resources").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/application:read")))
                        .uri(appUri))

                // ── iam-app-service: OAuth2 Clients ──────────────────────────────────
                // NOTE: specific paths (*/update, */secret/reset, */scopes) before wildcard
                // (/*) — first-match wins
                .route("update-client", r -> r
                        .path("/iam-app-service/clients/*/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/client:update")))
                        .uri(appUri))

                .route("reset-client-secret", r -> r
                        .path("/iam-app-service/clients/*/secret/reset").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/client:update")))
                        .uri(appUri))

                .route("manage-client-scopes", r -> r
                        .path("/iam-app-service/clients/*/scopes").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/client:update")))
                        .uri(appUri))

                .route("get-client-detail", r -> r
                        .path("/iam-app-service/clients/*").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/client:read")))
                        .uri(appUri))

                .route("get-clients", r -> r
                        .path("/iam-app-service/clients").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/client:read")))
                        .uri(appUri))

                .route("create-client", r -> r
                        .path("/iam-app-service/clients").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/client:create")))
                        .uri(appUri))

                // ── iam-app-service: Auth Flows (nested under application) ────────────
                // NOTE: */update must come before wildcard (/*) — first-match wins
                .route("update-auth-flow", r -> r
                        .path("/iam-app-service/applications/*/flows/*/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/auth-flow:update")))
                        .uri(appUri))

                .route("get-auth-flow-detail", r -> r
                        .path("/iam-app-service/applications/*/flows/*").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/auth-flow:read")))
                        .uri(appUri))

                .route("get-auth-flows", r -> r
                        .path("/iam-app-service/applications/*/flows").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/auth-flow:read")))
                        .uri(appUri))

                .route("create-auth-flow", r -> r
                        .path("/iam-app-service/applications/*/flows").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/auth-flow:create")))
                        .uri(appUri))

                // ── iam-app-service: Client Methods (nested under application) ─────────
                // NOTE: */update must come before bare /methods route — first-match wins
                .route("update-client-method", r -> r
                        .path("/iam-app-service/applications/*/methods/*/update").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/auth-flow:update")))
                        .uri(appUri))

                .route("get-client-methods", r -> r
                        .path("/iam-app-service/applications/*/methods").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/auth-flow:read")))
                        .uri(appUri))

                .route("batch-create-client-methods", r -> r
                        .path("/iam-app-service/applications/*/methods").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/auth-flow:create")))
                        .uri(appUri))

                // ── iam-app-service: Default Permissions ──────────────────────────────
                .route("get-default-permissions", r -> r
                        .path("/iam-app-service/default-permissions/**").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/default-permission:read")))
                        .uri(appUri))

                .route("manage-default-permissions", r -> r
                        .path("/iam-app-service/default-permissions/**").and().method(POST)
                        .filters(f -> f.filter(require("iam-service/default-permission:create")))
                        .uri(appUri))

                // ── iam-app-service: Reference / Lookup ───────────────────────────────
                .route("reference-data", r -> r
                        .path("/iam-app-service/reference/**").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/application:read")))
                        .uri(appUri))

                .route("auth-methods-catalog", r -> r
                        .path("/iam-app-service/auth-methods").and().method(GET)
                        .filters(f -> f.filter(require("iam-service/auth-flow:read")))
                        .uri(appUri))

                .build();
    }

    private GatewayFilter require(String permission) {
        return permissionCheck.apply(new PermissionCheckFilterFactory.Config(permission));
    }
}
