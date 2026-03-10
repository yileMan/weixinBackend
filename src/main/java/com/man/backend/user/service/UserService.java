package com.man.backend.user.service;

import com.man.backend.user.model.AppUser;
import com.man.backend.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private static final String DEFAULT_NICKNAME = "WeChat User";
    private static final String USER_ID_PREFIX = "U";
    private static final char[] USER_ID_ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
    private static final int USER_ID_RANDOM_LENGTH = 12;

    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Transactional
    public AppUser getOrCreateByOpenid(String openid) {
        String normalizedOpenid = requireNonBlank(openid, "openid is required");
        return userRepository.findByOpenid(normalizedOpenid)
                .map(this::ensureProfileFields)
                .orElseGet(() -> userRepository.save(new AppUser(
                        null,
                        generateUniqueUserId(),
                        normalizedOpenid,
                        DEFAULT_NICKNAME
                )));
    }

    @Transactional
    public AppUser getProfileByOpenid(String openid) {
        String normalizedOpenid = requireNonBlank(openid, "openid is required");
        return userRepository.findByOpenid(normalizedOpenid)
                .map(this::ensureProfileFields)
                .orElseThrow(() -> {
                    log.warn("User profile not found: openid={}", maskOpenid(normalizedOpenid));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
                });
    }

    @Transactional
    public AppUser updateNickname(String openid, String nickname) {
        String normalizedOpenid = requireNonBlank(openid, "openid is required");
        String normalizedNickname = requireNonBlank(nickname, "nickname is required");

        AppUser user = userRepository.findByOpenid(normalizedOpenid)
                .map(this::ensureProfileFields)
                .orElseThrow(() -> {
                    log.warn("User not found for nickname update: openid={}", maskOpenid(normalizedOpenid));
                    return new ResponseStatusException(HttpStatus.NOT_FOUND, "user not found");
                });

        user.setNickname(normalizedNickname);
        return userRepository.save(user);
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            log.warn("User request invalid: {}", message);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String generateUniqueUserId() {
        for (int i = 0; i < 20; i++) {
            String userId = USER_ID_PREFIX + randomPart(USER_ID_RANDOM_LENGTH);
            if (!userRepository.existsByUserId(userId)) {
                return userId;
            }
        }
        log.error("Failed to generate unique userId after 20 attempts");
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "failed to generate unique userId");
    }

    private String randomPart(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            builder.append(USER_ID_ALPHABET[secureRandom.nextInt(USER_ID_ALPHABET.length)]);
        }
        return builder.toString();
    }

    private AppUser ensureProfileFields(AppUser user) {
        boolean changed = false;
        if (user.getUserId() == null || user.getUserId().isBlank()) {
            user.setUserId(generateUniqueUserId());
            changed = true;
        }
        if (user.getNickname() == null || user.getNickname().isBlank()) {
            user.setNickname(DEFAULT_NICKNAME);
            changed = true;
        }
        return changed ? userRepository.save(user) : user;
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
