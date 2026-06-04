package com.iam.app.service.impl;

import com.iam.app.domain.AuthApplication;
import com.iam.app.domain.AuthClient;
import com.iam.app.domain.AuthClientOauth;
import com.iam.app.dto.request.CreateClientRequest;
import com.iam.app.dto.request.ScopeRequest;
import com.iam.app.dto.request.UpdateClientRequest;
import com.iam.app.dto.response.ClientDetailResponse;
import com.iam.app.dto.response.ClientListResponse;
import com.iam.app.dto.response.CreateClientResponse;
import com.iam.app.config.KafkaConfig;
import com.iam.app.kafka.payload.ClientSecretResetPayload;
import com.iam.app.kafka.producer.AppEventProducer;
import com.iam.app.repository.jpa.AuthRepository;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthClientOauthRepository;
import com.iam.app.repository.jpa.AuthClientRepository;
import com.iam.app.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientServiceImpl implements ClientService {

    private static final String TYPE_PUBLIC = "public";
    private static final String TYPE_CONFIDENTIAL = "confidential";

    private static final int DEFAULT_ACCESS_TOKEN_TTL  = 300;
    private static final int DEFAULT_REFRESH_TOKEN_TTL = 604800;
    private static final int DEFAULT_ID_TOKEN_TTL      = 3600;

    private static final List<String> DEFAULT_PUBLIC_SCOPES = List.of("openid", "email", "profile");

    private final AuthClientRepository clientRepository;
    private final AuthClientOauthRepository oauthRepository;
    private final AuthApplicationRepository appRepository;
    private final AuthRepository authRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppEventProducer appEventProducer;

    @Override
    public Page<ClientListResponse> getClients(
            String grantType, Long appId, String status, String clientId, Pageable pageable) {
        Integer enabled = null;
        if ("ENABLED".equalsIgnoreCase(status))   enabled = 1;
        else if ("DISABLED".equalsIgnoreCase(status)) enabled = 0;
        return authRepository.getClients(grantType, appId, enabled, clientId, pageable);
    }

    @Override
    public ClientDetailResponse getClientDetail(String clientId) {
        List<AuthClient> clients = clientRepository.findByClientId(clientId);
        if (clients.isEmpty()) {
            throw new BusinessException(ErrorCode.CLIENT_NOT_FOUND, "Không tìm thấy client '" + clientId + "'");
        }
        if (clients.size() > 1) {
            throw new BusinessException(ErrorCode.UNKNOWN, "Dữ liệu CLIENT_ID bị trùng lặp: " + clientId);
        }
        AuthClient client = clients.get(0);

        AuthClientOauth oauth = oauthRepository.findByClientId(client.getId()).orElse(null);

        AuthApplication app = client.getAppId() != null
                ? appRepository.findById(client.getAppId()).orElse(null)
                : null;

        return new ClientDetailResponse(client, oauth, app);
    }

    @Override
    @Transactional
    public CreateClientResponse createClient(CreateClientRequest request) {
        String type = request.getType().toLowerCase();
        if (!TYPE_PUBLIC.equals(type) && !TYPE_CONFIDENTIAL.equals(type)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "type phải là 'public' hoặc 'confidential'");
        }

        if (clientRepository.countByClientId(request.getClientId()) > 0) {
            throw new BusinessException(ErrorCode.CLIENT_ALREADY_EXISTS,
                    "clientId '" + request.getClientId() + "' đã tồn tại");
        }

        AuthApplication app = appRepository.findById(request.getAppId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Không tìm thấy application với id=" + request.getAppId()));
        if (!AuthApplication.STATUS.ACTIVE.equals(app.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Application đang INACTIVE, không thể tạo client");
        }

        List<String> grantTypes = request.getGrantTypes();
        boolean hasAuthCode        = grantTypes.contains("authorization_code");
        boolean hasClientCreds     = grantTypes.contains("client_credentials");

        // PUBLIC không được dùng client_credentials (không có secret)
        if (TYPE_PUBLIC.equals(type) && hasClientCreds) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Client PUBLIC không hỗ trợ grant type 'client_credentials'");
        }
        if (TYPE_PUBLIC.equals(type) && !hasAuthCode) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Client PUBLIC phải có grant type 'authorization_code'");
        }

        // authorization_code bất kể PUBLIC hay CONFIDENTIAL → redirectUris bắt buộc
        if (hasAuthCode && (request.getRedirectUris() == null || request.getRedirectUris().isBlank())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "redirectUris là bắt buộc khi có grant type 'authorization_code'");
        }

        String rawSecret    = null;
        String hashedSecret = null;
        String tokenEndpointAuth;
        String redirectUris;
        String allowedScopes;
        Integer requirePkce;
        Integer requireConsent;
        String  postLogoutRedirect;

        if (TYPE_PUBLIC.equals(type)) {
            tokenEndpointAuth  = "none";
            requirePkce        = 1;
            redirectUris       = request.getRedirectUris();
            requireConsent     = boolToInt(request.getRequireConsent(), 0);
            postLogoutRedirect = request.getPostLogoutRedirect();

        } else {
            // CONFIDENTIAL: tokenEndpointAuth bắt buộc
            String authMethod = request.getTokenEndpointAuth();
            if (authMethod == null || authMethod.isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "tokenEndpointAuth là bắt buộc với client CONFIDENTIAL");
            }
            if (!"client_secret_basic".equals(authMethod) && !"client_secret_post".equals(authMethod)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "tokenEndpointAuth phải là 'client_secret_basic' hoặc 'client_secret_post'");
            }
            rawSecret          = UUID.randomUUID().toString();
            hashedSecret       = passwordEncoder.encode(rawSecret);
            tokenEndpointAuth  = authMethod;
            requirePkce        = boolToInt(request.getRequirePkce(), 0);
            // hybrid: authorization_code có redirectUris, client_credentials-only thì ""
            redirectUris       = hasAuthCode ? request.getRedirectUris() : "";
            requireConsent     = hasAuthCode ? boolToInt(request.getRequireConsent(), 0) : 0;
            postLogoutRedirect = hasAuthCode ? request.getPostLogoutRedirect() : null;
        }

        // Scopes
        List<String> scopeList = request.getScopes();
        if (scopeList == null || scopeList.isEmpty()) {
            allowedScopes = String.join(" ", DEFAULT_PUBLIC_SCOPES);
        } else {
            allowedScopes = String.join(" ", scopeList);
        }

        // TTL
        int     accessTokenTtl  = request.getAccessTokenTtl()  != null ? request.getAccessTokenTtl()  : DEFAULT_ACCESS_TOKEN_TTL;
        Integer refreshTokenTtl = request.getRefreshTokenTtl() != null ? request.getRefreshTokenTtl() : DEFAULT_REFRESH_TOKEN_TTL;
        Integer idTokenTtl      = request.getIdTokenTtl()      != null ? request.getIdTokenTtl()      : DEFAULT_ID_TOKEN_TTL;

        AuthClient client = AuthClient.builder()
                .clientId(request.getClientId())
                .clientSecret(hashedSecret)
                .name(request.getName())
                .clientType(type)
                .defaultUrl(request.getDefaultUrl())
                .enabled(1)
                .logoUri(request.getLogoUri())
                .description(request.getDescription())
                .appId(request.getAppId())
                .build();

        AuthClient saved = clientRepository.save(client);

        AuthClientOauth oauth = AuthClientOauth.builder()
                .clientId(saved.getId())
                .grantTypes(String.join(",", grantTypes))
                .redirectUris(redirectUris)
                .allowedScopes(allowedScopes)
                .accessTokenTtl(accessTokenTtl)
                .refreshTokenTtl(refreshTokenTtl)
                .idTokenTtl(idTokenTtl)
                .tokenEndpointAuth(tokenEndpointAuth)
                .requirePkce(requirePkce)
                .requireConsent(requireConsent)
                .postLogoutRedirect(postLogoutRedirect)
                .build();

        oauthRepository.save(oauth);

        return new CreateClientResponse(saved, rawSecret);
    }

    @Override
    @Transactional
    public ClientDetailResponse updateClient(Long id, UpdateClientRequest request) {
        AuthClient client = clientRepository.findByNumericId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND,
                        "Không tìm thấy client với id=" + id));

        AuthClientOauth oauth = oauthRepository.findByClientId(client.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND,
                        "Không tìm thấy OAuth config cho client id=" + id));

        String type = client.getClientType();

        // grantTypes: ADD only
        if (request.getGrantTypes() != null && !request.getGrantTypes().isEmpty()) {
            Set<String> merged = new LinkedHashSet<>();
            if (oauth.getGrantTypes() != null) {
                merged.addAll(List.of(oauth.getGrantTypes().split(",")));
            }
            merged.addAll(request.getGrantTypes());

            if (TYPE_PUBLIC.equals(type) && merged.contains("client_credentials")) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Client PUBLIC không hỗ trợ grant type 'client_credentials'");
            }
            oauth.setGrantTypes(String.join(",", merged));
        }

        boolean hasAuthCode = oauth.getGrantTypes() != null
                && oauth.getGrantTypes().contains("authorization_code");

        // redirectUris: required khi có authorization_code
        if (hasAuthCode) {
            if (request.getRedirectUris() == null || request.getRedirectUris().isBlank()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "redirectUris là bắt buộc khi có grant type 'authorization_code'");
            }
            oauth.setRedirectUris(request.getRedirectUris());
        }

        // tokenEndpointAuth: CONFIDENTIAL only, null = giữ nguyên
        if (TYPE_CONFIDENTIAL.equals(type)
                && request.getTokenEndpointAuth() != null
                && !request.getTokenEndpointAuth().isBlank()) {
            String authMethod = request.getTokenEndpointAuth();
            if (!"client_secret_basic".equals(authMethod) && !"client_secret_post".equals(authMethod)) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "tokenEndpointAuth phải là 'client_secret_basic' hoặc 'client_secret_post'");
            }
            oauth.setTokenEndpointAuth(authMethod);
        }

        // AUTH_CLIENT fields
        client.setName(request.getName());
        client.setLogoUri(request.getLogoUri());
        client.setDescription(request.getDescription());
        client.setDefaultUrl(request.getDefaultUrl());
        if (request.getEnabled() != null) {
            client.setEnabled(request.getEnabled() ? 1 : 0);
        }

        // TTL
        oauth.setAccessTokenTtl(request.getAccessTokenTtl() != null
                ? request.getAccessTokenTtl() : DEFAULT_ACCESS_TOKEN_TTL);

        if (hasAuthCode) {
            oauth.setRefreshTokenTtl(request.getRefreshTokenTtl() != null
                    ? request.getRefreshTokenTtl() : DEFAULT_REFRESH_TOKEN_TTL);
            oauth.setIdTokenTtl(request.getIdTokenTtl() != null
                    ? request.getIdTokenTtl() : DEFAULT_ID_TOKEN_TTL);
            oauth.setPostLogoutRedirect(request.getPostLogoutRedirect());
        }

        // requirePkce: PUBLIC force=1, CONFIDENTIAL từ request
        if (TYPE_PUBLIC.equals(type)) {
            oauth.setRequirePkce(1);
        } else if (request.getRequirePkce() != null) {
            oauth.setRequirePkce(request.getRequirePkce() ? 1 : 0);
        }

        // requireConsent: commented out
        // oauth.setRequireConsent(boolToInt(request.getRequireConsent(), ...));

        clientRepository.save(client);
        oauthRepository.save(oauth);

        AuthApplication app = client.getAppId() != null
                ? appRepository.findById(client.getAppId()).orElse(null)
                : null;

        return new ClientDetailResponse(client, oauth, app);
    }

    @Override
    @Transactional
    public CreateClientResponse resetSecret(Long id) {
        AuthClient client = clientRepository.findByNumericId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND,
                        "Không tìm thấy client với id=" + id));

        if (!Integer.valueOf(1).equals(client.getEnabled())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Client đang bị vô hiệu hóa, không thể reset secret");
        }

        if (client.getClientSecret() == null || client.getClientSecret().isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Client hiện tại không có secret, không thể reset");
        }

        String rawSecret    = UUID.randomUUID().toString();
        String hashedSecret = passwordEncoder.encode(rawSecret);
        client.setClientSecret(hashedSecret);
        clientRepository.save(client);

        ClientSecretResetPayload payload = ClientSecretResetPayload.builder()
                .clientNumericId(client.getId())
                .clientId(client.getClientId())
                .clientType(client.getClientType())
                .appId(client.getAppId())
                .name(client.getName())
                .resetAt(Instant.now())
                .build();

        appEventProducer.publish(KafkaConfig.TOPIC_CLIENT_SECRET_RESET_NOTIFY,
                "CLIENT_SECRET_RESET", payload);

        return new CreateClientResponse(client, rawSecret);
    }

    @Override
    @Transactional
    public ClientDetailResponse updateScopes(Long id, ScopeRequest request) {
        String action = request.getAction().toLowerCase();
        if (!"add".equals(action) && !"delete".equals(action) && !"clear".equals(action)) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "action phải là 'add', 'delete' hoặc 'clear'");
        }

        if (!"clear".equals(action)) {
            List<String> scopes = request.getScopes();
            if (scopes == null || scopes.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "scopes không được để trống với action '" + action + "'");
            }
            if (scopes.size() != new HashSet<>(scopes).size()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "scopes trong request bị trùng lặp");
            }
        }

        AuthClient client = clientRepository.findByNumericId(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND,
                        "Không tìm thấy client với id=" + id));

        AuthClientOauth oauth = oauthRepository.findByClientId(client.getId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND,
                        "Không tìm thấy OAuth config cho client id=" + id));

        Set<String> existingSet = new LinkedHashSet<>();
        if (oauth.getAllowedScopes() != null && !oauth.getAllowedScopes().isBlank()) {
            existingSet.addAll(Arrays.asList(oauth.getAllowedScopes().split(" ")));
        }

        List<String> warnings = null;

        if ("add".equals(action)) {
            warnings = new ArrayList<>();
            for (String scope : request.getScopes()) {
                if (existingSet.contains(scope)) {
                    warnings.add(scope + " đã tồn tại, bỏ qua");
                } else {
                    existingSet.add(scope);
                }
            }
        } else if ("delete".equals(action)) {
            for (String scope : request.getScopes()) {
                existingSet.remove(scope);
            }
        } else {
            existingSet.clear();
        }

        if (existingSet.isEmpty()) {
            existingSet.addAll(DEFAULT_PUBLIC_SCOPES);
        }

        oauth.setAllowedScopes(String.join(" ", existingSet));
        oauthRepository.save(oauth);

        AuthApplication app = client.getAppId() != null
                ? appRepository.findById(client.getAppId()).orElse(null)
                : null;

        return new ClientDetailResponse(client, oauth, app, warnings);
    }

    private static Integer boolToInt(Boolean value, int defaultVal) {
        if (value == null) return defaultVal;
        return value ? 1 : 0;
    }
}
