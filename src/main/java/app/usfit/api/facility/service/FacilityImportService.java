package app.usfit.api.facility.service;

import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.facility.entity.FacilityAddress;
import app.usfit.api.facility.entity.FacilityContact;
import app.usfit.api.facility.entity.FacilityOwner;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class FacilityImportService {
    private final FacilityRepository repo;

    public FacilityImportService(FacilityRepository repo){
        this.repo = repo;
    }

    /** 업로드 파일로부터 적재 (기본 UTF-8, 엑셀 ANSI면 MS949 전달) */
    @Transactional
    public int importCsv(MultipartFile file, boolean upsert, Charset charset) throws Exception {
        try (Reader reader = new InputStreamReader(file.getInputStream(), charset)) {
            return parseAndPersist(reader, upsert);
        }
    }

    /** 로컬 경로에서 바로 적재하고 싶을 때 오버로드 */
    @Transactional
    public int importCsv(java.nio.file.Path path, boolean upsert, Charset charset) throws Exception {
        try (Reader reader = java.nio.file.Files.newBufferedReader(path, charset)) {
            return parseAndPersist(reader, upsert);
        }
    }

    // =================== 내부 구현 ===================

    private int parseAndPersist(Reader reader, boolean upsert) throws Exception {
        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)  // 셀 주위 공백 무시
                .setAllowMissingColumnNames(true)  // 일부 컬럼 비어 있어도 통과
                .build();

        Iterable<CSVRecord> rows = fmt.parse(reader);

        final int BATCH = 1000;
        List<Facility> batch = new ArrayList<>(BATCH);
        int count = 0;

        for (CSVRecord r : rows) {
            String name = get(r, "FCLTY_NM");
            String roadAddr1 = get(r, "RDNMADR_ONE_NM");       // 업서트 키
            if (name.isBlank()) continue;                      // 최소 검증

            List<Facility> hits = upsert ? repo.findAllByNameAndRoadAddr1(name, roadAddr1) : List.of();
            Facility f = hits.isEmpty() ? new Facility() : hits.get(0);

            // ---------- Facility ----------
            f.setName(name);
            f.setStatusValue(get(r, "FCLTY_STATE_VALUE"));
            f.setTypeCode(get(r, "FCLTY_TY_CD"));
            f.setTypeName(get(r, "FCLTY_TY_NM"));
            f.setSubdivCode(get(r, "FCLTY_SDIV_CD"));
            f.setSubdivName(get(r, "FCLTY_FLAG_NM"));
            f.setIndustryCode(get(r, "INDUTY_CD"));
            f.setIndustryName(get(r, "INDUTY_NM"));
            f.setIndoorOutdoor(get(r, "NDOR_SDIV_NM"));

            f.setPhone(normalizePhone(get(r, "FCLTY_TEL_NO")));
            f.setHomepageUrl(emptyToNull(get(r, "FCLTY_HMPG_URL")));
            f.setOperationStyle(get(r, "FCLTY_OPER_STLE_VALUE"));

            f.setSeatCount(toInteger(get(r, "ADTM_CO")));
            f.setCapacity(toInteger(get(r, "ACMD_NMPR_CO")));
            f.setAreaSqm(toIntegerFloor(get(r, "FCLTY_AR_CO"))); // 정수 필드에 맞춰 절삭

            f.setLifeOpenYn(toYN(get(r, "LVLH_OPN_AT")));
            f.setLifeGymName(get(r, "LVLH_GMNSM_NM"));
            f.setUserGroupName(get(r, "UTILIIZA_GRP_NM"));

            f.setCreatedStdDate(toLocalDate(get(r, "FCLTY_CRTN_STDR_DE")));
            f.setRegDate(toLocalDate(get(r, "ALSFC_REGIST_DE")));
            f.setCompletionDate(toLocalDate(get(r, "COMPET_DE")));
            f.setSuspendDate(toLocalDate(get(r, "SSS_DE")));
            f.setCloseDate(toLocalDate(get(r, "OPER_CLSBIZ_DE")));

            f.setNationalYn(toYN(get(r, "NATION_ALSFC_AT")));
            f.setEarthquakeYn(toYN(get(r, "ERDSGN_AT")));
            f.setSelfcheckYn(toYN(get(r, "ATNM_CHCK_TRGET_AT")));

            f.setDataOriginCd(get(r, "DATA_ORIGIN_FLAG_CD"));
            f.setDeletedYn(toYN(get(r, "DEL_AT")));

            // 원천 값 보존(없으면 @Creation/@UpdateTimestamp가 채움)
            LocalDateTime createdAt = toLocalDateTime(get(r, "REGIST_DT"));
            LocalDateTime updatedAt = toLocalDateTime(get(r, "UPDT_DT"));
            if (createdAt != null) f.setCreatedAt(createdAt);
            if (updatedAt != null) f.setUpdatedAt(updatedAt);

            // ---------- children 교체(업서트시) ----------
            if (upsert) {
                f.getAddresses().clear();
                f.getOwners().clear();
                f.getContacts().clear();
            }

            // Address (1개 행 = 1개 주소 생성)
            FacilityAddress a = FacilityAddress.builder()
                    .facility(f)
                    .roadZip(trunc(get(r, "ROAD_NM_ZIP_NO"), 6))
                    .roadAddr1(trunc(get(r, "RDNMADR_ONE_NM"), 500))
                    .roadAddr2(trunc(get(r, "RDNMADR_TWO_NM"), 500))
                    .zipValue(trunc(get(r, "ZIP_NO_VALUE"), 6))
                    .addr1(trunc(get(r, "FCLTY_ADDR_ONE_NM"), 500))
                    .addr2(trunc(get(r, "FCLTY_ADDR_TWO_NM"), 500))
                    .sidoCd(trunc(get(r, "CTPRVN_CD"), 20))
                    .sidoNm(trunc(get(r, "CTPRVN_NM"), 50))
                    .sigunguCd(trunc(get(r, "SIGNGU_CD"), 20))
                    .sigunguNm(trunc(get(r, "SIGNGU_NM"), 50))
                    .mngSidoCd(trunc(get(r, "FCLTY_MANAGE_CTPRVN_CD"), 10))
                    .mngSidoNm(trunc(get(r, "FCLTY_MANAGE_CTPRVN_NM"), 50))
                    .mngSigunguCd(trunc(get(r, "FCLTY_MANAGE_SIGNGU_CD"), 10))
                    .mngSigunguNm(trunc(get(r, "FCLTY_MANAGE_SIGNGU_NM"), 50))
                    .mngEmdCd(trunc(get(r, "FCLTY_MANAGE_EMD_CD"), 20))
                    .mngEmdNm(trunc(get(r, "FCLTY_MANAGE_EMD_NM"), 50))
                    .mngLiCd(trunc(get(r, "FCLTY_MANAGE_LI_CD"), 20))
                    .mngLiNm(trunc(get(r, "FCLTY_MANAGE_LI_NM"), 20))
                    .lat(toBigDecimal(get(r, "FCLTY_LA")))
                    .lng(toBigDecimal(get(r, "FCLTY_LO")))
                    .build();
            f.getAddresses().add(a);

            // Owner
            FacilityOwner o = FacilityOwner.builder()
                    .facility(f)
                    .ownerCd(trunc(get(r, "POSESN_MBY_CD"), 20))
                    .ownerNm(trunc(get(r, "POSESN_MBY_NM"), 200))
                    .ownerSidoCd(trunc(get(r, "POSESN_MBY_CTPRVN_CD"), 20))
                    .ownerSidoNm(trunc(get(r, "POSESN_MBY_CTPRVN_NM"), 50))
                    .ownerSigunguCd(trunc(get(r, "POSESN_MBY_SIGNGU_CD"), 20))
                    .ownerSigunguNm(trunc(get(r, "POSESN_MBY_SIGNGU_NM"), 50))
                    .build();
            f.getOwners().add(o);

            // Contact
            FacilityContact c = FacilityContact.builder()
                    .facility(f)
                    .managerName(trunc(get(r, "RSPNSBLTY_NM"), 200))
                    .managerPhone(trunc(normalizePhone(get(r, "RSPNSBLTY_TEL_NO")), 100))
                    .build();
            f.getContacts().add(c);

            batch.add(f);
            count++;

            if (batch.size() >= BATCH) {
                repo.saveAll(batch); repo.flush(); batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            repo.saveAll(batch); repo.flush();
        }
        return count;
    }

    // =================== 파서/유틸 ===================

    private static String get(CSVRecord r, String header) {
        try { return Optional.ofNullable(r.get(header)).orElse("").trim(); }
        catch (IllegalArgumentException e) { return ""; }
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }

    private static Integer toInteger(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.valueOf(s.replaceAll(",", "")); }
        catch (Exception e) { return null; }
    }

    /** 소수 입력을 정수 필드에 넣어야 할 때 절삭 */
    private static Integer toIntegerFloor(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            double v = Double.parseDouble(s.replaceAll(",", ""));
            return (int)Math.floor(v);
        } catch (Exception e) { return null; }
    }

    private static BigDecimal toBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s.replaceAll(",", "")); }
        catch (Exception e) { return null; }
    }

    /** yyyyMMdd 전용(원천 포맷 기준). 다른 포맷 섞이면 필요 시 확장 */
    private static LocalDate toLocalDate(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim();
        if (v.length() == 8 && v.chars().allMatch(Character::isDigit)) {
            int y = Integer.parseInt(v.substring(0,4));
            int m = Integer.parseInt(v.substring(4,6));
            int d = Integer.parseInt(v.substring(6,8));
            try { return LocalDate.of(y, m, d); } catch (Exception e) { return null; }
        }
        return null;
    }

    /** ISO-8601 우선, 실패하면 yyyyMMdd를 자정으로 */
    private static LocalDateTime toLocalDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s.trim()); }
        catch (Exception ignore) {
            LocalDate d = toLocalDate(s);
            return (d != null) ? d.atStartOfDay() : null;
        }
    }

    /** 다양한 입력값을 'Y'/'N'로 정규화 (모르면 null) */
    private static final Set<String> TRUE_SET = Set.of("Y","1","TRUE","T","YES","예");
    private static final Set<String> FALSE_SET = Set.of("N","0","FALSE","F","NO","아니오");

    private static String toYN(String s) {
        if (s == null || s.isBlank()) return null;
        String v = s.trim().toUpperCase(Locale.ROOT);
        if (TRUE_SET.contains(v)) return "Y";
        if (FALSE_SET.contains(v)) return "N";
        return null;
    }

    private static String normalizePhone(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("02") && (digits.length()==9 || digits.length()==10))
            return digits.replaceFirst("(02)(\\d{3,4})(\\d{4})", "$1-$2-$3");
        if (digits.length()==10)
            return digits.replaceFirst("(\\d{3})(\\d{3})(\\d{4})", "$1-$2-$3");
        if (digits.length()==11)
            return digits.replaceFirst("(\\d{3})(\\d{4})(\\d{4})", "$1-$2-$3");
        if (digits.length()==8)
            return digits.replaceFirst("(\\d{4})(\\d{4})", "$1-$2");
        return raw.trim();
    }

    private static String trunc(String s, int max) {
        if (s == null) return null;
        return (s.length() <= max) ? s : s.substring(0, max);
    }
}
