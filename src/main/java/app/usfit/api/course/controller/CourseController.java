package app.usfit.api.course.controller;

import app.usfit.api.course.dto.CourseDetailDto;
import app.usfit.api.course.service.CourseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/course")
@RequiredArgsConstructor
public class CourseController {

    private final CourseService courseService;

    /**
     * 프론트에서 3개 값(itemNm, ctprvnNm, signguNm)을 주면
     * 그 조건에 맞는 강좌 리스트 반환
     *
     * 예)
     * GET /api/courses/search?itemNm=농구&ctprvnNm=서울특별시&signguNm=강남구
     */
//    @GetMapping("/search")
//    public List<CourseDetailDto> searchCourses(
//            @RequestParam String itemNm,
//            @RequestParam String ctprvnNm,
//            @RequestParam String signguNm
//    ) {
//        return courseService.getCoursesByFilter(itemNm, ctprvnNm, signguNm);
//    }

    /**
     * 프론트에서 3개 값(itemNm, ctprvnNm, signguNm)을 주면
     * 그 조건에 맞는 강좌 리스트 반환
     *
     * 예)
     * GET /api/courses/search?itemNm=농구&ctprvnNm=서울특별시&signguNm=강남구
     * pagination 적용
     */
//    @GetMapping("/search/page")
//    public Page<CourseDetailDto> searchCourses(
//            @RequestParam String itemNm,
//            @RequestParam String ctprvnNm,
//            @RequestParam String signguNm,
//            Pageable pageable
//    ) {
//        return courseService.getCoursesByFilter(itemNm, ctprvnNm, signguNm, pageable);
//    }

    @GetMapping("/search")
    public Page<CourseDetailDto> searchCourses(
            @RequestParam(required = false) String itemNm,
            @RequestParam(required = false) String ctprvnNm,
            @RequestParam(required = false) String signguNm,
            Pageable pageable
    ) {
        // 빈 값은 null로 통일
        itemNm   = (itemNm == null || itemNm.isBlank()) ? null : itemNm;
        ctprvnNm = (ctprvnNm == null || ctprvnNm.isBlank()) ? null : ctprvnNm;
        signguNm = (signguNm == null || signguNm.isBlank()) ? null : signguNm;

        return courseService.getCoursesByFilter(itemNm, ctprvnNm, signguNm, pageable);
    }


    @GetMapping("/{id}")
    public CourseDetailDto getCourseDetail(
            @PathVariable Long id
    ){
        return courseService.getCourseById(id);
    }
}
