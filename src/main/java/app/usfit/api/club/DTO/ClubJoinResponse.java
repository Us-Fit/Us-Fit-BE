package app.usfit.api.club.DTO;

import java.time.LocalDateTime;

import app.usfit.api.user.dto.SimpleProfileResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ClubJoinResponse {
    private Long requestId;
    private Long clubId;
    private SimpleProfileResponse user;
    private String status;
    private String message;
    private LocalDateTime requestedAt;
}