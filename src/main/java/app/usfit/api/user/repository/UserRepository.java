package app.usfit.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    // User: 엔티티 타입
    // Long: Primary Key 타입 (User 엔티티의 id 필드 타입)
    // CRUD를 위한 기본 메서드를 제공해준다. (JpaRepository)
    // optional: null 처리 안전하게 하기 위해 사용(? 연산이랑 비슷.)
    // findBy + 필드명 (첫 글자 대문자) : select * from users where 필드명 = ?
    // 만약 Jpa사용 안하면 직접 쿼리 작성해야함. (ex. @Query("select u from User u where u.email = :email or u.username = :email"))
    Optional<User> findByEmail(String email);
    Optional<User> findByProviderAndProviderId(AuthProviderEnum provider, String providerId);

    @Modifying
    @Query(value = "DELETE FROM users WHERE id = :userId", nativeQuery = true)
    void deleteUserByIdNative(@Param("userId") Long userId);
}