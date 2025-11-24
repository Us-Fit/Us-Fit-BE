package app.usfit.api.club.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import app.usfit.api.club.DTO.ClubSportRequest;
import app.usfit.api.club.DTO.CreateClubRequest;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.entity.ClubMember;
import app.usfit.api.club.entity.ClubSport;
import app.usfit.api.club.repository.ClubRepository;
import app.usfit.api.facility.entity.Facility;
import app.usfit.api.sport.entity.Sport;
import app.usfit.api.sport.service.SportService;
import app.usfit.api.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;

@Service
public class ClubService {
    private final ClubRepository clubRepository;
    private final SportService sportService;
    
    @PersistenceContext
    private EntityManager entityManager;

    public ClubService(ClubRepository clubRepository, SportService sportService) {
        this.clubRepository = clubRepository;
        this.sportService = sportService;
    }

    @Transactional
    public Club createClub(CreateClubRequest req, Long ownerId) {
        User owner = entityManager.find(User.class, ownerId);

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
            Facility fRef = entityManager.getReference(Facility.class, req.getMainFacilityId());
            builder.mainFacility(fRef);
        }

        Club club = builder.build();
        club = clubRepository.save(club);

        // 소유자를 ClubMember로 추가 (role: owner)
        ClubMember ownerMember = ClubMember.builder()
                .club(club)
                .user(owner)
                .role("owner")
                .status("active")
                .build();
        // 양방향이 매핑되어 있다면 members에 추가
        entityManager.persist(ownerMember);
        
        // 선택된 운동들 처리
        if (req.getSports() != null && !req.getSports().isEmpty()) {
            List<String> sportNames = req.getSports().stream()
                    .map(s -> s.getSportName().trim().toLowerCase())
                    .toList();

            Map<String, Sport> sportMap = sportService.findByNamesAsMap(sportNames);
            
            for (ClubSportRequest sreq : req.getSports()) {
                String norm = sreq.getSportName().trim().toLowerCase();
                Sport sportEntity = sportMap.get(norm);

                if (sportEntity == null) {
                    continue; // 없는 운동은 무시   
                }

                ClubSport cs = ClubSport.builder()
                        .sport(sportEntity)
                        .levelMin(sreq.getLevelMin())
                        .levelMax(sreq.getLevelMax())
                        .note(sreq.getNote())
                        .build();
                cs.setClub(club);
                entityManager.persist(cs);
            }
            
        }
        
        return club;
    }

    // 동호회 목록 조회
    public List<Club> listClubs() {
        return clubRepository.findAll();
    }
}
