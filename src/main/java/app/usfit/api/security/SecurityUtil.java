package app.usfit.api.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecurityUtil {

    public static Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder
                .getContext()
                .getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("인증된 사용자가 없습니다.");
        }

        Object principal = authentication.getPrincipal();

        // principal이 String인 경우
        if (principal instanceof String) {
            try {
                return Long.parseLong((String) principal);
            } catch (NumberFormatException e) {
                throw new IllegalStateException("JWT principal에서 userId 파싱 실패");
            }
        }

        // 혹시 나중에 Long으로 바뀌는 경우 대비
        if (principal instanceof Long) {
            return (Long) principal;
        }

        throw new IllegalStateException("지원하지 않는 principal 타입: " + principal.getClass());
    }
}
