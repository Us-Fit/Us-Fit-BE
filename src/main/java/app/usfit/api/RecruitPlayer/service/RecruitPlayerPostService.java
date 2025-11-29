package app.usfit.api.RecruitPlayer.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostRequest;
import app.usfit.api.RecruitPlayer.dto.RecruitPlayerPostResponse;
import app.usfit.api.RecruitPlayer.entity.RecruitPlayerPost;
import app.usfit.api.RecruitPlayer.repository.RecruitPlayerPostRepository;
import app.usfit.api.sport.entity.Sport;
import app.usfit.api.sport.service.SportService;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.ProfileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class RecruitPlayerPostService {
    @Autowired
    private final RecruitPlayerPostRepository repository;
    private final SportService sportService;
    private final ProfileService profileService;

    @PersistenceContext
    private EntityManager entityManager;

    public RecruitPlayerPostService(RecruitPlayerPostRepository repository, SportService sportService, ProfileService profileService) {
        this.repository = repository;
        this.sportService = sportService;
        this.profileService = profileService;
    }
    
    @Transactional
    public RecruitPlayerPost createPost(RecruitPlayerPostRequest req, Long writerId) {
        // 작성자 User 엔티티 참조 가져오기
        User writer = entityManager.getReference(User.class, writerId);
        if (writer == null) {
            throw new IllegalArgumentException("작성자 User를 찾을 수 없습니다.");
        }
        // Sport 엔티티 참조 가져오기
        String sportName = req.getSportName();
        if (sportName == null || sportName.isEmpty()) {
            throw new IllegalArgumentException("운동 이름이 제공되지 않았습니다.");
        }

        Map<String, Sport> sportMap = sportService.findByNamesAsMap(List.of(sportName));
        Sport sport = sportMap.get(sportName.trim().toLowerCase());

        if (sport == null) {
            throw new IllegalArgumentException("스포츠 유형을 찾을 수 없습니다.");            
        }

        RecruitPlayerPost post = new RecruitPlayerPost();
        
        post.setWriter(writer);
        post.setSport(sport);
        post.setTitle(req.getTitle());
        post.setDescription(req.getDescription());
        post.setFacilityId(req.getFacilityId());
        post.setRecruitDeadline(req.getRecruitDeadline());
        post.setActivityStartTime(req.getActivityStartTime());
        post.setActivityDurationMinutes(req.getActivityDurationMinutes());
        post.setMaxMember(req.getMaxMember());
        post.setIsActive(true);
        post.setSidoNm(req.getSidoNm());
        post.setSigunguNm(req.getSigunguNm());

        return repository.save(post);
    }

    public List<RecruitPlayerPost> getActivePosts() {
        return repository.findByIsActiveTrue();
    }

    public List<RecruitPlayerPost> getMyPosts(Long userId, boolean activeOnly) {
        if (activeOnly) {
            return repository.findByWriter_IdAndIsActiveTrueOrderByIdDesc(userId);
        } else {
            return repository.findByWriter_IdOrderByIdDesc(userId);
        }
    }

    /**
     * 우선순위 기준으로 용병(RecruitPost) 추천 반환
     * 우선순위 (운동 있음):
     * 1. 시군구 일치 + 운동 일치
     * 2. 시도 일치 + 운동 일치
     * 3. 운동 일치
     * 4. 지역 일치 (시군구 OR 시도)
     * 5. 나머지 (전체 활성글)
     *
     * 우선순위 (운동 없음):
     * 1. 시군구 일치
     * 2. 시도 일치
     * 3. 나머지
     *
     * 반환: RecruitPost 엔티티 리스트 (정렬: 우선순위 기준 누적 후 내부적으로 최신순)
     */
    @Transactional(readOnly = true)
    public List<RecruitPlayerPostResponse> recommendRecruits(List<String> sports, String sidoNm, String sigunguNm, int limit) {
        final int maxLimit = (limit <= 0) ? 20 : limit;

        // 정규화: 소문자 trim, 중복 제거
        List<String> normNames = (sports == null) ? List.of()
                : sports.stream()
                        .filter(Objects::nonNull)
                        .map(s -> s.trim().toLowerCase())
                        .filter(s -> !s.isBlank())   // 빈 문자열 제거
                        .distinct()
                        .collect(Collectors.toList());

        List<Long> sportIds = List.of();
        if (!normNames.isEmpty()) {
            try {
                Map<String, Sport> sportMap = sportService.findByNamesAsMap(normNames); // key: lowerName
                sportIds = sportMap.values().stream()
                        .map(Sport::getId)
                        .filter(Objects::nonNull)
                        .distinct()
                        .collect(Collectors.toList());
                System.out.println("[RECOMMEND DEBUG] resolved sportIds=" + sportIds + " for names=" + normNames);
            } catch (Exception e) {
                System.out.println("[RECOMMEND WARN] sportService mapping failed, fallback to name-based queries");
            }
        }

        String lowSido = sidoNm == null ? null : sidoNm.trim().toLowerCase();
        String lowSigungu = sigunguNm == null ? null : sigunguNm.trim().toLowerCase();

        List<Long> orderedIds = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        Consumer<List<Long>> appendUniqueLimited = ids -> {
            if (ids == null) return;
            for (Long id : ids) {
                if (orderedIds.size() >= maxLimit) break;
                if (seen.add(id)) orderedIds.add(id);
            }
        };

        System.out.println("[RECOMMEND DEBUG] input sports=" + sports + " normNames=" + normNames + " lowSido=" + lowSido + " lowSigungu=" + lowSigungu);

        // (운동 있음) 그룹1~5
        if (!normNames.isEmpty()) {
            // group1: 운동+시군구+시도
            if (lowSigungu != null && !lowSigungu.isBlank() && lowSido != null && !lowSido.isBlank()) {
                List<Long> g1;
                if (!sportIds.isEmpty()) {
                    g1 = entityManager.createQuery(
                            "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true AND r.sport.id IN :sportIds AND LOWER(r.sigunguNm) = :sigungu AND LOWER(r.sidoNm) = :sido ORDER BY r.updatedAt DESC",
                            Long.class)
                            .setParameter("sportIds", sportIds)
                            .setParameter("sigungu", lowSigungu)
                            .setParameter("sido", lowSido)
                            .setMaxResults(maxLimit)
                            .getResultList();
                } else {
                    g1 = entityManager.createQuery(
                            "SELECT r.id FROM RecruitPlayerPost r JOIN r.sport s WHERE r.isActive = true AND LOWER(s.name) IN :names AND LOWER(r.sigunguNm) = :sigungu AND LOWER(r.sidoNm) = :sido ORDER BY r.updatedAt DESC",
                            Long.class)
                            .setParameter("names", normNames)
                            .setParameter("sigungu", lowSigungu)
                            .setParameter("sido", lowSido)
                            .setMaxResults(maxLimit)
                            .getResultList();
                }
                appendUniqueLimited.accept(g1);
                System.out.println("[RECOMMEND DEBUG] group1 returned=" + (g1==null?0:g1.size()));
            }

            // group2: 운동+시도
            if (orderedIds.size() < maxLimit && lowSido != null && !lowSido.isBlank()) {
                List<Long> g2;
                if (!sportIds.isEmpty()) {
                    g2 = entityManager.createQuery(
                            "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true AND r.sport.id IN :sportIds AND LOWER(r.sidoNm) = :sido ORDER BY r.updatedAt DESC",
                            Long.class)
                            .setParameter("sportIds", sportIds)
                            .setParameter("sido", lowSido)
                            .setMaxResults(maxLimit)
                            .getResultList();
                } else {
                    g2 = entityManager.createQuery(
                            "SELECT r.id FROM RecruitPlayerPost r JOIN r.sport s WHERE r.isActive = true AND LOWER(s.name) IN :names AND LOWER(r.sidoNm) = :sido ORDER BY r.updatedAt DESC",
                            Long.class)
                            .setParameter("names", normNames)
                            .setParameter("sido", lowSido)
                            .setMaxResults(maxLimit)
                            .getResultList();
                }
                appendUniqueLimited.accept(g2);
                System.out.println("[RECOMMEND DEBUG] group2 returned=" + (g2==null?0:g2.size()));
            }

            // group3: 운동 전체
            if (orderedIds.size() < maxLimit) {
                List<Long> g3;
                if (!sportIds.isEmpty()) {
                    g3 = entityManager.createQuery(
                            "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true AND r.sport.id IN :sportIds ORDER BY r.updatedAt DESC",
                            Long.class)
                            .setParameter("sportIds", sportIds)
                            .setMaxResults(maxLimit)
                            .getResultList();
                } else {
                    g3 = entityManager.createQuery(
                            "SELECT r.id FROM RecruitPlayerPost r JOIN r.sport s WHERE r.isActive = true AND LOWER(s.name) IN :names ORDER BY r.updatedAt DESC",
                            Long.class)
                            .setParameter("names", normNames)
                            .setMaxResults(maxLimit)
                            .getResultList();
                }
                appendUniqueLimited.accept(g3);
                System.out.println("[RECOMMEND DEBUG] group3 returned=" + (g3==null?0:g3.size()));
            }

            // group4: 지역 일치 (sigungu OR sido)
            if (orderedIds.size() < maxLimit && (lowSigungu != null || lowSido != null)) {
                List<Long> g4 = entityManager.createQuery(
                        "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true AND ( (:sigungu IS NOT NULL AND LOWER(r.sigunguNm)=:sigungu) OR (:sido IS NOT NULL AND LOWER(r.sidoNm)=:sido) ) ORDER BY r.updatedAt DESC",
                        Long.class)
                        .setParameter("sigungu", lowSigungu)
                        .setParameter("sido", lowSido)
                        .setMaxResults(maxLimit)
                        .getResultList();
                appendUniqueLimited.accept(g4);
                System.out.println("[RECOMMEND DEBUG] group4 returned=" + (g4==null?0:g4.size()));
            }

            // group5: 나머지
            if (orderedIds.size() < maxLimit) {
                List<Long> g5 = entityManager.createQuery(
                        "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true ORDER BY r.updatedAt DESC",
                        Long.class)
                        .setMaxResults(maxLimit)
                        .getResultList();
                appendUniqueLimited.accept(g5);
                System.out.println("[RECOMMEND DEBUG] group5 returned=" + (g5==null?0:g5.size()));
            }
        }
        // (운동 없음) sigungu -> sido -> rest
        else {
            if (lowSigungu != null && !lowSigungu.isBlank()) {
                List<Long> g1 = entityManager.createQuery(
                        "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true AND LOWER(r.sigunguNm) = :sigungu ORDER BY r.updatedAt DESC",
                        Long.class)
                        .setParameter("sigungu", lowSigungu)
                        .setMaxResults(maxLimit)
                        .getResultList();
                appendUniqueLimited.accept(g1);
                System.out.println("[RECOMMEND DEBUG] (no-sport) group1 returned=" + (g1==null?0:g1.size()));
            }
            if (orderedIds.size() < maxLimit && lowSido != null && !lowSido.isBlank()) {
                List<Long> g2 = entityManager.createQuery(
                        "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true AND LOWER(r.sidoNm) = :sido ORDER BY r.updatedAt DESC",
                        Long.class)
                        .setParameter("sido", lowSido)
                        .setMaxResults(maxLimit)
                        .getResultList();
                appendUniqueLimited.accept(g2);
                System.out.println("[RECOMMEND DEBUG] (no-sport) group2 returned=" + (g2==null?0:g2.size()));
            }
            if (orderedIds.size() < maxLimit) {
                List<Long> g3 = entityManager.createQuery(
                        "SELECT r.id FROM RecruitPlayerPost r WHERE r.isActive = true ORDER BY r.updatedAt DESC",
                        Long.class)
                        .setMaxResults(maxLimit)
                        .getResultList();
                appendUniqueLimited.accept(g3);
                System.out.println("[RECOMMEND DEBUG] (no-sport) group3 returned=" + (g3==null?0:g3.size()));
            }
        }

        if (orderedIds.isEmpty()) return List.of();

        System.out.println("[RECOMMEND DEBUG] orderedIds(final)=" + orderedIds);

        // 엔티티 한 번 로드 (writer, sport fetch)
        List<RecruitPlayerPost> posts = entityManager.createQuery(
                "SELECT r FROM RecruitPlayerPost r LEFT JOIN FETCH r.writer LEFT JOIN FETCH r.sport WHERE r.id IN :ids",
                RecruitPlayerPost.class)
                .setParameter("ids", orderedIds)
                .getResultList();

        // id->entity 맵 만들고 orderedIds 순으로 재조립 (우선순위 보장)
        Map<Long, RecruitPlayerPost> byId = posts.stream()
                .filter(p -> p.getId() != null)
                .collect(Collectors.toMap(RecruitPlayerPost::getId, p -> p, (a,b)->a));

        List<RecruitPlayerPost> finalList = new ArrayList<>();
        for (Long id : orderedIds) {
            RecruitPlayerPost p = byId.get(id);
            if (p == null) continue;
            finalList.add(p);
            if (finalList.size() >= maxLimit) break;
        }

        System.out.println("[RECOMMEND DEBUG] finalList ids=" + finalList.stream().map(RecruitPlayerPost::getId).collect(Collectors.toList()));

        // writer 프로필 batch 조회
        Set<Long> writerIds = finalList.stream().map(p -> p.getWriter() != null ? p.getWriter().getId() : null).filter(Objects::nonNull).collect(Collectors.toSet());
        final Map<Long, SimpleProfileResponse> profiles = writerIds.isEmpty() ? Map.of() : profileService.getSimpleProfiles(new ArrayList<>(writerIds));

        // DTO 변환
        return finalList.stream().map(p -> {
            SimpleProfileResponse writerDto = p.getWriter() != null ? profiles.get(p.getWriter().getId()) : null;
            return new RecruitPlayerPostResponse(
                    p.getId(),
                    writerDto,
                    p.getSport() != null ? p.getSport().getId() : null,
                    p.getSport() != null ? p.getSport().getName() : null,
                    p.getTitle(),
                    p.getDescription(),
                    p.getRecruitDeadline(),
                    p.getActivityStartTime(),
                    p.getActivityDurationMinutes(),
                    p.getMaxMember(),
                    p.getIsActive(),
                    p.getFacilityId(),
                    p.getSidoNm(),
                    p.getSigunguNm()
            );
        }).collect(Collectors.toList());
    }
}