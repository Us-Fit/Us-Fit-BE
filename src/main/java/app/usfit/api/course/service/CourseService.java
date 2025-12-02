package app.usfit.api.course.service;

import app.usfit.api.common.exception.CourseNotFoundException;
import app.usfit.api.course.dto.CourseDetailDto;
import app.usfit.api.course.entity.Course;
import app.usfit.api.course.repository.CourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CourseService {

    private final CourseRepository courseRepository;

    /**
     * itemNm, ctprvnNm, signguNm 기준으로 강좌 리스트 조회
     */
    public List<CourseDetailDto> getCoursesByFilter(String itemNm, String ctprvnNm, String signguNm) {

        List<Course> courses = courseRepository.findByItemNmAndCtprvnNmAndSignguNm(
                itemNm,
                ctprvnNm,
                signguNm
        );

        return courses.stream()
                .map(CourseDetailDto::fromEntity)
                .toList();
    }

    /**
     * itemNm, ctprvnNm, signguNm 기준으로 강좌 리스트 조회 pagination
     */
//    public Page<CourseDetailDto> getCoursesByFilter(String itemNm, String ctprvnNm, String signguNm, Pageable pageable) {
//
//        Page<Course> page = courseRepository.findByItemNmAndCtprvnNmAndSignguNm(
//                itemNm,
//                ctprvnNm,
//                signguNm,
//                pageable
//        );
//
//        return page.map(CourseDetailDto::fromEntity);
//    }

    public Page<CourseDetailDto> getCoursesByFilter(
            String itemNm,
            String ctprvnNm,
            String signguNm,
            Pageable pageable
    ) {

        Page<Course> page = courseRepository.findCoursesDynamic(
                itemNm,
                ctprvnNm,
                signguNm,
                pageable
        );

        return page.map(CourseDetailDto::fromEntity);
    }


    public CourseDetailDto getCourseById(Long id) {
        Course course = courseRepository.findCourseById(id)
                .orElseThrow(() -> new CourseNotFoundException(id));
        return CourseDetailDto.fromEntity(course);
    }
}
