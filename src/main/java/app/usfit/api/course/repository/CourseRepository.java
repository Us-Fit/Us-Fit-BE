package app.usfit.api.course.repository;

import app.usfit.api.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    /**
     * 종목명 + 시도명 + 시군구명으로 강좌 검색
     */
    List<Course> findByItemNmAndCtprvnNmAndSignguNm(
            String itemNm,
            String ctprvnNm,
            String signguNm
    );
}