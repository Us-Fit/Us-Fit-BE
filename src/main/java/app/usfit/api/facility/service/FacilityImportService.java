package app.usfit.api.facility.service;

import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.entity.FacilityAddress;
import app.usfit.api.facility.entity.FacilityContact;
import app.usfit.api.facility.repository.FacilityRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.springframework.data.domain.PageRequest;
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
import java.math.RoundingMode;

@Service
public class FacilityImportService {

    private final FacilityRepository repo;

    @PersistenceContext
    private EntityManager em;

    public FacilityImportService(FacilityRepository repo) {
        this.repo = repo;
    }

    /** 업로드 파일에서 적재 */
    @Transactional
    public int importCsv(MultipartFile file, boolean upsert, Charset charset) throws Exception {
        try (Reader reader = new InputStreamReader(file.getInputStream(), charset)) {
            return parseAndPersist(reader, upsert);
        }
    }

    /** 로컬 경로에서 적재 */
    @Transactional
    public int importCsv(java.nio.file.Path path, boolean upsert, Charset charset) throws Exception {
        try (Reader reader = java.nio.file.Files.newBufferedReader(path, charset)) {
            return parseAndPersist(reader, upsert);
        }
    }

    // =================== 내부 구현 ===================

    private int parseAndPersist(Reader reader, boolean upsert) throws Exception {
        // 네가 쓴 헤더만 사용
        CSVFormat fmt = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setIgnoreEmptyLines(true)
                .setTrim(true)
                .setIgnoreSurroundingSpaces(true)
                .setAllowMissingColumnNames(true)
                .build();

        Iterable<CSVRecord> rows = fmt.parse(reader);

        // upsert면 기존 데이터 미리 가져오기 (페이징 or 키 기반)
        Map<String, Facility> existingMap = Collections.emptyMap();

        if (upsert) {
            existingMap = new HashMap<>();

            int pageSize = 2000;
            int page = 0;
            List<Facility> pageResult;

            do {
                pageResult = repo.findAll(PageRequest.of(page, pageSize)).getContent();

                for (Facility f : pageResult) {
                    String addr = (f.getAddresses().isEmpty()) ? "" : f.getAddresses().get(0).getRoadAddr1();
                    String key = makeKey(f.getName(), addr);
                    existingMap.put(key, f);
                }

                page++;
                //em.clear(); // 메모리 방지
            } while (!pageResult.isEmpty());
        }


        final int BATCH = 2000;
        List<Facility> batch = new ArrayList<>(BATCH);
        int count = 0;

        for (CSVRecord r : rows) {
            String name = get(r, "FCLTY_NM");
            if (name.isBlank()) continue;

            String statusValue = get(r, "FCLTY_STATE_VALUE"); // 운영중/폐업 등
            String delAt = get(r, "DEL_AT");                  // Y / N

            // 1) 폐업이면 버림
            if ("폐업".equals(statusValue)) continue;
            // 2) 삭제여부가 Y면 버림
            if ("Y".equalsIgnoreCase(delAt)) continue;

            String roadAddr1 = get(r, "RDNMADR_ONE_NM");

            Facility f;
            if (upsert) {
                String key = makeKey(name, roadAddr1);
                f = existingMap.getOrDefault(key, new Facility());
                f.getAddresses().clear();
                f.getContacts().clear();
            } else {
                f = new Facility();
            }

            // ---------- Facility ----------
            f.setName(name);
            f.setTypeName(get(r, "FCLTY_TY_NM"));
            f.setIndoorOutdoor(get(r, "NDOR_SDIV_NM"));
            f.setAreaSqm(toIntegerFloor(get(r, "FCLTY_AR_CO")));
            String self = toYN(get(r, "ATNM_CHCK_TRGET_AT"));
            f.setSelfcheckYn(self == null ? "N" : self);

            // 삭제여부는 여기서 강제 'N' 처리 (필터 안 걸린 건 다 유효데이터)
            f.setDeletedYn("N");

            // ---------- Address (값이 있을 때만) ----------
            boolean hasAnyAddress =
                    !get(r, "RDNMADR_ONE_NM").isBlank() ||
                            !get(r, "CTPRVN_NM").isBlank();

            if (hasAnyAddress) {
                FacilityAddress a = FacilityAddress.builder()
                        .facility(f)
                        .roadAddr1(trunc(get(r, "RDNMADR_ONE_NM"), 500))
                        .roadAddr2(trunc(get(r, "RDNMADR_TWO_NM"), 500))
                        .sidoNm(trunc(get(r, "CTPRVN_NM"), 50))
                        .sigunguNm(trunc(get(r, "SIGNGU_NM"), 50))
                        .lat(toCoord(get(r, "FCLTY_LA"), 90))
                        .lng(toCoord(get(r, "FCLTY_LO"), 180))
                        .build();
                f.getAddresses().add(a);
            }

            // ---------- Contact (전화 있는 경우에만) ----------
            String phone = get(r, "RSPNSBLTY_TEL_NO");
            if (!phone.isBlank()) {
                FacilityContact c = FacilityContact.builder()
                        .facility(f)
                        .managerPhone(trunc(normalizePhone(phone), 100))
                        .build();
                f.getContacts().add(c);
            }

            batch.add(f);
            count++;

            if (batch.size() >= BATCH) {
                repo.saveAll(batch);
                repo.flush();
                batch.clear();
                em.clear();
            }
        }

        if (!batch.isEmpty()) {
            repo.saveAll(batch);
            repo.flush();
            em.clear();
        }

        return count;
    }

    // =================== 유틸 ===================

    private static String makeKey(String name, String roadAddr1) {
        return (name == null ? "" : name) + "|" + (roadAddr1 == null ? "" : roadAddr1);
    }

    private static String get(CSVRecord r, String header) {
        try {
            return Optional.ofNullable(r.get(header)).orElse("").trim();
        } catch (IllegalArgumentException e) {
            return "";
        }
    }

    private static Integer toIntegerFloor(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            double v = Double.parseDouble(s.replaceAll(",", ""));
            return (int) Math.floor(v);
        } catch (Exception e) {
            return null;
        }
    }

    private static BigDecimal toBigDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return new BigDecimal(s.replaceAll(",", ""));
        } catch (Exception e) {
            return null;
        }
    }

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

    // 필요하면 아래 날짜 변환 유틸도 유지
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

    private static LocalDateTime toLocalDateTime(String s) {
        if (s == null || s.isBlank()) return null;
        try { return LocalDateTime.parse(s.trim()); }
        catch (Exception ignore) {
            LocalDate d = toLocalDate(s);
            return (d != null) ? d.atStartOfDay() : null;
        }
    }

    private static BigDecimal toCoord(String s, int maxAbs) {
        if (s == null || s.isBlank()) return null;
        try {
            BigDecimal v = new BigDecimal(s.replaceAll(",", ""));
            // 1) 위치가 말이 안 되면 버려 (예: 9999)
            if (v.abs().compareTo(BigDecimal.valueOf(maxAbs)) > 0) {
                return null;
            }
            // 2) 소수점 6자리까지만
            return v.setScale(6, RoundingMode.DOWN);
        } catch (Exception e) {
            return null;
        }
    }
}
