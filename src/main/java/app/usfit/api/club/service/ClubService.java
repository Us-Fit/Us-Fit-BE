package app.usfit.api.club.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import app.usfit.api.config.S3Config;
import org.springframework.stereotype.Service;

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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

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
                .regionCode(req.getRegionCode())
                .regionName(req.getRegionName())
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

        // ownerId들을 수집해 한 번에 프로필 조회
        List<Long> ownerIds = clubs.stream()
                .map(c -> c.getOwner() != null ? c.getOwner().getId() : null)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, SimpleProfileResponse> ownerProfiles =
                ownerIds.isEmpty() ? Map.of() : profileService.getSimpleProfiles(ownerIds);

        return clubs.stream()
                .map(club -> {
                    // owner 프로필
                    SimpleProfileResponse ownerProfile = null;
                    if (club.getOwner() != null) {
                        ownerProfile = ownerProfiles.get(club.getOwner().getId());
                    }

                    // sports -> DTO
                    List<ClubSportResponse> sports = club.getSports() == null
                            ? List.of()
                            : club.getSports().stream()
                            .map(s -> new ClubSportResponse(
                                    // 조인 테이블 id 대신 실제 sport id를 쓰고 싶으면 이렇게:
                                    s.getSport() != null ? s.getSport().getId() : null,
                                    s.getSport() != null ? s.getSport().getName() : null
                            ))
                            .collect(Collectors.toList());

                    // DTO 변환
                    return ClubSimpleInfoResponse.from(club, ownerProfile, sports);
                })
                .collect(Collectors.toList());
    }
}
