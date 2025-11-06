package app.usfit.api.user.service;

import java.util.HashSet;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import app.usfit.api.sport.entity.Sport;
import app.usfit.api.sport.repository.SportRepository;
import app.usfit.api.user.dto.InterestInput;
import app.usfit.api.user.dto.InterestOutput;
import app.usfit.api.user.dto.ProfileRequest;
import app.usfit.api.user.dto.ProfileResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.entity.UserInterestSport;
import app.usfit.api.user.entity.UserProfile;
import app.usfit.api.user.repository.UserInterestSportRepository;
import app.usfit.api.user.repository.UserProfileRepository;
import app.usfit.api.user.repository.UserRepository;

@Service
public class ProfileService {
    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final UserInterestSportRepository userInterestSportRepository;
    private final SportRepository sportRepository;

    public ProfileService(UserRepository userRepository,
                          UserProfileRepository userProfileRepository,
                          UserInterestSportRepository userInterestSportRepository,
                          SportRepository sportRepository) {
        this.userRepository = userRepository;
        this.userProfileRepository = userProfileRepository;
        this.userInterestSportRepository = userInterestSportRepository;
        this.sportRepository = sportRepository;
    }

    @Transactional
    public UserProfile upsert(Long userId, ProfileRequest req) {
        User userRef = userRepository.getReferenceById(userId);

        // 기존 프로필 조회 또는 새 프로필 생성
        UserProfile profile = userProfileRepository.findByUserId(userId)
            .orElseGet(() -> {
                var p = new UserProfile(userRef);
                p.setUser(userRef);
                return p;
            });

        // 프로필 값 저장
        profile.setNickname(req.nickname());
        profile.setGender(req.gender());
        profile.setHeight(req.height());
        profile.setWeight(req.weight());
        profile.setBirthDate(req.birthDate());
        profile.setPreferredArea(req.preferredArea());
        profile.setLat(req.lat());
        profile.setLng(req.lng());
        

        // 관심운동 저장(전체 교체)
        if (req.interests() != null) {
            userInterestSportRepository.deleteByUser_Id(userId);
            if (!req.interests().isEmpty()) {
                var links = new java.util.HashSet<UserInterestSport>();
                var seen = new java.util.HashSet<String>();
                for (InterestInput input : req.interests()) {
                    if (!seen.add(input.code())) continue;
                    var sport = sportRepository.findByCode(input.code())
                        .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 스포츠 코드: " + input.code()));
                    links.add(new UserInterestSport(userRef, sport, input.level()));
                }
                userInterestSportRepository.saveAll(links);
            }
        }
        
        return userProfileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        var profileOpt = userProfileRepository.findByUserId(userId);
        var links = userInterestSportRepository.findByUser_Id(userId);

        var interests = links.stream()
            .map(link -> new InterestOutput (
                link.getSport().getCode(),
                link.getSport().getName(),
                link.getSkillLevel()
            ))
            .collect(Collectors.toList());
        
        var p = profileOpt.orElseThrow(() -> new IllegalArgumentException("프로필 없음"));
        return new ProfileResponse(
            p.getNickname(),
            p.isGender(),
            p.getHeight(),
            p.getWeight(),
            p.getBirthDate(),
            p.getPreferredArea(),
            p.getLat(),
            p.getLng(),
            interests
        );
    }
}
