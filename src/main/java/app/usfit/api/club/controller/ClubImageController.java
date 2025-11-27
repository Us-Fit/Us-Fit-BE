package app.usfit.api.club.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.club.DTO.ClubImageResponse;
import app.usfit.api.club.entity.ClubImage;
import app.usfit.api.club.service.ClubImageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/club/{clubId}/images")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Club Image", description = "동호회 이미지 업로드 및 조회 API")
@RequiredArgsConstructor
public class ClubImageController {
    private final ClubImageService clubImageService;

    @PostMapping(consumes = {"multipart/form-data"})
    @Operation(
        summary = "클럽 이미지 업로드",
        description = "클럽에 여러 이미지를 업로드합니다. 권한은 서비스단에서 검증됩니다."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(
        description = "multipart/form-data 형식. files 필드에 이미지 파일들을 첨부하세요. title/description 은 선택입니다.",
        content = @Content(
            mediaType = "multipart/form-data",
            examples = {
                @ExampleObject(
                    name = "업로드 예시",
                    value = """
                        {
                        "files": [ {file1}, {file2}, ... ],
                        "title": "클럽 워크샵 사진",
                        "description": "2025년 봄 워크샵에서 찍은 사진들"
                        }
                        """
                        )}))
    public ResponseEntity<Object> uploadClubImages(
            @Parameter(in = ParameterIn.PATH, name = "clubId", required = true, description = "클럽 ID", example = "123")
            @PathVariable("clubId") Long clubId,
            @RequestPart("files") MultipartFile[] files,
            @RequestParam(value = "title", required = false) String title,
            @RequestParam(value = "description", required = false) String description,
            Authentication authentication
    ) {
        try {
            Long userId = Long.parseLong(authentication.getName());
            List<ClubImage> uploaded = clubImageService.uploadClubImages(files, clubId, userId , title, description);
            List<String> keys = uploaded.stream().map(ClubImage::getImageKey).collect(Collectors.toList());
            return ResponseEntity.ok(keys);
        } catch (SecurityException se) {
            return ResponseEntity.status(403).body(Map.of("error", se.getMessage()));
        } catch (IllegalArgumentException ie) {
            return ResponseEntity.badRequest().body(Map.of("error", ie.getMessage()));
        }
    }

    // 모든 클럽 멤버가 이미지 접근 가능한 목록 조회
    @Operation(summary = "클럽 이미지 목록 조회", description = "클럽 소속 멤버만 조회 가능. 최신순 반환")
    @GetMapping
    public ResponseEntity<Object> listClubImages(
            Authentication authentication,
            @Parameter(in = ParameterIn.PATH, name = "clubId", required = true, description = "클럽 ID", example = "123")
            @PathVariable("clubId") Long clubId
    ) {
        try {
            if (authentication == null || authentication.getName() == null) {
                return ResponseEntity.status(401).body(Map.of("error", "인증 필요"));
            }
            Long userId = Long.parseLong(authentication.getName());
            List<ClubImageResponse> images = clubImageService.listClubImages(clubId, userId);
            return ResponseEntity.ok(images);
        } catch (NumberFormatException nfe) {
            return ResponseEntity.status(400).body(Map.of("error", "사용자 ID 파싱 실패"));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", "서버 오류"));
        }
    }
}