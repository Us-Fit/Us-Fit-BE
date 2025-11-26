package app.usfit.api.user.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.user.repository.UserProfileRepository;
import lombok.RequiredArgsConstructor;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
public class UserProfileImageService {
    private final S3Client s3Client;

    @Autowired
    private final UserProfileRepository userProfileRepository;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    // 업로드하고 S3 키(key)를 반환
    public String uploadProfileImage(MultipartFile file, Long userId) {
        String originalName = file.getOriginalFilename();
        String ext = "";

        if (originalName != null && originalName.contains(".")) {
            ext = originalName.substring(originalName.lastIndexOf('.'));
        }

        String key = "profile/" + userId + "/" + UUID.randomUUID() + ext;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        try (InputStream is = file.getInputStream()) {
            s3Client.putObject(putObjectRequest, RequestBody.fromInputStream(is, file.getSize()));
        } catch (IOException e) {
            throw new RuntimeException("S3 업로드 실패", e);
        }

        // 업로드 후 공개 URL 생성
        String url = getProfileImageUrl(key);

        // User의 profileImageUrl에 저장
        userProfileRepository.findById(userId).ifPresent(user -> {
            user.setProfileImageKey(url);
            userProfileRepository.save(user);
        });

        return key;
    }

    // 저장된 키로 public URL 생성 (버킷 설정에 따라 URL 형식 변경 가능)
    public String getProfileImageUrl(String key) {
        if (key == null) return null;
        return s3Client.utilities()
                    .getUrl(GetUrlRequest.builder().bucket(bucket).key(key).build())
                    .toExternalForm();
    }

    public void deleteProfileImage(String key) {
        if (key == null) return;
        DeleteObjectRequest request = DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();
        s3Client.deleteObject(request);
    }
}
