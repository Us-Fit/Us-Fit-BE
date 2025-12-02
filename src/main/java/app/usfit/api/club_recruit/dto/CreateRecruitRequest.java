package app.usfit.api.club_recruit.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateRecruitRequest {
    private String title;
    private String description;

    public CreateRecruitRequest() {}
}
