package app.usfit.api.oauth.kakao;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import app.usfit.api.oauth.kakao.dto.KakaoTokenResponse;
import app.usfit.api.oauth.kakao.dto.KakaoUserInfoResponse;

@Component
public class KakaoOAuthClient {

    private final WebClient webClient;

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

    public KakaoOAuthClient(WebClient.Builder builder) {
        this.webClient = builder.build();
    }

    public KakaoTokenResponse exchangeToken(String code) {
        // Post는 tokenUri로 보낸다.
        return webClient.post()
                .uri(tokenUri)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                // formFormData는 폼 키-값을 만든다. .with로 항목을 추가
                .body(BodyInserters.fromFormData("grant_type","authorization_code")
                        .with("client_id", clientId)
                        .with("redirect_uri", redirectUri)
                        .with("code", code)
                        .with("client_secret", clientSecret == null ? "" : clientSecret))
                // retrieve(): 요청(메서드, URL, 헤더, 바디 등)을 끝내고 응답을 받음
                .retrieve()
                // onStatus(): 특정 HTTP 상태 코드에 대한 오류 처리
                .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
                    resp -> resp.bodyToMono(String.class)
                .defaultIfEmpty("kakao token error")
                .map(msg -> new RuntimeException("Kakao token exchange failed: " + msg)))
                // 바디 디코딩 방식 선택 -> KakaoTokenResponse.class 로 매핑
                .bodyToMono(KakaoTokenResponse.class)
                // block(): Mono -> 실제 객체로 변환 (동기 처리) 응답이 올때까지 스레드를 블로킹한다. 예외 발생 시 오류 던짐.
                .block(java.time.Duration.ofSeconds(5)); // 타임아웃 5초
    }

    public KakaoUserInfoResponse getUserInfo(String accessToken) {
    return webClient.get()
                .uri(userInfoUri)
                // Authorization: Bearer {accessToken} 이 형태로 전송해야함.
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
        .retrieve()
        .onStatus(status -> status.is4xxClientError() || status.is5xxServerError(),
            resp -> resp.bodyToMono(String.class)
                .defaultIfEmpty("kakao userinfo error")
                .map(msg -> new RuntimeException("Kakao userinfo failed: " + msg)))
                .bodyToMono(KakaoUserInfoResponse.class)
        .block(java.time.Duration.ofSeconds(5));
    }
}
