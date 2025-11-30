package app.usfit.api.RecruitPlayer.dto;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RecruitRecommendRequest {
    List<String> sports;
    String sidoNm;
    String sigunguNm;
    Integer limit; // optional
    public RecruitRecommendRequest() {}
}
