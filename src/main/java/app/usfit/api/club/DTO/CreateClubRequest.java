package app.usfit.api.club.DTO;

import java.util.List;

import lombok.Data;

@Data
public class CreateClubRequest {
    private String name;
    private String description;
    private String regionCode;
    private String regionName;
    private Integer memberLimit;
    private Boolean visibility;
    private String status;
    private String phoneNumber;
    private String snsLink;

    private Long mainFacilityId;
    // 선택 운동들
    private List<ClubSportRequest> sports;
}
