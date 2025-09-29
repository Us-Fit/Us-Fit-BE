package app.usfit.api.facility.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Facility (시설) 엔티티
 * - 원천 데이터(공공데이터)에서 내려오는 시설 기본 정보를 보관
 * - 주소/소유자/담당자/종목 등의 하위 엔티티와 1:N 관계를 가짐
 */
@Entity // JPA가 관리하는 테이블 매핑 객체임을 선언
@Table(name = "facility") // 테이블 이름 지정(미지정 시 클래스명 기준)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 프록시 및 리플렉션용 기본 생성자(외부 직접 생성 방지)
@AllArgsConstructor
@Builder
public class Facility {

    @Id // PK 컬럼 지정
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL 등의 AutoIncrement 전략
    private Long id;   // 시설 PK (내부 식별자)

    @Column(nullable = false, length = 200)
    private String name;   // 시설명 (FCLTY_NM)

    @Column(length = 20)
    private String statusValue;   // 시설 상태값 (FCLTY_STATE_VALUE)

    @Column(length = 20)
    private String typeCode;   // 시설 유형 코드 (FCLTY_TY_CD)

    @Column(length = 100)
    private String typeName;   // 시설 유형 명 (FCLTY_TY_NM)

    @Column(length = 20)
    private String subdivCode; // 시설 구분 코드 (FCLTY_SDIV_CD)

    @Column(length = 20)
    private String subdivName; // 시설 구분 명 (FCLTY_FLAG_NM)

    @Column(length = 20)
    private String industryCode; // 업종 코드 (INDUTY_CD)

    @Column(length = 100)
    private String industryName; // 업종 명 (INDUTY_NM)

    @Column(length = 20)
    private String indoorOutdoor; // 실내/외 구분 (NDOR_SDIV_NM)

    @Column(length = 20)
    private String phone;         // 시설 전화번호 (FCLTY_TEL_NO)

    @Column(length = 200)
    private String homepageUrl;   // 시설 홈페이지 URL (FCLTY_HMPG_URL)

    private Integer seatCount;    // 관람석 수 (ADTM_CO)
    private Integer capacity;     // 수용 인원 수 (ACMD_NMPR_CO)

    @Column(precision = 12, scale = 2)
    private BigDecimal areaSqm;   // 시설 면적 m² (FCLTY_AR_CO) - 금액/면적 등 정밀 값은 BigDecimal 권장

    @Column(length = 1)
    private String lifeOpenYn;    // 생활 개방 여부 (LVLH_OPN_AT)

    @Column(length = 100)
    private String lifeGymName;   // 생활 체육관 명 (LVLH_GMNSM_NM)

    @Column(length = 100)
    private String userGroupName; // 이용 단체 명 (UTILIIZA_GRP_NM)

    // 원천은 CHAR(8) YYYYMMDD → 애플리케이션에서는 LocalDate로 파싱 저장 권장
    private LocalDate createdStdDate; // 시설 생성 기준 일자 (FCLTY_CRTN_STDR_DE)
    private LocalDate regDate;        // 체육시설 등록 일자 (ALSFC_REGIST_DE)
    private LocalDate completionDate; // 준공 일자 (COMPET_DE)
    private LocalDate suspendDate;    // 휴업 일자 (SSS_DE)
    private LocalDate closeDate;      // 운영 폐업 일자 (OPER_CLSBIZ_DE)

    @Column(length = 1)
    private String nationalYn;   // 국가 체육시설 여부 (NATION_ALSFC_AT)

    @Column(length = 1)
    private String earthquakeYn; // 내진 설계 여부 (ERDSGN_AT)

    @Column(length = 1, nullable = false)
    private String selfcheckYn;  // 자율 점검 대상 여부 (ATNM_CHCK_TRGET_AT) - 원천 NOT NULL

    @Column(length = 20)
    private String dataOriginCd; // 데이터 출처 구분 코드 (DATA_ORIGIN_FLAG_CD)

    @Column(length = 1, nullable = false)
    private String deletedYn;    // 삭제 여부 (DEL_AT) - 논리삭제 플래그

    @CreationTimestamp // INSERT 시점에 자동 생성
    private LocalDateTime createdAt; // 등록 일시 (REGIST_DT)

    @UpdateTimestamp   // UPDATE 시점에 자동 갱신
    private LocalDateTime updatedAt; // 수정 일시 (UPDT_DT)

    /* =================== 연관관계 (모두 지연 로딩, 소유자는 N 쪽 엔티티) =================== */

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacilityAddress> addresses = new ArrayList<>(); // 주소 목록 (1:N 역방향)

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacilityOwner> owners = new ArrayList<>(); // 소유자/소유주체 정보 (1:N 역방향)

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacilityContact> contacts = new ArrayList<>(); // 담당자 연락처 (1:N 역방향)

    @OneToMany(mappedBy = "facility", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FacilitySport> sports = new ArrayList<>(); // 시설-종목 매핑 (1:N 역방향, 다대다의 허브)
}
