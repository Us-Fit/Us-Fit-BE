package app.usfit.api.sport.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import app.usfit.api.sport.entity.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {
    Optional<Sport> findByCode(String code);
    List<Sport> findByCodeIn(List<String> codes);
    boolean existsByCode(String code);
}