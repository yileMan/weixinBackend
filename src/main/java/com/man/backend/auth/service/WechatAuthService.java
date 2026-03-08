package com.man.backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

@Service
public class WechatAuthService {

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
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "wechat.miniapp.app-id or wechat.miniapp.app-secret is not configured");
        }

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
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "wechat response is empty");
            }

            if (response.hasNonNull("openid")) {
                return response.get("openid").asText();
            }

            int errcode = response.path("errcode").asInt(-1);
            String errmsg = response.path("errmsg").asText("unknown error");
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "wechat auth failed: errcode=" + errcode + ", errmsg=" + errmsg);
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "failed to call wechat api", ex);
        }
    }

    private String requireNonBlank(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }
}
