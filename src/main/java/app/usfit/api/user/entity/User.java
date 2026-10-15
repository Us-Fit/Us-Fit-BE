package app.usfit.api.user.entity;

import app.usfit.api.common.enums.AuthProviderEnum;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
// (provider, provider_id) 유니크: 첫 로그인 요청이 동시에 와도 소셜 계정이 하나만 생성되도록 DB에서 보장
// LOCAL 계정은 provider_id가 NULL이라 여러 개여도 제약에 걸리지 않음 (MySQL은 NULL 중복 허용)
@Table(
    name = "users",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_users_provider_provider_id",
        columnNames = {"provider", "provider_id"}
    )
)
@Data  // getter, setter, toString, equals, hashCode 자동 생성
@Builder  // Builder 패턴 자동 생성
@NoArgsConstructor  // 기본 생성자 (JPA 필수)
@AllArgsConstructor  // 모든 필드 생성자
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String password;
    private String email;

    // STRING: Enum 이름을 DB에 저장
    // ORDINAL: Enum 순서를 DB에 저장 (0,1,2...) // 새 enum 추가 혹은 순서 변경 시 데이터 의미 가 깨짐.
    @Enumerated(EnumType.STRING)
    private AuthProviderEnum provider; // AuthProvider enum 사용
    
    // providerId: 공급자에서 사용자를 유일하게 식별하기 위한 값임.
    private String providerId; // 소셜 로그인 시 외부 식별자

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY)
    private UserProfile profile;
}