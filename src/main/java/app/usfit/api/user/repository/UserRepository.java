package app.usfit.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // User: 엔티티 타입
    // Long: Primary Key 타입 (User 엔티티의 id 필드 타입)
    // CRUD를 위한 기본 메서드를 제공해준다. (JpaRepository)
    // optional: null 처리 안전하게 하기 위해 사용(? 연산이랑 비슷.)
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
}