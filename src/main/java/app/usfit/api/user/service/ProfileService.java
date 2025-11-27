package app.usfit.api.user.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.sport.repository.SportRepository;
import app.usfit.api.user.dto.InterestInput;
import app.usfit.api.user.dto.InterestOutput;
import app.usfit.api.user.dto.ProfileRequest;
import app.usfit.api.user.dto.ProfileResponse;
import app.usfit.api.user.dto.SimpleProfileResponse;
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

    @Autowired
    private UserProfileImageService userProfileImageService;

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
                // 요청에서 사용하는 이름을 정규화하여 집합으로 추출
                var normalizedNames = req.interests().stream()
                    .map(input -> input.sportName().trim().toLowerCase())
                    .collect(Collectors.toSet());

                // 한 번의 쿼리로 모든 Sport 엔티티 로드 (N+1 회피)
                var sports = sportRepository.findAllByNormalizedNames(normalizedNames);
                var sportByName = sports.stream()
                    .collect(Collectors.toMap(s -> s.getName().trim().toLowerCase(), s -> s));

                var links = new HashSet<UserInterestSport>();
                var seen = new HashSet<String>();
                for (InterestInput input : req.interests()) {
                    String norm = input.sportName().trim().toLowerCase();
                    if (!seen.add(norm)) continue;

                    var sport = sportByName.get(norm);
                    if (sport == null) {
                        throw new IllegalArgumentException("스포츠 유형을 찾을 수 없습니다: " + input.sportName());
                    }

                    links.add(new UserInterestSport(userRef, sport, input.level()));
                }
                userInterestSportRepository.saveAll(links);
            }
        }
        
        return userProfileRepository.save(profile);
    }

    // 특정 유저 프로필 조회
    @Transactional(readOnly = true)
    public ProfileResponse getProfile(Long userId) {
        var profileOpt = userProfileRepository.findByUserId(userId);
        var links = userInterestSportRepository.findByUser_Id(userId);

        var interests = links.stream()
            .map(link -> new InterestOutput (
                link.getSport().getName(),
                link.getSkillLevel()
            ))
            .collect(Collectors.toList());
        
        var p = profileOpt.orElseThrow(() -> new IllegalArgumentException("프로필 없음"));

        String profileImageUrl = null;
        if (p.getProfileImageKey() != null) {
            profileImageUrl = userProfileImageService.getProfileImageUrl(p.getProfileImageKey());
        }

        return new ProfileResponse(
            p.getNickname(),
            profileImageUrl,
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

    @Transactional
    public SimpleProfileResponse getSimpleProfile(Long userId) {
        var profileOpt = userProfileRepository.findByUserId(userId);
        var p = profileOpt.orElseThrow(() -> new IllegalArgumentException("프로필 없음"));

        String profileImageUrl = null;
        if (p.getProfileImageKey() != null) {
            profileImageUrl = userProfileImageService.getProfileImageUrl(p.getProfileImageKey());
        }

        return new SimpleProfileResponse(
            p.getUserId(),
            p.getNickname(),
            profileImageUrl,
            p.isGender()
        );
    }

    @Transactional(readOnly = true)
    public Map<Long, SimpleProfileResponse> getSimpleProfiles(java.util.List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) return Map.of();

        System.out.println("[TRACE] getSimpleProfiles called with userIds=" + userIds);
        var profiles = userProfileRepository.findByUserIdIn(userIds);
        System.out.println("[TRACE] userProfileRepository.findByUserIdIn returned size=" + (profiles == null ? "null" : profiles.size()));

        if (profiles != null) {
            for (var p : profiles) {
                if (p == null) {
                    System.out.println("[TRACE] profile entry is null");
                    continue;
                }
                Object uidObj = null;
                try { uidObj = p.getUserId(); } catch (Exception e) { uidObj = "error:"+e.getClass().getSimpleName(); }
                String keyClass = uidObj == null ? "null" : uidObj.getClass().getName();
                System.out.println("[TRACE] profile: userId=" + uidObj + " keyClass=" + keyClass
                        + " nickname=" + p.getNickname()
                        + " profileKey=" + p.getProfileImageKey()
                        + " safeGender=" + safeGetGender(p));
            }
        }

        return profiles.stream().filter(Objects::nonNull)
            .collect(Collectors.toMap(
                UserProfile::getUserId,
                p -> {
                    String profileImageUrl = null;
                    try {
                        if (p.getProfileImageKey() != null) {
                            profileImageUrl = userProfileImageService.getProfileImageUrl(p.getProfileImageKey());
                        }
                    } catch (Exception e) {
                        System.out.println("[WARN] getProfileImageUrl failed for userId=" + p.getUserId() + " ex=" + e.getClass().getSimpleName());
                    }

                    return new SimpleProfileResponse(
                        p.getUserId(),
                        p.getNickname(),
                        profileImageUrl,
                        safeGetGender(p)
                    );
                },
                // duplicate key 방지: 첫번째 값 유지
                (existing, replacement) -> existing
            ));
    }
    private Boolean safeGetGender(UserProfile p) {
        if (p == null) return null;
        try { return (Boolean) p.getClass().getMethod("getGender").invoke(p); } catch (Exception ignored) {}
        try { return (Boolean) p.getClass().getMethod("isGender").invoke(p); } catch (Exception ignored) {}
        return null;
    }

    // 프로필 이미지 업로드: S3에 업로드 후 user_profile.profile_image_key 갱신
    @Transactional
    public String uploadProfileImage(MultipartFile file, Long userId) {
        // 업로드 (서비스는 S3에 저장하고 key 반환)
        String key = userProfileImageService.uploadProfileImage(file, userId);

        UserProfile profile = userProfileRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("프로필 없음"));

        // 이전 이미지가 있으면 삭제(선택)
        String previousKey = profile.getProfileImageKey();
        if (previousKey != null && !previousKey.equals(key)) {
            try {
                userProfileImageService.deleteProfileImage(previousKey);
            } catch (Exception ignored) { }
        }

        // 프로필에 새 키 저장
        profile.setProfileImageKey(key);
        userProfileRepository.save(profile);

        // 반환은 public URL
        return userProfileImageService.getProfileImageUrl(key);
    }
}
