package app.usfit.api.club.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.club.DTO.ClubImageResponse;
import app.usfit.api.club.entity.ClubImage;
import app.usfit.api.club.repository.ClubImageRepository;
import app.usfit.api.club.repository.ClubRepository;
import app.usfit.api.user.dto.SimpleProfileResponse;
import app.usfit.api.user.service.ProfileService;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;


@Service
@RequiredArgsConstructor
public class ClubImageService {    
    private final S3Client s3Client;
    private final ClubImageRepository clubImageRepository;
    private final ClubRepository clubRepository;
    private final ProfileService profileService;

    @Autowired
    private ClubinfoService clubinfoService;
    
    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 권한/소속 검사 포함된 상위 메서드 (컨트롤러에서 호출)
    @Transactional
    public List<ClubImage> checkUploadRole(MultipartFile[] files, Long clubId, Long userId, String title, String description) {
        // 동호회 정보
        var club = clubRepository.findById(clubId);
        if (club.isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 동호회입니다.");
        }

        // 동호회 멤버 목록에서 현재 사용자 조회
        var clubUserOpt = clubinfoService.listClubMemebers(clubId).stream()
                .filter(cm -> cm.userSimpleProfile().userId().equals(clubId))
                .findFirst();

        if (clubUserOpt.isEmpty()) {
            throw new SecurityException("동호회 소속 회원만 이미지 업로드가 가능합니다.");
        }

        var clubUser = clubUserOpt.get();
        
        // role 체크: owner 또는 admin 허용
        String role = clubUser.role();
        if (!( "owner".equalsIgnoreCase(role) || "admin".equalsIgnoreCase(role) )) {
            throw new SecurityException("동호회 관리자만 이미지 업로드가 가능합니다.");
        }

        return uploadClubImages(files, clubId, userId, title, description);
    }

    // 기존 실제 업로드 로직 (uploaderId 사용)
    public List<ClubImage> uploadClubImages(MultipartFile[] files, Long clubId, Long uploaderId, String title, String description) {
        List<ClubImage> saved = new ArrayList<>();
        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) continue;

            String original = file.getOriginalFilename();
            String ext = "";
            if (original != null && original.contains(".")) {
                ext = original.substring(original.lastIndexOf('.'));
            }

            String key = "club/" + clubId + "/" + UUID.randomUUID() + ext;

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

            ClubImage ci = ClubImage.builder()
                    .clubId(clubId)
                    .imageKey(key)
                    .uploaderId(uploaderId)
                    .title(title)
                    .description(description)
                    .build();

            saved.add(clubImageRepository.save(ci));
        }
        return saved;
    }

    // 클럽 이미지 목록 조회: 소속 멤버만 접근 가능, uploader 정보를 한 번에 조회하여 N+1 방지
    @Transactional(readOnly = true)
    public List<ClubImageResponse> listClubImages(Long clubId, Long currentUserId) {
        // 동호회 존재 확인
        if (clubRepository.findById(clubId).isEmpty()) {
            throw new IllegalArgumentException("존재하지 않는 동호회입니다.");
        }

        var members = clubinfoService.listClubMemebers(clubId);
        boolean isMember = members.stream().anyMatch(cm -> cm.userSimpleProfile().userId().equals(currentUserId));
        boolean isOwner = clubRepository.existsByIdAndOwnerId(clubId, currentUserId);

        if (!isMember && !isOwner) {
            throw new SecurityException("클럽 소속 멤버만 접근할 수 있습니다.");
        }

        // 최신순 조회 (repo 메서드명에 OrderByCreatedAtDesc 적용됨)
        List<ClubImage> images = clubImageRepository.findByClubIdOrderByCreatedAtDesc(clubId);
        if (images.isEmpty()) return List.of();

        Set<Long> uploaderIds = images.stream()
                .map(ClubImage::getUploaderId)
                .filter(id -> id != null)
                .collect(Collectors.toCollection(HashSet::new));

        Map<Long, SimpleProfileResponse> userMap = new HashMap<>();
        for (Long uid : uploaderIds) {
            try {
                SimpleProfileResponse simple = profileService.getSimpleProfile(uid);
                if (simple != null) userMap.put(uid, simple);
            } catch (Exception ignored) {
                // 조회 실패해도 처리 흐름을 멈추지 않음
            }
        }

        return images.stream().map(img -> {
            String url = null;
            try {
                if (img.getImageKey() != null) {
                    url = s3Client.utilities()
                            .getUrl(GetUrlRequest.builder().bucket(bucket).key(img.getImageKey()).build())
                            .toExternalForm();
                } else {
                    url = img.getImageKey();
                }
            } catch (Exception e) {
                url = img.getImageKey();
            }

            SimpleProfileResponse uploader = userMap.get(img.getUploaderId());
            var uploadTime = img.getCreatedAt();

            return new ClubImageResponse(
                    url,
                    uploader,
                    uploadTime,
                    img.getTitle(),
                    img.getDescription()
            );
        }).collect(Collectors.toList());
    }

    // 클럽 메인(프로필) 이미지 업로드용
    public String uploadClubMainImage(Long clubId, Long uploaderId, MultipartFile file) {
        if (file == null || file.isEmpty()) return null;

        String original = file.getOriginalFilename();
        String ext = "";
        if (original != null && original.contains(".")) {
            ext = original.substring(original.lastIndexOf('.'));
        }

        // 프로필 이미지는 따로 path 분리
        String key = "club/main/" + clubId + "/" + UUID.randomUUID() + ext;

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
}