package app.usfit.api.sport.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import app.usfit.api.sport.entity.Sport;

public interface SportRepository extends JpaRepository<Sport, Long> {
    Optional<Sport> findByCode(String code);
    List<Sport> findByCodeIn(List<String> codes);
    boolean existsByCode(String code);

    // 대소문자 무시 버전 (JPQL)
    @Query("select s.id from Sport s where lower(trim(s.name)) in :names")
    List<Long> findIdsByNormalizedNames(@Param("names") Collection<String> normalizedNames);

    // 운동 이름으로 운동 조회 (대소문자 무시) -> List로 하는 이유는 N+1 문제 방지하고자 이렇게 함.
    @Query("select s from Sport s where lower(trim(s.name)) in :names")
    List<Sport> findAllByNormalizedNames(@Param("names") Collection<String> normalizedNames);
}