package app.usfit.api.course.service;

import app.usfit.api.course.entity.Course;
import app.usfit.api.course.repository.CourseRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class CourseImportService {

    private final CourseRepository courseRepository;

    @PersistenceContext
    private EntityManager em;

    /** JPA 배치 크기 (20만 행 기준 1000 정도가 무난) */
    private static final int BATCH_SIZE = 1000;

    /** 가져올 강좌 시작 연도 (요구사항: 2025년만) */
    private static final String TARGET_YEAR = "2025";

    public CourseImportService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    /**
     * MultipartFile 기반 CSV import (API 업로드용)
     */
    @Transactional
    public int importCsv(MultipartFile file, Charset charset) throws Exception {
        try (Reader reader = new InputStreamReader(file.getInputStream(), charset)) {
            return importCsv(reader);
        }
    }

    /**
     * 로컬 파일(Path) 기반 CSV import (CommandLineRunner에서 사용)
     */
    @Transactional
    public int importCsv(Path path, boolean b, Charset charset) throws Exception {
        try (Reader reader = Files.newBufferedReader(path, charset)) {
            return importCsv(reader);
        }
    }

    /**
     * 실제 CSV → DB 적재 공통 로직
     */
    private int importCsv(Reader reader) throws Exception {
        int count = 0;
        int batchCount = 0;

        Iterable<CSVRecord> records = CSVFormat.DEFAULT
                .builder()
                .setHeader()              // 첫 줄을 header로 사용
                .setSkipHeaderRecord(true)
                .build()
                .parse(reader);

        for (CSVRecord record : records) {

            // ① 2025년 데이터만 필터링 (COURSE_BEGIN_DE 기준)
            String beginDe = record.get("COURSE_BEGIN_DE");
            if (!isTargetYear(beginDe)) {
                continue;   // 2025년이 아니면 스킵
            }

            // ② CSVRecord → Course 엔티티 매핑
            Course course = mapRecordToCourse(record);

            // ③ 배치 insert
            em.persist(course);
            count++;
            batchCount++;

            if (batchCount >= BATCH_SIZE) {
                em.flush();   // 쌓인 INSERT 실행
                em.clear();   // 1차 캐시 비우기
                batchCount = 0;
            }
        }

        // 마지막에 남은 배치 처리
        if (batchCount > 0) {
            em.flush();
            em.clear();
        }

        return count;
    }

    /**
     * COURSE_BEGIN_DE가 TARGET_YEAR(예: "2025")로 시작하는지 체크
     */
    private boolean isTargetYear(String beginDe) {
        if (beginDe == null) return false;
        String trimmed = beginDe.trim();
        if (trimmed.length() < 4) return false;
        return trimmed.startsWith(TARGET_YEAR);
    }

    /**
     * CSVRecord → Course 엔티티 매핑
     */
    private Course mapRecordToCourse(CSVRecord r) {
        return Course.builder()
                .bsnsNo(r.get("BSNS_NO"))
                .fcltyNm(r.get("FCLTY_NM"))
                .itemCd(r.get("ITEM_CD"))
                .itemNm(r.get("ITEM_NM"))
                .ctprvnCd(r.get("CTPRVN_CD"))
                .ctprvnNm(r.get("CTPRVN_NM"))
                .signguCd(r.get("SIGNGU_CD"))
                .signguNm(r.get("SIGNGU_NM"))
                .fcltyAddr(r.get("FCLTY_ADDR"))
                .fcltyDetailAddr(r.get("FCLTY_DETAIL_ADDR"))
                .zipNo(r.get("ZIP_NO"))
                .telNo(r.get("TEL_NO"))
                .courseNm(r.get("COURSE_NM"))
                .courseNo(r.get("COURSE_NO"))
                .courseEstblYear(r.get("COURSE_ESTBL_YEAR"))
                .courseEstblMt(r.get("COURSE_ESTBL_MT"))
                .courseBeginDe(r.get("COURSE_BEGIN_DE"))
                .courseEndDe(r.get("COURSE_END_DE"))
                .courseReqstNmprCo(parseBigDecimal(r.get("COURSE_REQST_NMPR_CO")))
                .coursePrc(parseBigDecimal(r.get("COURSE_PRC")))
                .build();
    }

    /**
     * 빈 문자열/공백을 null로 처리하는 BigDecimal 변환
     */
    private BigDecimal parseBigDecimal(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return null;
        return new BigDecimal(trimmed);
    }
}
