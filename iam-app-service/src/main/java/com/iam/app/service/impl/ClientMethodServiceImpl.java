package com.iam.app.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.app.domain.AuthClientMethod;
import com.iam.app.domain.AuthMethod;
import com.iam.app.dto.config.MethodConfig;
import com.iam.app.dto.config.OtpEmailConfig;
import com.iam.app.dto.config.UsernamePasswordConfig;
import com.iam.app.dto.request.CreateClientMethodRequest;
import com.iam.app.dto.request.UpdateClientMethodRequest;
import com.iam.app.dto.response.ClientMethodResponse;
import com.iam.app.enums.ErrorCode;
import com.iam.app.exception.BusinessException;
import com.iam.app.repository.jpa.AuthApplicationRepository;
import com.iam.app.repository.jpa.AuthClientMethodRepository;
import com.iam.app.repository.jpa.AuthFlowExecutionRepository;
import com.iam.app.repository.jpa.AuthMethodRepository;
import com.iam.app.service.ClientMethodService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientMethodServiceImpl implements ClientMethodService {

    private final AuthApplicationRepository appRepository;
    private final AuthMethodRepository methodRepository;
    private final AuthClientMethodRepository clientMethodRepository;
    private final AuthFlowExecutionRepository flowExecutionRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    @Override
    public List<ClientMethodResponse> getMethods(Long appId) {
        appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        List<AuthClientMethod> entities = clientMethodRepository.findByAppId(appId);
        List<ClientMethodResponse> result = new ArrayList<>();

        for (AuthClientMethod entity : entities) {
            AuthMethod method = methodRepository.findById(entity.getMethodId()).orElse(null);
            String methodName = method != null ? method.getMethod() : null;
            Object parsedConfig = parseConfigToObject(methodName, entity.getConfig());
            result.add(new ClientMethodResponse(entity, methodName, parsedConfig));
        }

        return result;
    }

    @Override
    @Transactional
    public List<ClientMethodResponse> createMethods(Long appId, CreateClientMethodRequest request) {
        appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        // Kiểm tra trùng methodId trong cùng một request
        Set<Long> seen = new HashSet<>();
        for (CreateClientMethodRequest.Item item : request.getItems()) {
            if (!seen.add(item.getMethodId())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "methodId " + item.getMethodId() + " bị trùng trong cùng một request");
            }
        }

        // Validate toàn bộ trước khi lưu bất kỳ record nào
        List<AuthClientMethod> toSave = new ArrayList<>();
        List<Map.Entry<AuthClientMethod, String>> methodMeta = new ArrayList<>();

        for (CreateClientMethodRequest.Item item : request.getItems()) {
            AuthMethod authMethod = methodRepository.findById(item.getMethodId())
                    .orElseThrow(() -> new BusinessException(ErrorCode.VALIDATION_FAILED,
                            "Method ID " + item.getMethodId() + " không tồn tại"));

            if (!authMethod.getMethod().equals(item.getMethodName())) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "methodName '" + item.getMethodName() + "' không khớp với methodId "
                                + item.getMethodId() + " (expected: " + authMethod.getMethod() + ")");
            }

            if (clientMethodRepository.countByAppIdAndMethodId(appId, item.getMethodId()) > 0) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Method '" + authMethod.getMethod() + "' đã được cấu hình cho ứng dụng này");
            }

            MethodConfig config = buildAndValidateConfig(authMethod.getMethod(), item.getConfig());
            String configJson = serializeConfig(config);

            AuthClientMethod entity = AuthClientMethod.builder()
                    .appId(appId)
                    .methodId(item.getMethodId())
                    .config(configJson)
                    .status("ACTIVE")
                    .build();

            toSave.add(entity);
            methodMeta.add(Map.entry(entity, authMethod.getMethod()));
        }

        List<AuthClientMethod> saved = clientMethodRepository.saveAll(toSave);
        log.info("Created {} client method(s) for appId={}", saved.size(), appId);

        // Build response — map theo index vì saveAll giữ nguyên thứ tự
        List<ClientMethodResponse> result = new ArrayList<>();
        for (int i = 0; i < saved.size(); i++) {
            String methodName = methodMeta.get(i).getValue();
            Object parsedConfig = parseConfigToObject(methodName, saved.get(i).getConfig());
            result.add(new ClientMethodResponse(saved.get(i), methodName, parsedConfig));
        }

        return result;
    }

    @Override
    @Transactional
    public ClientMethodResponse updateMethod(Long appId, Long methodId, UpdateClientMethodRequest request) {
        appRepository.findById(appId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Ứng dụng không tồn tại"));

        AuthClientMethod entity = clientMethodRepository.findByAppIdAndMethodId(appId, methodId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND,
                        "Method chưa được cấu hình cho ứng dụng này"));

        // Guard: ACTIVE -> INACTIVE — kiểm tra conflict với flow execution
        if ("INACTIVE".equals(request.getStatus()) && "ACTIVE".equals(entity.getStatus())) {
            List<String> conflictFlows = flowExecutionRepository
                    .findActiveFlowAliasesByClientMethodAndApp(entity.getId(), appId);
            if (!conflictFlows.isEmpty()) {
                throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                        "Method đang được sử dụng trong các luồng xác thực: "
                                + String.join(", ", conflictFlows));
            }
        }

        // Validate status value nếu được truyền
        if (request.getStatus() != null
                && !"ACTIVE".equals(request.getStatus())
                && !"INACTIVE".equals(request.getStatus())) {
            throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "status không hợp lệ, chỉ chấp nhận ACTIVE hoặc INACTIVE");
        }

        // Cập nhật config nếu được truyền
        if (request.getConfig() != null) {
            AuthMethod authMethod = methodRepository.findById(methodId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Method không tồn tại"));
            MethodConfig config = buildAndValidateConfig(authMethod.getMethod(), request.getConfig());
            entity.setConfig(serializeConfig(config));
        }

        // Cập nhật status nếu được truyền
        if (request.getStatus() != null) {
            entity.setStatus(request.getStatus());
        }

        AuthClientMethod saved = clientMethodRepository.save(entity);
        log.info("Updated client method: appId={}, methodId={}", appId, methodId);

        AuthMethod authMethod = methodRepository.findById(methodId).orElse(null);
        String methodName = authMethod != null ? authMethod.getMethod() : null;
        Object parsedConfig = parseConfigToObject(methodName, saved.getConfig());
        return new ClientMethodResponse(saved, methodName, parsedConfig);
    }

    private MethodConfig buildAndValidateConfig(String methodName, Map<String, Object> configMap) {
        MethodConfig config = switch (methodName) {
            case AuthMethod.METHOD.USERNAME_PASSWORD ->
                    configMap != null
                            ? objectMapper.convertValue(configMap, UsernamePasswordConfig.class)
                            : new UsernamePasswordConfig();
            case AuthMethod.METHOD.OTP_EMAIL ->
                    configMap != null
                            ? objectMapper.convertValue(configMap, OtpEmailConfig.class)
                            : new OtpEmailConfig();
            default -> throw new BusinessException(ErrorCode.VALIDATION_FAILED,
                    "Method '" + methodName + "' chưa được hỗ trợ cấu hình");
        };

        config.applyDefaults();

        Set<ConstraintViolation<MethodConfig>> violations = validator.validate(config);
        if (!violations.isEmpty()) {
            String message = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .findFirst()
                    .orElse("Cấu hình không hợp lệ");
            throw new BusinessException(ErrorCode.VALIDATION_FAILED, message);
        }

        return config;
    }

    private String serializeConfig(MethodConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNKNOWN, "Lỗi khi xử lý cấu hình");
        }
    }

    private Object parseConfigToObject(String methodName, String configJson) {
        if (configJson == null || methodName == null) return null;
        try {
            return switch (methodName) {
                case AuthMethod.METHOD.USERNAME_PASSWORD ->
                        objectMapper.readValue(configJson, UsernamePasswordConfig.class);
                case AuthMethod.METHOD.OTP_EMAIL ->
                        objectMapper.readValue(configJson, OtpEmailConfig.class);
                default -> objectMapper.readValue(configJson, Map.class);
            };
        } catch (Exception e) {
            log.warn("Không thể parse config cho method {}: {}", methodName, e.getMessage());
            return configJson;
        }
    }
}
