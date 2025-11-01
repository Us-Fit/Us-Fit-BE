package app.usfit.api.security.jwt;


import java.util.Date;
import java.util.Map;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenProvider {
    private final SecretKey key;
    private final long validityMillis;

    public JwtTokenProvider(
        @Value("${jwt.secret}") String secret,
        @Value("${jwt.access-token-validity-seconds}") long validitySeconds
    )
    {
        this.key = buildKey(secret);
        this.validityMillis = validitySeconds * 1000L;
    }

    private SecretKey buildKey(String secret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException e) {
            // 실패하면 일단 그냥 바이트 배열로 사용
            bytes = secret.getBytes();
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    public String createAccessToken(Long userId, String provider, String email) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + validityMillis);

        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .addClaims(Map.of(
                        "provider", provider == null ? "" : provider,
                        "email", email == null ? "" : email
                ))
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    // 토큰 유효성 검사
    public boolean validate(String token) {
        try {
            // 검증함
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public Authentication getAuthentication(String token) {
        Long userId = getUserId(token);
        return new UsernamePasswordAuthenticationToken(
            String.valueOf(userId),
            token,
            AuthorityUtils.NO_AUTHORITIES
        );
    }

    public Long getUserId(String token) {
        // token 이용해서 Authentication 객체 생성
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build()
                .parseClaimsJws(token).getBody();
        return Long.valueOf(claims.getSubject());
    }
}
