package app.usfit.api.course.repository;

import app.usfit.api.course.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseRepository extends JpaRepository<Course, Long> {
    /**
     * 종목명 + 시도명 + 시군구명으로 강좌 검색
     */
    List<Course> findByItemNmAndCtprvnNmAndSignguNm(
            String itemNm,
            String ctprvnNm,
            String signguNm
    );

    /**
     * 종목명 + 시도명 + 시군구명으로 강좌 검색
     */
    Page<Course> findByItemNmAndCtprvnNmAndSignguNm(
            String itemNm,
            String ctprvnNm,
            String signguNm,
            Pageable pageable
    );

    //강좌 id로 특정 강좌 검색
    Optional<Course> findCourseById(
            Long id
    );
}