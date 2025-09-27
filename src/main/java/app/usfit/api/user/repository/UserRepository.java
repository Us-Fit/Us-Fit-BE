package app.usfit.api.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.user.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
}