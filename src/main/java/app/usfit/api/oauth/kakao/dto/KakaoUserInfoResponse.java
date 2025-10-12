package app.usfit.api.oauth.kakao.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(
        Long id,
        @JsonProperty("kakao_account") KakaoAccount kakaoAccount
) {
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
            String email,
            Profile profile
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Profile(
            String nickname
    ) {}

    // null-safe 메서드
    public String safeEmail() {
        return (kakaoAccount != null) ? kakaoAccount.email : null;
    }
    public String safeNickname() {
        return (kakaoAccount != null && kakaoAccount.profile != null) 
            ? kakaoAccount.profile.nickname 
            : null;
    }
}
