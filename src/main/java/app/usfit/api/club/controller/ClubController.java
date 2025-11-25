package app.usfit.api.club.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import app.usfit.api.club.DTO.CreateClubRequest;
import app.usfit.api.club.entity.Club;
import app.usfit.api.club.service.ClubService;


@RestController
@RequestMapping("/api/clubs")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Club", description = "동호회 생성 및 목록 조회 API")
public class ClubController {
    private final ClubService clubService;

    public ClubController(ClubService clubService) {
        this.clubService = clubService;
    }

        @PostMapping
        @io.swagger.v3.oas.annotations.Operation(summary = "동호회 생성", description = "새 동호회를 생성합니다. 아래 예시를 참고하여 요청 바디를 구성하세요.")
        @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "동호회 생성 요청 예시", content = @io.swagger.v3.oas.annotations.media.Content(mediaType = "application/json", schema = @io.swagger.v3.oas.annotations.media.Schema(implementation = CreateClubRequest.class), examples = {@io.swagger.v3.oas.annotations.media.ExampleObject(name = "한글_예시", value = """
                        {
                            "name": "주말 축구 모임",
                            "description": "주말에 같이 축구할 분들 모집",
                            "regionCode": "SEOUL",
                            "regionName": "서울 강남구",
                            "memberLimit": 20,
                            "visibility": true,
                            "status": "recruiting",
                            "phoneNumber": "010-1234-5678",
                            "snsLink": "https://instagram.com/example",
                            "mainFacilityId": 5,
                            "sports": [ { "sportName": "축구", "levelMin": 1, "levelMax": 5, "note": "초중급 환영" } ]
                        }
                        """)})
        )
        public ResponseEntity<Club> createClub(@RequestBody CreateClubRequest req, Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId = Long.parseLong(authentication.getName());

        Club c = clubService.createClub(req, userId);
        return ResponseEntity.ok(c);
        //return ResponseEntity.created(URI.create("/api/clubs/" + c.getId())).body(c);
    }

    @GetMapping
    @io.swagger.v3.oas.annotations.Operation(summary = "동호회 목록 조회", description = "등록된 동호회 목록을 간단히 조회합니다.")
    public ResponseEntity<List<Club>> listClubs() {
        return ResponseEntity.ok(clubService.listClubs());
    }
}
