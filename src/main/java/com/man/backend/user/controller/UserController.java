package com.man.backend.user.controller;

import com.man.backend.user.dto.UserProfileUpdateRequest;
import com.man.backend.user.model.AppUser;
import com.man.backend.user.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private static final Logger log = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/profile")
    public ResponseEntity<Map<String, Object>> getProfile(@RequestParam String openid) {
        log.info("GET /api/users/profile openid={}", maskOpenid(openid));
        AppUser user = userService.getProfileByOpenid(openid);
        return ResponseEntity.ok(toProfileBody(user));
    }

    @PutMapping("/profile")
    public ResponseEntity<Map<String, Object>> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        log.info("PUT /api/users/profile openid={}, nickname={}",
                maskOpenid(request == null ? null : request.getOpenid()),
                request == null ? null : request.getNickname());

        AppUser user = userService.updateNickname(
                request == null ? null : request.getOpenid(),
                request == null ? null : request.getNickname());
        return ResponseEntity.ok(toProfileBody(user));
    }

    private Map<String, Object> toProfileBody(AppUser user) {
        Map<String, Object> profileData = Map.of(
                "nickname", user.getNickname(),
                "userId", user.getUserId()
        );
        return Map.of(
                "nickname", user.getNickname(),
                "userId", user.getUserId(),
                "data", profileData
        );
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
