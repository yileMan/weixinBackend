package com.man.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.JsonNode;

@Service
public class WechatAuthService {

    private static final Logger log = LoggerFactory.getLogger(WechatAuthService.class);

    private final RestClient restClient;
    private final String appId;
    private final String appSecret;

    public WechatAuthService(
            @Value("${wechat.miniapp.app-id:}") String appId,
            @Value("${wechat.miniapp.app-secret:}") String appSecret) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.weixin.qq.com")
                .build();
        this.appId = appId;
        this.appSecret = appSecret;
    }

    public String exchangeCodeForOpenid(String code) {
        String normalizedCode = requireNonBlank(code, "code is required");

        if (appId.isBlank() || appSecret.isBlank()) {
            log.warn("Wechat auth config missing: appIdPresent={}, appSecretPresent={}",
                    !appId.isBlank(), !appSecret.isBlank());
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "wechat.miniapp.app-id or wechat.miniapp.app-secret is not configured");
        }

        log.info("Calling WeChat jscode2session appIdSuffix={}, codeLength={}",
                maskAppId(appId),
                normalizedCode.length());

        try {
            JsonNode response = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/sns/jscode2session")
                            .queryParam("appid", appId)
                            .queryParam("secret", appSecret)
                            .queryParam("js_code", normalizedCode)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);

            if (response == null) {
                log.error("WeChat response is null");
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "wechat response is empty");
            }

            if (response.hasNonNull("openid")) {
                String openid = response.get("openid").asText();
                log.info("WeChat auth success openid={}", maskOpenid(openid));
                return response.get("openid").asText();
            }

            int errcode = response.path("errcode").asInt(-1);
            String errmsg = response.path("errmsg").asText("unknown error");
            log.warn("WeChat auth failed: errcode={}, errmsg={}, raw={}", errcode, errmsg, response.toString());
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "wechat auth failed: errcode=" + errcode + ", errmsg=" + errmsg);
        } catch (RestClientException ex) {
            log.error("WeChat API call failed: {}", ex.getMessage(), ex);
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to call wechat api", ex);
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String maskAppId(String appId) {
        if (appId == null || appId.isBlank()) {
            return "blank";
        }
        String trimmed = appId.trim();
        if (trimmed.length() <= 6) {
            return "***";
        }
        return "***" + trimmed.substring(trimmed.length() - 4);
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
