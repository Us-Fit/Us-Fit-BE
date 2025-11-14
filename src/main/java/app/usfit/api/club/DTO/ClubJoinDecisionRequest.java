package app.usfit.api.club.DTO;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ClubJoinDecisionRequest {
    private boolean accept; // true: 수락, false: 거절
}
