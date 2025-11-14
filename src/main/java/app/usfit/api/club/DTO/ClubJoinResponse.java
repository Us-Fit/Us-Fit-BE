package app.usfit.api.club.DTO;

import java.time.LocalDateTime;

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
    private Long id;
    private Long clubId;
    private Long userId;
    private String status;
    private String message;
    private LocalDateTime requestedAt;
}