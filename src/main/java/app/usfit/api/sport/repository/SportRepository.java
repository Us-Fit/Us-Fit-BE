package app.usfit.api.sport.repository;

import app.usfit.api.sport.entity.Sport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SportRepository extends JpaRepository<Sport, Long> {
    Optional<Sport> findByCode(String code);
    boolean existsByCode(String code);
}