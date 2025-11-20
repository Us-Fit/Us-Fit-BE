package app.usfit.api.course.dto;

import lombok.*;
import java.math.BigDecimal;
import app.usfit.api.course.entity.Course;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CourseDetailDto {

    private Long id;

    private String bsnsNo;
    private String fcltyNm;
    private String itemCd;
    private String itemNm;
    private String ctprvnCd;
    private String ctprvnNm;
    private String signguCd;
    private String signguNm;
    private String fcltyAddr;
    private String fcltyDetailAddr;
    private String zipNo;
    private String telNo;
    private String courseNm;
    private String courseNo;
    private String courseEstblYear;
    private String courseEstblMt;
    private String courseBeginDe;
    private String courseEndDe;
    private BigDecimal courseReqstNmprCo;
    private BigDecimal coursePrc;

    /** Entity → DTO 변환 */
    public static CourseDetailDto fromEntity(Course course) {
        if (course == null) return null;

        return CourseDetailDto.builder()
                .id(course.getId())
                .bsnsNo(course.getBsnsNo())
                .fcltyNm(course.getFcltyNm())
                .itemCd(course.getItemCd())
                .itemNm(course.getItemNm())
                .ctprvnCd(course.getCtprvnCd())
                .ctprvnNm(course.getCtprvnNm())
                .signguCd(course.getSignguCd())
                .signguNm(course.getSignguNm())
                .fcltyAddr(course.getFcltyAddr())
                .fcltyDetailAddr(course.getFcltyDetailAddr())
                .zipNo(course.getZipNo())
                .telNo(course.getTelNo())
                .courseNm(course.getCourseNm())
                .courseNo(course.getCourseNo())
                .courseEstblYear(course.getCourseEstblYear())
                .courseEstblMt(course.getCourseEstblMt())
                .courseBeginDe(course.getCourseBeginDe())
                .courseEndDe(course.getCourseEndDe())
                .courseReqstNmprCo(course.getCourseReqstNmprCo())
                .coursePrc(course.getCoursePrc())
                .build();
    }
}
