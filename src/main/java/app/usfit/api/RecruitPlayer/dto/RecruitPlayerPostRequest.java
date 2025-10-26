package app.usfit.api.RecruitPlayer.dto;

import lombok.Data;

@Data
public class RecruitPlayerPostRequest {
    private String title;
    private String description;
    private String sportType;
    private int maxMemberCount;
    private int currMemeberCount;    
    private String location;
    private String scheduledTime;
    
    // 글 작성자
    private Long writerId;
}
