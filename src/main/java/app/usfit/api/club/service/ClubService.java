package app.usfit.api.club.service;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    // 동호회 FacilityId를 통해 필터링
    // 관심운동 필터를 통해 동호회 우선순위 지정
    @Transactional(readOnly = true)
    public List<ClubSimpleInfoResponse> recommendClubs(List<String> sports, Long facilityId, int limit) {
        List<String> normNames = (sports == null) ? List.of()
                : sports.stream().filter(Objects::nonNull).map(s -> s.trim().toLowerCase()).distinct().collect(Collectors.toList());

        // 운동 매칭 클럽
        List<Club> matchedBySport = normNames.isEmpty() ? List.of() :
                entityManager.createQuery(
                        "SELECT DISTINCT c FROM Club c JOIN c.sports cs JOIN cs.sport s WHERE LOWER(s.name) IN :names", Club.class)
                        .setParameter("names", normNames)
                        .getResultList();

        // facility 매칭 클럽 (facilityId 없으면 빈 리스트)
        List<Club> facilityMatched = (facilityId == null) ? List.of() :
                entityManager.createQuery("SELECT c FROM Club c WHERE c.mainFacility.id = :fid", Club.class)
                        .setParameter("fid", facilityId)
                        .getResultList();

        // 전체 클럽 (for fallback)
        List<Club> allClubs = entityManager.createQuery("SELECT c FROM Club c", Club.class).getResultList();

        // 매칭 카운트 계산 (운동 매칭 개수)
        Map<Long, Integer> matchCount = new HashMap<>();
        for (Club c : matchedBySport) {
            int cnt = 0;
            if (c.getSports() != null) {
                for (ClubSport cs : c.getSports()) {
                    if (cs.getSport() != null && normNames.contains(cs.getSport().getName().trim().toLowerCase())) cnt++;
                }
            }
            matchCount.put(c.getId(), cnt);
        }

        // id 집합 생성
        Set<Long> sportIds = matchedBySport.stream().map(Club::getId).collect(Collectors.toSet());
        Set<Long> facilityIds = facilityMatched.stream().map(Club::getId).collect(Collectors.toSet());

        // 그룹 분류
        List<Club> group1 = matchedBySport.stream()
                .filter(c -> facilityId != null && facilityIds.contains(c.getId()))
                .distinct()
                .collect(Collectors.toList()); // 시설+운동

        List<Club> group3 = matchedBySport.stream()
                .filter(c -> !facilityIds.contains(c.getId()))
                .distinct()
                .collect(Collectors.toList()); // 운동만

        List<Club> group2 = facilityMatched.stream()
                .filter(c -> !sportIds.contains(c.getId()))
                .distinct()
                .collect(Collectors.toList()); // 시설만

        // 나머지 (group4): allClubs에서 이미 포함된 것 제외
        Set<Long> included = Stream.of(group1, group2, group3)
                .flatMap(List::stream)
                .map(Club::getId)
                .collect(Collectors.toSet());

        List<Club> group4 = allClubs.stream()
                .filter(c -> !included.contains(c.getId()))
                .collect(Collectors.toList());

        // 정렬 규칙: group1 -> 시설일치 우선 + matchCount desc + memberCount desc
        Comparator<Club> byMatchThenMembers = Comparator
                .comparing((Club c) -> matchCount.getOrDefault(c.getId(), 0), Comparator.reverseOrder())
                .thenComparing((Club c) -> c.getMembers() != null ? c.getMembers().size() : 0, Comparator.reverseOrder());

        group1.sort(byMatchThenMembers);
        // group2: 시설만 - 멤버수 기준 내림차순
        group2.sort(Comparator.comparing((Club c) -> c.getMembers() != null ? c.getMembers().size() : 0, Comparator.reverseOrder()));
        // group3: 운동만 - matchCount desc then members desc
        group3.sort(byMatchThenMembers);
        // group4: 멤버수 기준 내림차순
        group4.sort(Comparator.comparing((Club c) -> c.getMembers() != null ? c.getMembers().size() : 0, Comparator.reverseOrder()));

        // 합치기 및 limit 적용
        List<Club> combined = Stream.of(group1, group2, group3, group4)
                .flatMap(List::stream)
                .filter(Objects::nonNull)
                .distinct()
                .limit(Math.max(0, limit))
                .collect(Collectors.toList());

        return mapToSimpleResponses(combined);
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
