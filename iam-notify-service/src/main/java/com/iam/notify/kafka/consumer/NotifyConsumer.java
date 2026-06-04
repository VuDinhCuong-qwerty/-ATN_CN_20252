package com.iam.notify.kafka.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iam.notify.config.KafkaConfig;
import com.iam.notify.kafka.event.BaseEvent;
import com.iam.notify.kafka.payload.PasswordChangedPayload;
import com.iam.notify.kafka.payload.PermissionApprovedPayload;
import com.iam.notify.kafka.payload.PermissionRequestPayload;
import com.iam.notify.kafka.payload.UserCreatedPayload;
import com.iam.notify.repository.UserEmailRepository;
import com.iam.notify.service.EmailService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotifyConsumer {

    private final EmailService emailService;
    private final ObjectMapper objectMapper;
    private final UserEmailRepository userEmailRepository;

    // ── 1. Tạo tài khoản mới ─────────────────────────────────────────────────

    @KafkaListener(topics = KafkaConfig.TOPIC_USER_CREATED)
    public void handleUserCreated(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<UserCreatedPayload> event = objectMapper.readValue(
                    record.value(), new TypeReference<BaseEvent<UserCreatedPayload>>() {});
            UserCreatedPayload payload = event.getPayload();

            if (payload == null || payload.getUserId() == null) {
                log.warn("UserCreated: missing userId — skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            emailService.sendUserCreatedEmail(payload);
            log.info("UserCreated email sent for userId={} username={}", payload.getUserId(), payload.getUsername());
        } catch (Exception e) {
            log.error("handleUserCreated failed offset={} partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    // ── 2+3. Đổi / Reset mật khẩu — phân biệt theo eventType ─────────────────

    @KafkaListener(topics = KafkaConfig.TOPIC_PASSWORD_CHANGED)
    public void handlePasswordChanged(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<PasswordChangedPayload> event = objectMapper.readValue(
                    record.value(), new TypeReference<BaseEvent<PasswordChangedPayload>>() {});
            PasswordChangedPayload payload = event.getPayload();

            if (payload == null || payload.getEmail() == null) {
                log.warn("PasswordChanged: missing email — skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            if ("RESET".equals(payload.getEventType())) {
                // Admin đặt lại mật khẩu → gửi mật khẩu mới cho user
                emailService.sendPasswordResetEmail(payload);
                log.info("PasswordReset email sent to={} username={}", payload.getEmail(), payload.getUsername());
            } else {
                // User tự đổi mật khẩu → cảnh báo bảo mật
                emailService.sendPasswordChangedEmail(payload);
                log.info("PasswordChanged email sent to={} username={}", payload.getEmail(), payload.getUsername());
            }
        } catch (Exception e) {
            log.error("handlePasswordChanged failed offset={} partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    // ── 4. Yêu cầu cấp quyền — gửi 2 email (reviewer + requester) ────────────

    @KafkaListener(topics = KafkaConfig.TOPIC_PERMISSION_REQUEST)
    public void handlePermissionRequest(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<PermissionRequestPayload> event = objectMapper.readValue(
                    record.value(), new TypeReference<BaseEvent<PermissionRequestPayload>>() {});
            PermissionRequestPayload payload = event.getPayload();

            if (payload == null) {
                log.warn("PermissionRequest: null payload — skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            // Gửi email đến reviewer (CAB) — link approve/reject
            String reviewerEmail = userEmailRepository.findEmailByEmployeeCode(payload.getReviewerCode());
            if (reviewerEmail != null) {
                emailService.sendPermissionRequestEmail(payload, reviewerEmail);
                log.info("PermissionRequest email sent to reviewer={} requestId={}",
                        reviewerEmail, payload.getRequestId());
            } else {
                log.warn("PermissionRequest: reviewer email not found for code={}", payload.getReviewerCode());
            }

            // Gửi email đến requester — xác nhận đã gửi + link xem chi tiết
            String requesterEmail = userEmailRepository.findEmailByEmployeeCode(payload.getRequesterCode());
            if (requesterEmail != null) {
                emailService.sendPermissionSubmittedEmail(payload, requesterEmail);
                log.info("PermissionSubmitted email sent to requester={} requestId={}",
                        requesterEmail, payload.getRequestId());
            } else {
                log.warn("PermissionRequest: requester email not found for code={}", payload.getRequesterCode());
            }
        } catch (Exception e) {
            log.error("handlePermissionRequest failed offset={} partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }

    // ── 5. Kết quả duyệt — phân biệt APPROVED / REJECTED theo eventType ───────

    @KafkaListener(topics = KafkaConfig.TOPIC_PERMISSION_APPROVED)
    public void handlePermissionApproved(ConsumerRecord<String, String> record, Acknowledgment ack) {
        try {
            BaseEvent<PermissionApprovedPayload> event = objectMapper.readValue(
                    record.value(), new TypeReference<BaseEvent<PermissionApprovedPayload>>() {});
            PermissionApprovedPayload payload = event.getPayload();

            if (payload == null) {
                log.warn("PermissionApproved: null payload — skipping. eventId={}", event.getEventId());
                ack.acknowledge();
                return;
            }

            // Derive action từ eventType (identity publish 2 eventType khác nhau)
            String action = "PERMISSION_REQUEST_APPROVED".equals(event.getEventType())
                    ? "APPROVED" : "REJECTED";
            payload.setAction(action);

            String requesterEmail = userEmailRepository.findEmailByEmployeeCode(payload.getRequestedBy());
            if (requesterEmail != null) {
                emailService.sendPermissionApprovedEmail(payload, requesterEmail);
                log.info("PermissionApproved email sent to={} action={} requestId={}",
                        requesterEmail, action, payload.getRequestId());
            } else {
                log.warn("PermissionApproved: requester email not found for code={}", payload.getRequestedBy());
            }
        } catch (Exception e) {
            log.error("handlePermissionApproved failed offset={} partition={}: {}",
                    record.offset(), record.partition(), e.getMessage(), e);
        } finally {
            ack.acknowledge();
        }
    }
}
