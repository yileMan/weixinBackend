package com.man.backend.auth.controller;

import com.man.backend.auth.dto.OpenidCodeRequest;
import com.man.backend.user.model.AppUser;
import com.man.backend.auth.service.WechatAuthService;
import com.man.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final WechatAuthService wechatAuthService;
    private final UserService userService;

    public AuthController(WechatAuthService wechatAuthService, UserService userService) {
        this.wechatAuthService = wechatAuthService;
        this.userService = userService;
    }

    @PostMapping("/openid")
    public ResponseEntity<Map<String, Object>> exchangeOpenid(@RequestBody OpenidCodeRequest request) {
        String code = request == null ? null : request.getCode();
        log.info("POST /api/auth/openid codeLength={}", code == null ? 0 : code.length());

        String openid = wechatAuthService.exchangeCodeForOpenid(request == null ? null : request.getCode());
        AppUser user = userService.getOrCreateByOpenid(openid);
        log.info("POST /api/auth/openid success openid={}", maskOpenid(openid));

        return ResponseEntity.ok(Map.of(
                "openid", openid,
                "userId", user.getUserId(),
                "nickname", user.getNickname(),
                "data", Map.of(
                        "openid", openid,
                        "userId", user.getUserId(),
                        "nickname", user.getNickname()
                )
        ));
    }

    private String maskOpenid(String openid) {
        if (openid == null || openid.isBlank()) {
            return "blank";
        }
        String trimmed = openid.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return trimmed.substring(0, 3) + "***" + trimmed.substring(trimmed.length() - 3);
    }
}
