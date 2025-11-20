package app.usfit.api.course.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "course")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Course{

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;   // 엑셀에는 없으니까 surrogate key 하나 둠

    /** 사업자등록번호 (VARCHAR 10) */
    @Column(name = "BSNS_NO", length = 10)
    private String bsnsNo;

    /** 시설명 (VARCHAR 200) */
    @Column(name = "FCLTY_NM", length = 200)
    private String fcltyNm;

    /** 종목코드 (VARCHAR 30) */
    @Column(name = "ITEM_CD", length = 30)
    private String itemCd;

    /** 종목명 (VARCHAR 200) */
    @Column(name = "ITEM_NM", length = 200)
    private String itemNm;

    /** 시도코드 (VARCHAR 30) */
    @Column(name = "CTPRVN_CD", length = 30)
    private String ctprvnCd;

    /** 시도명 (VARCHAR 200) */
    @Column(name = "CTPRVN_NM", length = 200)
    private String ctprvnNm;

    /** 시군구코드 (VARCHAR 30) */
    @Column(name = "SIGNGU_CD", length = 30)
    private String signguCd;

    /** 시군구명 (VARCHAR 200) */
    @Column(name = "SIGNGU_NM", length = 200)
    private String signguNm;

    /** 시설주소 (VARCHAR 200) */
    @Column(name = "FCLTY_ADDR", length = 200)
    private String fcltyAddr;

    /** 시설상세주소 (VARCHAR 200) */
    @Column(name = "FCLTY_DETAIL_ADDR", length = 200)
    private String fcltyDetailAddr;

    /** 우편번호 (VARCHAR 5) */
    @Column(name = "ZIP_NO", length = 5)
    private String zipNo;

    /** 전화번호 (VARCHAR 20) */
    @Column(name = "TEL_NO", length = 20)
    private String telNo;

    /** 강좌명 (VARCHAR 200) */
    @Column(name = "COURSE_NM", length = 200)
    private String courseNm;

    /** 강좌번호 (VARCHAR 20) */
    @Column(name = "COURSE_NO", length = 20)
    private String courseNo;

    /** 강좌개설년도 (VARCHAR 4) */
    @Column(name = "COURSE_ESTBL_YEAR", length = 4)
    private String courseEstblYear;

    /** 강좌개설월 (VARCHAR 2) */
    @Column(name = "COURSE_ESTBL_MT", length = 2)
    private String courseEstblMt;

    /** 강좌시작일자 (VARCHAR 8, yyyymmdd로 온다고 가정) */
    @Column(name = "COURSE_BEGIN_DE", length = 8)
    private String courseBeginDe;

    /** 강좌종료일자 (VARCHAR 8) */
    @Column(name = "COURSE_END_DE", length = 8)
    private String courseEndDe;

    /** 강좌신청인원수 (DECIMAL 38,0) */
    @Column(name = "COURSE_REQST_NMPR_CO", precision = 38, scale = 0)
    private BigDecimal courseReqstNmprCo;

    /** 강좌가격 (DECIMAL 28,5) */
    @Column(name = "COURSE_PRC", precision = 28, scale = 5)
    private BigDecimal coursePrc;
}