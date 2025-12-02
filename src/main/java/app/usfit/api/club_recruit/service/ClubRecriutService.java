package app.usfit.api.club_recruit.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.Utils.ImageConvertUtils;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.service.ClubUtilsService;
import app.usfit.api.club_recruit.dto.ClubRecruitResponse;
import app.usfit.api.club_recruit.dto.CreateRecruitRequest;
import app.usfit.api.club_recruit.entity.ClubRecruit;
import app.usfit.api.club_recruit.entity.ClubRecruitImage;
import app.usfit.api.club_recruit.repository.ClubRecruitRepository;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.ProfileService;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class ClubRecriutService {
    private final EntityManager entityManager;
    private final ClubRecruitRepository clubRecruitRepository;
    private final ClubUtilsService clubUtilsService;
    private final ProfileService profileService;

    private final ImageConvertUtils imageConvertUtils;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;
    // 모집글 생성
    @Transactional
    public ClubRecruitResponse createRecruit (Long clubId, CreateRecruitRequest req, MultipartFile[] images, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Club club = entityManager.find(Club.class, clubId);
        if (club == null) {
            throw new IllegalArgumentException("Club not found with ID: " + clubId);
        }

        // 소유자 혹은 운영진만 모집글 생성 가능.
        if (!clubUtilsService.isUserClubAdmin(clubId, userId)) {
            throw new IllegalStateException("동호회 운영진만 모집글을 생성할 수 있습니다.");
        }
        
        // 이미 모집글이 있으면 덮어쓰기 정책에 따라 예외 혹은 업데이트 선택.
        Optional<ClubRecruit> before = clubRecruitRepository.findByClubId(clubId);
        ClubRecruit recruit;
        if (before.isPresent()) {
            recruit = before.get();
            if (req.getTitle() != null) recruit.setTitle(req.getTitle());
            if (req.getDescription() != null) recruit.setDescription(req.getDescription());

            // 기존 이미지 제거 (orphanRemoval에 의해 DB에서 삭제됨)
            if (recruit.getImages() != null && !recruit.getImages().isEmpty()) {
                recruit.getImages().clear();
                entityManager.flush(); // 즉시 제거 보장
            }
        }
        else {
            User creator = entityManager.find(User.class, userId);
            if (creator == null) {
                throw new IllegalArgumentException("User not found with ID: " + userId);
            }

            recruit = ClubRecruit.builder()
                .club(club)
                .creator(creator)
                .title(req.getTitle())
                .description(req.getDescription())
                .build();
            entityManager.persist(recruit);
        }

        // 이미지 업로드 및 엔티티 추가
        if (images != null) {
            int idx = 0;
            List<ClubRecruitImage> added = new ArrayList<>();
            for (MultipartFile f : images) {
                if (f == null || f.isEmpty()) continue;
                String key = uploadClubRecruitImage(clubId, f); // 실제 메서드명에 맞게 조정하세요
                if (key == null) throw new IllegalStateException("이미지 업로드 결과가 null입니다.");
                ClubRecruitImage img = ClubRecruitImage.builder()
                        .recruit(recruit)
                        .imageKey(key)
                        .description(null)
                        .sortOrder(idx++)
                        .build();
                entityManager.persist(img);
                added.add(img);
            }
            if (!added.isEmpty()) {
                recruit.getImages().addAll(added);
            }
        }

        // club 상태를 recruiting으로 변경
        club.setStatus("recruiting");
        entityManager.merge(club);

        entityManager.flush();

        SimpleProfileResponse creatorProfile = null;
        if (recruit.getCreator() != null && recruit.getCreator().getId() != null) {
            creatorProfile = profileService.getSimpleProfile(recruit.getCreator().getId());
        }

        List<String> imageUrls = (recruit.getImages() == null ? List.<ClubRecruitImage>of() : recruit.getImages()).stream()
            .map(ClubRecruitImage::getImageKey)
            .filter(Objects::nonNull)
            .map(imageConvertUtils::keyToUrl)
            .collect(Collectors.toList());

        return new ClubRecruitResponse(recruit.getId(), recruit.getTitle(), recruit.getDescription(), imageUrls, creatorProfile);
    }

    // 사진 업로드 헬퍼 메서드
    private String uploadClubRecruitImage(Long clubId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        // 프로필 이미지는 따로 path 분리
        String key = "club/recruit/" + clubId + "/" + UUID.randomUUID() + ext;

        PutObjectRequest putReq = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (InputStream is = file.getInputStream()) {
            s3Client.putObject(putReq, RequestBody.fromInputStream(is, file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }

        // 프로필 이미지는 club_image 테이블에 안 넣고, club에만 저장해도 됨
        return key;
    }

    /**
     * 모집글 삭제: 운영진만 가능.
     */
    @Transactional
    public void deleteRecruit(Long clubId, Long userId) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }

        Club club = entityManager.find(Club.class, clubId);
        if (club == null) {
            throw new IllegalArgumentException("Club not found with ID: " + clubId);
        }

        // 권한 검사 (운영진/소유자)
        if (!clubUtilsService.isUserClubAdmin(clubId, userId)) {
            throw new SecurityException("동호회 운영진만 모집글을 삭제할 수 있습니다.");
        }

        Optional<ClubRecruit> maybe = clubRecruitRepository.findByClubId(clubId);
        if (maybe.isEmpty()) {
            throw new IllegalStateException("존재하는 모집글이 없습니다.");
        }

        ClubRecruit recruit = maybe.get();

        // 삭제 (cascade/orphanRemoval에 의해 이미지도 삭제됨)
        clubRecruitRepository.delete(recruit);

        // 동호회 상태 복구 (원하는 값으로 변경 가능)
        club.setStatus("active");
        entityManager.merge(club);

        entityManager.flush();
    }
}
