package app.usfit.api.user.dto;

import java.time.LocalDate;
import java.util.List;

// 특정 사용자 프로필 정보
public record ProfileResponse (
    String nickname,
    String profileImageUrl, // 프로필 이미지의 public URL
    Boolean gender,
    Double height,
    Double weight,
    LocalDate birthDate,
    String preferredArea,
    Double lat,
    Double lng,
    List<InterestOutput> interests
) {}
