package app.usfit.api.club.DTO;

import java.time.LocalDate;
import java.util.List;

import app.usfit.api.facility.dto.FacilityDetailView;

public record ClubDetailInfoResponse(
    String name, // 동호회 이름
    String description, // 동호회 설명
    String mainImageUrl, //동호회 메인 이미지 URL
    int currMemberCount,
    int memberLimit,
    String phoneNumber,
    LocalDate createAt,
    String snsLink,

    // 메인 시설 정보
    FacilityDetailView mainFacility,

    // 동호회가 제공하는 스포츠 종목 리스트
    List<ClubSportResponse> sports
) {}
