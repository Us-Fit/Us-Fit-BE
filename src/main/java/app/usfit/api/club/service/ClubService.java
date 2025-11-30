package app.usfit.api.club.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.club.DTO.ClubCreatedResponse;
import app.usfit.api.club.DTO.ClubSimpleInfoResponse;
import app.usfit.api.club.DTO.ClubSportRequest;
import app.usfit.api.club.DTO.ClubSportResponse;
import app.usfit.api.club.DTO.CreateClubRequest;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubMember;
import app.usfit.api.club.entity.ClubSport;
import app.usfit.api.club.repository.ClubRepository;
import app.usfit.api.facility.entity.Facility;
import app.usfit.api.sport.entity.Sport;
import app.usfit.api.sport.service.SportService;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.ProfileService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
public class ClubService {
    private final ClubRepository clubRepository;
    private final SportService sportService;
    private final ProfileService profileService;
    private final ClubImageService clubImageService;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ClubService(ClubRepository clubRepository, SportService sportService, ProfileService profileService, ClubImageService clubImageService) {
        this.clubRepository = clubRepository;
        this.sportService = sportService;
        this.profileService = profileService;
        this.clubImageService = clubImageService;
    }

    //클럽 생성
    @Transactional
    public ClubCreatedResponse createClub(CreateClubRequest req,
                                          MultipartFile mainImage,
                                          Long ownerId) {
        User owner = entityManager.find(User.class, ownerId);
        if (owner == null) {
            throw new IllegalArgumentException("소유자(user)가 존재하지 않습니다. id=" + ownerId);
        }

        Club.ClubBuilder builder = Club.builder()
                .owner(owner)
                .name(req.getName())
                .description(req.getDescription())
                .sidoNm(req.getSidoNm())
                .sigunguNm(req.getSigunguNm())
                .memberLimit(req.getMemberLimit())
                .visibility(req.getVisibility())
                .status(req.getStatus())
                .phoneNumber(req.getPhoneNumber())
                .snsLink(req.getSnsLink());

        if (req.getMainFacilityId() != null) {
            try {
                Facility fRef = entityManager.getReference(Facility.class, req.getMainFacilityId());
                builder.mainFacility(fRef);
            } catch (jakarta.persistence.EntityNotFoundException ex) {
                // 선택한 시설이 없으면 무시
            }
        }

        // 1) club 먼저 저장해서 clubId 확보
        Club club = builder.build();
        club = clubRepository.save(club);

        // 2) 메인 이미지가 있다면 업로드 후 key 저장
        if (mainImage != null && !mainImage.isEmpty()) {
            String imageKey = clubImageService.uploadClubMainImage(club.getId(), ownerId, mainImage);
            club.setClubMainImageUrl(imageKey);   // 여기서 프로필 이미지 세팅
            // 트랜잭션 안이니까 별도 save() 안 해도 flush 시점에 업데이트 됨
        }

        // 3) 소유자를 ClubMember로 추가
        ClubMember ownerMember = ClubMember.builder()
                .club(club)
                .user(owner)
                .role("owner")
                .status("active")
                .build();
        entityManager.persist(ownerMember);
        if (club.getMembers() != null) {
            club.getMembers().add(ownerMember);
        }

        // 4) 종목 처리
        if (req.getSports() != null && !req.getSports().isEmpty()) {
            List<String> sportNames = req.getSports().stream()
                    .map(s -> s.getSportName().trim().toLowerCase())
                    .toList();

            Map<String, Sport> sportMap = sportService.findByNamesAsMap(sportNames);

            for (ClubSportRequest sreq : req.getSports()) {
                String norm = sreq.getSportName().trim().toLowerCase();
                Sport sportEntity = sportMap.get(norm);
                if (sportEntity == null) continue;

                ClubSport cs = ClubSport.builder()
                        .sport(sportEntity)
                        .levelMin(sreq.getLevelMin())
                        .levelMax(sreq.getLevelMax())
                        .note(sreq.getNote())
                        .build();
                cs.setClub(club);
                entityManager.persist(cs);
                if (club.getSports() != null) {
                    club.getSports().add(cs);
                }
            }
        }

        SimpleProfileResponse ownerProfile = profileService.getSimpleProfile(ownerId);
        Long mainFacilityId = club.getMainFacility() != null ? club.getMainFacility().getId() : null;

        return new ClubCreatedResponse(
                club.getId(),
                club.getName(),
                club.getDescription(),
                ownerProfile,
                mainFacilityId
        );
    }

    // 동호회 목록 조회 (간단 정보 DTO 반환)
    @Transactional(readOnly = true)
    public List<ClubSimpleInfoResponse> listClubs() {
        List<Club> clubs = clubRepository.findAll();
        return mapToSimpleResponses(clubs);
    }

    // 내가 속한 동호회 목록 조회
    @Transactional(readOnly = true)
    public List<ClubSimpleInfoResponse> listMyClubs(Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        List<Club> myClubs = entityManager.createQuery(
                "SELECT DISTINCT cm.club FROM ClubMember cm WHERE cm.user.id = :userId AND cm.status = :status", Club.class)
                .setParameter("userId", userId)
                .setParameter("status", "active")
                .getResultList();

        return mapToSimpleResponses(myClubs);
    }

    /**
     * 동호회 정보 수정 (소유자(owner)만 가능)
     * - request의 널 체크 후 필드별로 업데이트
     * - mainImage가 들어오면 S3 업로드 후 클럽의 mainImageKey 갱신
     */
    @Transactional
    public ClubCreatedResponse updateClub(Long clubId, Long userId, CreateClubRequest req, MultipartFile mainImage) {
        if (userId == null) {
            throw new IllegalArgumentException("userId가 필요합니다.");
        }

        Club club = entityManager.find(Club.class, clubId);
        if (club == null) {
            throw new IllegalArgumentException("존재하지 않는 동호회입니다.");
        }

        // 소유자 확인: club.owner 가 존재하고 일치해야 함
        if (club.getOwner() == null || !Objects.equals(club.getOwner().getId(), userId)) {
            throw new SecurityException("동호회 소유자만 동호회 정보를 수정할 수 있습니다.");
        }

        // 필드별 업데이트 (null 체크)
        if (req.getName() != null) club.setName(req.getName());
        if (req.getDescription() != null) club.setDescription(req.getDescription());
        if (req.getSidoNm() != null) club.setSidoNm(req.getSidoNm());
        if (req.getSigunguNm() != null) club.setSigunguNm(req.getSigunguNm());
        if (req.getMemberLimit() != null) club.setMemberLimit(req.getMemberLimit());
        if (req.getVisibility() != null) club.setVisibility(req.getVisibility());
        if (req.getStatus() != null) club.setStatus(req.getStatus());
        if (req.getPhoneNumber() != null) club.setPhoneNumber(req.getPhoneNumber());
        if (req.getSnsLink() != null) club.setSnsLink(req.getSnsLink());

        if (req.getMainFacilityId() != null) {
            try {
                var fRef = entityManager.getReference(app.usfit.api.facility.entity.Facility.class, req.getMainFacilityId());
                club.setMainFacility(fRef);
            } catch (jakarta.persistence.EntityNotFoundException ex) {
                // 선택한 시설이 없으면 무시
                
            }
        }
        // 종목 업데이트: 기존 종목 삭제 후 새로 추가
        if (req.getSports() != null) {
            // 1) DB에서 해당 클럽의 ClubSport 레코드를 확실히 삭제 (bulk delete)
            entityManager.createQuery("DELETE FROM ClubSport cs WHERE cs.club.id = :clubId")
                    .setParameter("clubId", clubId)
                    .executeUpdate();

            // 2) 엔티티의 컬렉션도 비움
            if (club.getSports() != null) {
                club.getSports().clear();
            }

            // 3) 새 종목 매핑 및 추가 (요청 내 중복 종목은 무시)
            List<String> sportNames = req.getSports().stream()
                    .map(s -> s.getSportName().trim().toLowerCase())
                    .collect(Collectors.toList());
            Map<String, Sport> sportMap = sportService.findByNamesAsMap(sportNames);

            // 추가된 sportId 추적하여 중복 삽입 방지
            Set<Long> addedSportIds = new HashSet<>();

            for (var sreq : req.getSports()) {
                String norm = sreq.getSportName().trim().toLowerCase();
                Sport sportEntity = sportMap.get(norm);
                if (sportEntity == null) continue;

                Long sportId = sportEntity.getId();
                if (!addedSportIds.add(sportId)) {
                    // 이미 추가된 종목이면 스킵
                    continue;
                }

                ClubSport cs = ClubSport.builder()
                        .sport(sportEntity)
                        .levelMin(sreq.getLevelMin())
                        .levelMax(sreq.getLevelMax())
                        .note(sreq.getNote())
                        .build();
                cs.setClub(club);
                entityManager.persist(cs);
                if (club.getSports() != null) {
                    club.getSports().add(cs);
                }
            }
        }

        // 메인 이미지 업로드 처리
        if (mainImage != null && !mainImage.isEmpty()) {
            try {
                // uploadClubMainImage는 (clubId, userId, MultipartFile) -> String key 을 반환한다고 가정
                String imageKey = clubImageService.uploadClubMainImage(clubId, userId, mainImage);
                if (imageKey != null) {
                    // createClub 에서는 setClubMainImageUrl 를 사용했으므로 동일하게 설정
                    try { club.setClubMainImageUrl(imageKey); } catch (Exception ignored) {}
                }
            } catch (Exception ex) {
                throw new IllegalStateException("메인 이미지 업로드 중 오류가 발생했습니다.", ex);
            }
        }

        club = entityManager.merge(club);

        // owner 프로필은 club.getOwner().getId() 와 다름 — 반환 시 실제 owner 프로필 조회
        SimpleProfileResponse ownerProfile = profileService.getSimpleProfile(club.getOwner().getId());
        return ClubCreatedResponse.fromEntity(club, ownerProfile);
    }

    /*
        * 추천 알고리즘 (지역 우선 분류)
        * 우선순위 (운동 있음):
     * 1. 시군구 일치 + 운동 일치
     * 2. 시도만 일치 + 운동 일치
     * 3. 운동일치
     * 4. 지역 일치 (시군구 or 시도)
     * 5. 나머지
     *
     * 우선순위 (운동 없음):
     * 1. 시군구 일치
     * 2. 시도 일치
     * 3. 나머지
    */
   @Transactional(readOnly = true)
    public List<ClubSimpleInfoResponse> recommendClubs(List<String> sports, String sidoNm, String sigunguNm, int limit) {
        final int maxLimit = (limit <= 0) ? 20 : limit;

        // 운동 세팅 - 정규화 (소문자, trim, 중복 제거)
        List<String> normNames = (sports == null) ? List.of()
                : sports.stream().filter(Objects::nonNull).map(s -> s.trim().toLowerCase()).distinct().collect(Collectors.toList());

        String lowSido = sidoNm == null ? null : sidoNm.trim().toLowerCase();
        String lowSigungu = sigunguNm == null ? null : sigunguNm.trim().toLowerCase();

        // 누적된 id 보관 (순서 유지, 중복 제거)
        List<Long> orderedIds = new ArrayList<>();
        Set<Long> seen = new HashSet<>();

        // helper: append unique up to remaining
        Consumer<List<Long>> appendUniqueLimited = ids -> {
            for (Long id : ids) {
                if (orderedIds.size() >= maxLimit) break;
                if (seen.add(id)) orderedIds.add(id);
            }
        };

        // 1) 운동이 있는 경우: group1~5 차례로 DB에서 id를 가져와 누적
        if (!normNames.isEmpty()) {
            // group1: 운동 일치 + sigungu match + sido match
            if (lowSigungu != null && !lowSigungu.isBlank()) {
                List<Long> g1 = entityManager.createQuery(
                        "SELECT c.id FROM Club c JOIN c.sports cs JOIN cs.sport s LEFT JOIN c.members m " +
                                "WHERE LOWER(s.name) IN :names AND LOWER(c.sigunguNm) = :sigungu AND LOWER(c.sidoNm) = :sido " +
                                "GROUP BY c.id ORDER BY COUNT(DISTINCT s.id) DESC, COUNT(DISTINCT m.id) DESC", Long.class)
                        .setParameter("names", normNames)
                        .setParameter("sigungu", lowSigungu)
                        .setParameter("sido", lowSido)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g1);
            }

            // group2: 운동 일치 + sido match
            if (orderedIds.size() < limit && lowSido != null && !lowSido.isBlank()) {
                List<Long> g2 = entityManager.createQuery(
                        "SELECT c.id FROM Club c JOIN c.sports cs JOIN cs.sport s LEFT JOIN c.members m " +
                                "WHERE LOWER(s.name) IN :names AND LOWER(c.sidoNm) = :sido " +
                                "GROUP BY c.id ORDER BY COUNT(DISTINCT s.id) DESC, COUNT(DISTINCT m.id) DESC", Long.class)
                        .setParameter("names", normNames)
                        .setParameter("sido", lowSido)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g2);
            }

            // group3: 운동일치 (모든 운동매칭 클럽)
            if (orderedIds.size() < limit) {
                List<Long> g3 = entityManager.createQuery(
                        "SELECT c.id FROM Club c JOIN c.sports cs JOIN cs.sport s LEFT JOIN c.members m " +
                                "WHERE LOWER(s.name) IN :names " +
                                "GROUP BY c.id ORDER BY COUNT(DISTINCT s.id) DESC, COUNT(DISTINCT m.id) DESC", Long.class)
                        .setParameter("names", normNames)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g3);
            }

            // group4: 지역 일치 (sigungu 또는 sido) - 멤버수 기준
            if (orderedIds.size() < limit && (lowSigungu != null || lowSido != null)) {
                List<Long> g4 = entityManager.createQuery(
                        "SELECT c.id FROM Club c LEFT JOIN c.members m WHERE " +
                                "(:sigungu IS NOT NULL AND LOWER(c.sigunguNm)=:sigungu) OR " +
                                "(:sido IS NOT NULL AND LOWER(c.sidoNm)=:sido) " +
                                "GROUP BY c.id ORDER BY COUNT(DISTINCT m.id) DESC", Long.class)
                        .setParameter("sigungu", lowSigungu)
                        .setParameter("sido", lowSido)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g4);
            }

            // group5: 나머지 (모든 클럽, 멤버수 기준)
            if (orderedIds.size() < limit) {
                List<Long> g5 = entityManager.createQuery(
                        "SELECT c.id FROM Club c LEFT JOIN c.members m GROUP BY c.id ORDER BY COUNT(DISTINCT m.id) DESC", Long.class)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g5);
            }

        }
        // 운동 없음: 그룹1 sigungu, group2 sido, group3 rest (그냥 지역순으로)
        else {
            if (lowSigungu != null && !lowSigungu.isBlank()) {
                List<Long> g1 = entityManager.createQuery(
                        "SELECT c.id FROM Club c LEFT JOIN c.members m WHERE LOWER(c.sigunguNm) = :sigungu GROUP BY c.id ORDER BY COUNT(DISTINCT m.id) DESC", Long.class)
                        .setParameter("sigungu", lowSigungu)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g1);
            }

            if (orderedIds.size() < limit && lowSido != null && !lowSido.isBlank()) {
                List<Long> g2 = entityManager.createQuery(
                        "SELECT c.id FROM Club c LEFT JOIN c.members m WHERE LOWER(c.sidoNm) = :sido GROUP BY c.id ORDER BY COUNT(DISTINCT m.id) DESC", Long.class)
                        .setParameter("sido", lowSido)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g2);
            }

            if (orderedIds.size() < limit) {
                List<Long> g3 = entityManager.createQuery(
                        "SELECT c.id FROM Club c LEFT JOIN c.members m GROUP BY c.id ORDER BY COUNT(DISTINCT m.id) DESC", Long.class)
                        .setMaxResults(limit)
                        .getResultList();
                appendUniqueLimited.accept(g3);
            }
        }

        if (orderedIds.isEmpty()) return List.of();

        // 한 번에 엔티티 로드 (IN 쿼리) -> map으로 재정렬
        List<Club> clubs = entityManager.createQuery("SELECT c FROM Club c WHERE c.id IN :ids", Club.class)
                .setParameter("ids", orderedIds)
                .getResultList();
        Map<Long, Club> clubById = clubs.stream().collect(Collectors.toMap(Club::getId, c -> c));

        List<Club> finalList = orderedIds.stream()
                .filter(clubById::containsKey)
                .limit(limit)
                .map(clubById::get)
                .collect(Collectors.toList());

        return mapToSimpleResponses(finalList);
    }

    // helper: Club -> ClubSimpleInfoResponse 매핑 (owner 프로필 batch 조회)
    private List<ClubSimpleInfoResponse> mapToSimpleResponses(List<Club> clubs) {
        if (clubs == null || clubs.isEmpty()) return List.of();

        // club ids
        List<Long> clubIds = clubs.stream()
                .map(Club::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (clubIds.isEmpty()) return List.of();

        // 1) owner 프로필 배치 조회 (기존)
        List<Long> ownerIds = clubs.stream()
                .map(c -> c.getOwner() != null ? c.getOwner().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, SimpleProfileResponse> ownerProfiles =
                ownerIds.isEmpty() ? Map.of() : profileService.getSimpleProfiles(ownerIds);

        // 2) club -> List<ClubSport> 매핑을 한번에 조회 (ClubSport + Sport fetch)
        List<ClubSport> clubSports = entityManager.createQuery(
                "SELECT cs FROM ClubSport cs JOIN FETCH cs.sport s WHERE cs.club.id IN :ids", ClubSport.class)
                .setParameter("ids", clubIds)
                .getResultList();
        Map<Long, List<ClubSport>> sportsByClub = clubSports.stream()
                .filter(cs -> cs.getClub() != null && cs.getClub().getId() != null)
                .collect(Collectors.groupingBy(cs -> cs.getClub().getId(), Collectors.toList()));

        // 3) club -> memberCount (한 번의 그룹쿼리)
        List<Object[]> counts = entityManager.createQuery(
                "SELECT cm.club.id, COUNT(cm) FROM ClubMember cm WHERE cm.club.id IN :ids GROUP BY cm.club.id", Object[].class)
                .setParameter("ids", clubIds)
                .getResultList();
        Map<Long, Long> memberCountByClub = counts.stream()
                .collect(Collectors.toMap(o -> (Long) o[0], o -> (Long) o[1]));

        // 4) 최종 매핑 (DB에서 미리 조회한 데이터만 사용 — lazy 접근 최소화)
        return clubs.stream().map(club -> {
            SimpleProfileResponse ownerProfile = club.getOwner() != null ? ownerProfiles.get(club.getOwner().getId()) : null;

            List<ClubSportResponse> sports = sportsByClub.getOrDefault(club.getId(), List.of()).stream()
                    .map(cs -> {
                        var s = cs.getSport();
                        return new ClubSportResponse(s != null ? s.getId() : null, s != null ? s.getName() : null);
                    })
                    .collect(Collectors.toList());

            return ClubSimpleInfoResponse.from(club, ownerProfile, sports);
        }).collect(Collectors.toList());
    }
}
