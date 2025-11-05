package app.usfit.api.oauth.kakao;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import app.usfit.api.oauth.kakao.dto.KakaoTokenResponse;
import app.usfit.api.oauth.kakao.dto.KakaoUserInfoResponse;
import io.jsonwebtoken.io.IOException;

@Component
public class KakaoOAuthClient {

    private final RestClient restClient;

    @Value("${kakao.oauth.client-id}")
    private String clientId;
    @Value("${kakao.oauth.client-secret:}")
    private String clientSecret;
    @Value("${kakao.oauth.redirect-uri}")
    private String redirectUri;
    @Value("${kakao.oauth.token-uri}")
    private String tokenUri;
    @Value("${kakao.oauth.user-info-uri}")
    private String userInfoUri;

    public KakaoOAuthClient() {
        this.restClient = RestClient.builder().build();
    }

    public KakaoTokenResponse exchangeToken(String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", clientId);
        formData.add("redirect_uri", redirectUri);
        formData.add("code", code);
        if (clientSecret != null && !clientSecret.isBlank()) {
            formData.add("client_secret", clientSecret);
        }
        // Post는 tokenUri로 보낸다.
        return restClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // formData는 폼 키-값을 만든다.
                .body(formData)
                // retrieve(): 요청(메서드, URL, 헤더, 바디 등)을 끝내고 응답을 받음
                .retrieve()
                // onStatus(): 특정 HTTP 상태 코드에 대한 오류 처리
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    (req, res) -> {
                        String msg = "";
                        try (var is = res.getBody()) {
                            if (is != null) msg = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                        } catch (IOException ignored) {}
                        throw new RuntimeException("Kakao token exchange failed: " + msg);
                })
                // 바디 디코딩 방식 선택 -> KakaoTokenResponse.class 로 매핑
                .body(KakaoTokenResponse.class);
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
        return restClient.get()
            .uri(userInfoUri)
            .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
            .retrieve()
            .onStatus(s -> s.is4xxClientError() || s.is5xxServerError(),
                (req, res) -> {
                    String msg = "";
                    try (var is = res.getBody()) {
                        if (is != null) msg = new String(is.readAllBytes(), StandardCharsets.UTF_8);
                    } catch (IOException ignored) {}
                    throw new RuntimeException("Kakao userinfo failed: " + msg);
                })
            .body(KakaoUserInfoResponse.class);
    }
}
