package com.man.backend.user.dto;

public class UserProfileUpdateRequest {

    private String openid;
    private String nickname;

    public UserProfileUpdateRequest() {
    }

    public String getOpenid() {
        return openid;
    }

    public String getNickname() {
        return nickname;
    }

    public void setOpenid(String openid) {
        this.openid = openid;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }
}
