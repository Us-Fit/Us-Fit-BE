package app.usfit.api.club.recruit.controller;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import app.usfit.api.club.recruit.dto.ClubRecruitResponse;
import app.usfit.api.club.recruit.dto.CreateRecruitRequest;
import app.usfit.api.club.recruit.service.ClubRecriutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/clubs/recruit")
@RequiredArgsConstructor
public class ClubRecruitController {
    private final ClubRecriutService clubRecriutService;

    @Operation(summary = "모집글 생성/수정",
               description = "운영진(또는 소유자)이 해당 동호회에 모집글을 생성하거나 기존 모집글을 업데이트합니다.")
    @RequestBody(content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
    @PostMapping(value = "/{clubId}/recruit", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ClubRecruitResponse> createRecruit(
            @PathVariable("clubId") Long clubId,
            @RequestPart("request") CreateRecruitRequest req,
            @RequestPart(value = "images", required = false) MultipartFile[] images,
            Authentication authentication
    ) {
        // 인증에서 userId 추출 (authentication.getName()이 userId 문자열인 프로젝트 가정)
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }

        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).build();
        }

        try {
            ClubRecruitResponse res = clubRecriutService.createRecruit(clubId, req, images, userId);
            return ResponseEntity.status(201).body(res);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        } catch (SecurityException ex) {
            return ResponseEntity.status(403).build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.status(409).build();
        } catch (Exception ex) {
            System.out.println("Error creating recruit: " + ex.getMessage());
            return ResponseEntity.status(500).build();
        }
    }

    @DeleteMapping("/{clubId}/recruit")
    public ResponseEntity<Void> deleteRecruit(
            @PathVariable("clubId") Long clubId,
            Authentication authentication
    ) {
        if (authentication == null || authentication.getName() == null) {
            return ResponseEntity.status(401).build();
        }
        Long userId;
        try {
            userId = Long.parseLong(authentication.getName());
        } catch (NumberFormatException e) {
            return ResponseEntity.status(401).build();
        }

        try {
            clubRecriutService.deleteRecruit(clubId, userId);
            return ResponseEntity.noContent().build();
        }
        catch (Exception ex) {
            return ResponseEntity.status(500).build();
        }
    }
}