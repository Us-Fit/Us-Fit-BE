package app.usfit.api.club.controller;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.club.DTO.ClubCreatedResponse;
import app.usfit.api.club.DTO.ClubSimpleInfoResponse;
import app.usfit.api.club.DTO.CreateClubRequest;
import app.usfit.api.club.service.ClubService;
import io.swagger.v3.oas.annotations.Operation;


@RestController
@RequestMapping("/api/clubs")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Club", description = "동호회 생성 및 목록 조회 API")
public class ClubController {
    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "동호회 생성", description = "새 동호회를 생성합니다. JSON + 이미지 파일을 함께 전송합니다.")
    public ResponseEntity<ClubCreatedResponse> createClub(
            @RequestPart("request") CreateClubRequest req,           // JSON
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage, // 이미지 파일
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(authentication.getName());
        ClubCreatedResponse c = clubService.createClub(req, mainImage, userId);
        return ResponseEntity.ok(c);
    }

    @GetMapping
        @Operation(summary = "동호회 목록 조회", description = "등록된 동호회 목록을 간단히 조회합니다. 각 항목은 동호회의 요약 정보만 포함합니다.")
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "동호회 요약 목록 반환", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json",
        examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시",
        value = """
                        [
                            {
                                "id": 10,
                                "name": "주말 축구 모임",
                                "regionName": "서울 강남구",
                                "memberLimit": 20,
                                "visibility": true,
                                "status": "recruiting",
                                "ownerId": 100,
                                "mainFacilityId": 5,
                                "memberCount": 8,
                                "sports": [ { "sportId": 1, "sportName": "축구", "levelMin": 1, "levelMax": 5, "note": "초중급 환영" } ]
                            },
                            {
                                "id": 11,
                                "name": "저녁 농구",
                                "regionName": "서울 송파구",
                                "memberLimit": 12,
                                "visibility": false,
                                "status": "active",
                                "ownerId": 101,
                                "mainFacilityId": null,
                                "memberCount": 5,
                                "sports": [ { "sportId": 2, "sportName": "농구", "levelMin": 2, "levelMax": 5, "note": "중급 이상" } ]
                            }
                        ]
                        """)}))
        public ResponseEntity<List<ClubSimpleInfoResponse>> listClubs() {
                return ResponseEntity.ok(clubService.listClubs());
        }

    // 클럽 업데이트
    @PatchMapping(value = "/{clubId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "동호회 수정", description = "동호회 정보를 수정합니다. request(JSON) + mainImage(file) 파트 전송")
    public ResponseEntity<ClubCreatedResponse> updateClub(
            @PathVariable("clubId") Long clubId,
            @RequestPart("request") CreateClubRequest req,
            @RequestPart(value = "mainImage", required = false) MultipartFile mainImage,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException ex) {
            return ResponseEntity.status(401).build();
        }

        try {
            ClubCreatedResponse c = clubService.updateClub(clubId, userId, req, mainImage);
            return ResponseEntity.ok(c);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException e) {
            return ResponseEntity.status(403).build();
        } catch (Exception e) {
            return ResponseEntity.status(500).build();
        }
    }
}
