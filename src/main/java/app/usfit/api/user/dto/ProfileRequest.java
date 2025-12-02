package app.usfit.api.user.dto;

import java.time.LocalDate;
import java.util.List;

public record ProfileRequest (
    // 프로필 정보
    String nickname,
    Boolean gender, // true: 남성, false: 여성
    Double height,
    Double weight,
    LocalDate birthDate,
    String preferredArea,
    Double lat,
    Double lng,

    //관심 운동
    List<InterestInput> interests,

    // 전화번호
    String phoneNumber
) {}
