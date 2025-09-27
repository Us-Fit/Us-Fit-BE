package app.usfit.api.user.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Data  // getter, setter, toString, equals, hashCode 자동 생성
@Builder  // Builder 패턴 자동 생성
@NoArgsConstructor  // 기본 생성자 (JPA 필수)
@AllArgsConstructor  // 모든 필드 생성자
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String username;
    private String password;
    private String email;
    private String nickname;
}