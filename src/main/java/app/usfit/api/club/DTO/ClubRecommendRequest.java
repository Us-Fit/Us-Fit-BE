package app.usfit.api.club.DTO;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ClubRecommendRequest {
    private List<String> sports;
    private String sidoNm;
    private String sigunguNm;
    private Integer limit; // optional

    public ClubRecommendRequest() {}
}
