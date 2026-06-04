package com.iam.auth.controller;

import com.iam.auth.enums.ErrorCode;
import com.iam.auth.utils.SessionTokenUtil;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@Controller
@RequestMapping("/ms-internal-iam/auth/internal")
public class PageController {

    private final String tokenSecret;

    public PageController(@Value("${session.token.secret}") String tokenSecret) {
        this.tokenSecret = tokenSecret;
    }

    /**
     * Renders an MFA step page.
     *
     * <p>On every render a fresh single-use {@code sessionToken} is derived via HMAC from the
     * {@code authSessionId} stored in HttpSession. Only {@code sessionToken} is placed in the
     * Thymeleaf model — the real {@code authSessionId} never leaves the server.
     */
    @GetMapping("/login-action")
    public String login(
            @RequestParam(name = "client-id") Long clientId,
            @RequestParam(name = "theme", required = false, defaultValue = "default") String theme,
            @RequestParam(name = "action-type", required = true) String actionType,
            HttpSession session,
            Model model
    ) {
        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) session.getAttribute("auth:ui:context");

        if (data != null) {
            model.addAttribute("availableMethods", data.get("availableMethods"));
            model.addAttribute("challengeInfo",    data.get("challengeInfo"));

            // Generate a new single-use sessionToken bound to this session and timestamp
            String authSessionId = (String) data.get("authSessionId");
            if (authSessionId != null) {
                String jSessionId   = session.getId();
                long   timestamp    = System.currentTimeMillis();
                String sessionToken = SessionTokenUtil.generate(tokenSecret, authSessionId, jSessionId, timestamp);

                // Persist the token + timestamp so AuthController can reconstruct and verify the HMAC
                data.put("sessionToken",    sessionToken);
                data.put("tokenTimestamp",  timestamp);
                session.setAttribute("auth:ui:context", data);

                model.addAttribute("sessionToken", sessionToken);
            } else {
                model.addAttribute("sessionToken", null);
            }

            // Pass through error attributes (populated by GlobalExceptionHandler via FlashMap)
            if (!model.containsAttribute("errorCode") && data.containsKey("errorCode")) {
                model.addAttribute("errorCode", data.get("errorCode"));
                model.addAttribute("errorDesc", data.get("errorDesc"));
                data.remove("errorCode");
                data.remove("errorDesc");
                session.setAttribute("auth:ui:context", data);
            }
        }

        if (!model.containsAttribute("sessionToken")) {
            model.addAttribute("sessionToken", null);
        }

        model.addAttribute("clientId", clientId);
        model.addAttribute("type",  actionType.toLowerCase());
        model.addAttribute("theme", theme);
        return theme + "/" + actionType.toLowerCase() + "/index";
        }

    @GetMapping("/error")
    public String errorPage(
            @RequestParam(value = "code", defaultValue = "400") String code,
            Model model) {
        String imgPath = switch (code) {
            case "403" -> "/template/default/error/img/403.png";
            case "404" -> "/template/default/error/img/404.png";
            default    -> ErrorCode.BAD_REQUEST_PAGE.getDesc();
        };
        model.addAttribute("errorImg", imgPath);
        return "default/error/error";
    }
}