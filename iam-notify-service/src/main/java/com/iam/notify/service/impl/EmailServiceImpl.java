package com.iam.notify.service.impl;

import java.io.UnsupportedEncodingException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import com.iam.notify.domain.AuthUserProfile;
import com.iam.notify.kafka.payload.PasswordChangedPayload;
import com.iam.notify.kafka.payload.PermissionApprovedPayload;
import com.iam.notify.kafka.payload.PermissionRequestPayload;
import com.iam.notify.kafka.payload.UserCreatedPayload;
import com.iam.notify.repository.AuthUserProfileRepository;
import com.iam.notify.service.EmailService;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;
    private final AuthUserProfileRepository userProfileRepository;

    @Value("${notify.mail.from}")
    private String mailFrom;

    @Value("${notify.mail.from-name}")
    private String mailFromName;

    @Value("${notify.mail.portal-url}")
    private String portalUrl;

    /**
     * Mỗi phương thức public theo cấu trúc 3 bước:
     *   1. BUILD CONTEXT — tạo Thymeleaf Context, đưa vào các biến mà template HTML cần
     *      (ví dụ: fullName, username, portalUrl, link approve, ...).
     *   2. GỌI send() — truyền địa chỉ nhận, subject, tên template (tương ứng file
     *      src/main/resources/templates/<template>.html), và context vừa build.
     *   3. TRONG send() — TemplateEngine render HTML từ template + context, JavaMailSender
     *      đóng gói thành MimeMessage (UTF-8, multipart), đính kèm logo inline (CID "logo"),
     *      và gửi qua SMTP Gmail. Nếu địa chỉ nhận khác CC_ADMIN thì tự động CC thêm
     *      CC_ADMIN để theo dõi luồng email trong quá trình phát triển.
     */

    public static final String CC_ADMIN = "vudinhcuong8404@gmail.com";

    // ── User lifecycle ────────────────────────────────────────────────────────

    @Override
    public void sendUserCreatedEmail(UserCreatedPayload p) {
        // Tài khoản mới chưa có email công việc → dùng EMAIL_PERSONAL từ AUTH_USER_PROFILE
        // Nếu không có → fallback CC_ADMIN để không bỏ sót thông báo
        String toEmail = userProfileRepository.findById(p.getUserId())
                .map(AuthUserProfile::getEmailPersonal)
                .filter(e -> e != null && !e.isBlank())
                .orElseGet(() -> {
                    log.warn("sendUserCreatedEmail: emailPersonal not found for userId={}, fallback to CC_ADMIN",
                            p.getUserId());
                    return CC_ADMIN;
                });

        Context ctx = new Context();
        ctx.setVariable("fullName", p.getFullName());
        ctx.setVariable("username", p.getUsername());
        ctx.setVariable("tempPassword", p.getTempPassword());
        ctx.setVariable("joinDate", p.getJoinDate());
        ctx.setVariable("hasChangeLink", p.getChangePasswordLink() != null);
        ctx.setVariable("changePasswordLink",
                p.getChangePasswordLink() != null ? p.getChangePasswordLink() : portalUrl);
        ctx.setVariable("portalUrl", portalUrl);
        send(toEmail, "[IAM] Tài khoản của bạn đã được tạo", "user-created", ctx);
    }

    // ── Password ──────────────────────────────────────────────────────────────

    @Override
    public void sendPasswordChangedEmail(PasswordChangedPayload p) {
        Context ctx = new Context();
        ctx.setVariable("username", p.getUsername());
        ctx.setVariable("portalUrl", portalUrl);
        send(p.getEmail(), "[IAM] Mật khẩu tài khoản đã được thay đổi", "password-changed", ctx);
    }

    @Override
    public void sendPasswordResetEmail(PasswordChangedPayload p) {
        Context ctx = new Context();
        ctx.setVariable("username", p.getUsername());
        ctx.setVariable("newPassword", p.getPassword());
        ctx.setVariable("portalUrl", portalUrl);
        send(p.getEmail(), "[IAM] Mật khẩu tài khoản đã được đặt lại", "password-reset", ctx);
    }

    // ── Permission request ────────────────────────────────────────────────────

    @Override
    public void sendPermissionRequestEmail(PermissionRequestPayload p, String reviewerEmail) {
        Context ctx = new Context();
        ctx.setVariable("requestId", p.getRequestId());
        ctx.setVariable("requesterCode", p.getRequesterCode());
        ctx.setVariable("granteeCode", p.getGranteeCode());
        ctx.setVariable("reason", p.getReason());
        ctx.setVariable("reviewLink",
                portalUrl + "/users/permissions/detail?requestId=" + p.getRequestId() + "&mode=approve");
        ctx.setVariable("portalUrl", portalUrl);
        send(reviewerEmail,
                "[IAM] Yêu cầu cấp quyền #" + p.getRequestId() + " cần xem xét",
                "permission-request", ctx);
    }

    @Override
    public void sendPermissionSubmittedEmail(PermissionRequestPayload p, String requesterEmail) {
        Context ctx = new Context();
        ctx.setVariable("requestId", p.getRequestId());
        ctx.setVariable("requesterCode", p.getRequesterCode());
        ctx.setVariable("reason", p.getReason());
        ctx.setVariable("viewLink",
                portalUrl + "/users/permissions/detail?requestId=" + p.getRequestId());
        ctx.setVariable("portalUrl", portalUrl);
        send(requesterEmail,
                "[IAM] Yêu cầu cấp quyền #" + p.getRequestId() + " đã được gửi",
                "permission-submitted", ctx);
    }

    @Override
    public void sendPermissionApprovedEmail(PermissionApprovedPayload p, String requesterEmail) {
        boolean approved = "APPROVED".equalsIgnoreCase(p.getAction());
        Context ctx = new Context();
        ctx.setVariable("requestId", p.getRequestId());
        ctx.setVariable("requestedBy", p.getRequestedBy());
        ctx.setVariable("reviewedBy", p.getReviewedBy());
        ctx.setVariable("approved", approved);
        ctx.setVariable("portalUrl", portalUrl);
        String subject = approved
                ? "[IAM] Yêu cầu #" + p.getRequestId() + " đã được phê duyệt"
                : "[IAM] Yêu cầu #" + p.getRequestId() + " đã bị từ chối";
        send(requesterEmail, subject, "permission-approved", ctx);
    }

    // ── Internal helper ───────────────────────────────────────────────────────

    private void send(String to, String subject, String template, Context ctx) {
        if (to == null || to.isBlank()) {
            log.warn("Skipping email — recipient is null. subject={}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(new InternetAddress(mailFrom, mailFromName));
            helper.setTo(to);
            if (!CC_ADMIN.equalsIgnoreCase(to)) {
                helper.setCc(CC_ADMIN);
            }
            helper.setSubject(subject);

            String html = templateEngine.process(template, ctx);
            helper.setText(html, true);

            ClassPathResource logo = new ClassPathResource("static/img/logo.png");
            if (logo.exists()) {
                helper.addInline("logo", logo);
            }

            mailSender.send(message);
            log.info("Email sent to={} subject={}", to, subject);
        } catch (MessagingException | UnsupportedEncodingException e) {
            log.error("Failed to send email to={} subject={}: {}", to, subject, e.getMessage(), e);
        }
    }
}
