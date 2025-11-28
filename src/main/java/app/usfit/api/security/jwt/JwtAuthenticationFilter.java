package app.usfit.api.security.jwt;

import java.io.IOException;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider tokenProvider;

    private static final AntPathMatcher matcher = new AntPathMatcher();
    private static final String[] WHITELIST = {
        "/v3/api-docs",
        "/api/user/login",
        "/api/user/signup",
        "/kakao-test.html",
        "/v3/api-docs/**",
        "/swagger-ui.html",
        "/swagger-ui/**",
        "/login"
    };
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true; // CORS preflight
        String path = request.getRequestURI();
        for (String p : WHITELIST) {
            System.out.println("매칭 시도: " + p + " [vs] " + path);
            if (matcher.match(p, path)) {System.out.println("매칭"); return true;} // permitAll 경로는 필터 건너뜀
        }
        return false;
    }


    public JwtAuthenticationFilter(JwtTokenProvider tokenProvider) {
        this.tokenProvider = tokenProvider;
    }

    @Override
    protected void doFilterInternal(
        HttpServletRequest request,
        HttpServletResponse response,
        FilterChain chain
        ) throws ServletException, IOException {
        // Authoziation 헤더에서 Bearer 토큰 추출 -> "Authorization: Bearer <JWT>" 느낌.
        String header = request.getHeader("Authorization");
        System.out.println("JWT 필터 작동, 헤더: " + header);
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            try {
                var auth = tokenProvider.getAuthentication(token);
                // SecurityContext에 인증 정보 설정
                SecurityContextHolder.getContext().setAuthentication(auth);
            } catch (Exception e) {
                // 토큰이 유효하지 않은 경우 예외 처리 (로그 남기기)
                System.out.println("유효하지 않은 JWT 토큰: " + e.getMessage());
            }
        }
        chain.doFilter(request, response);
    }
}
