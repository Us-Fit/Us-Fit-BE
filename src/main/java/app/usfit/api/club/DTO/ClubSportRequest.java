package app.usfit.api.club.DTO;

import lombok.Data;

@Data
public class ClubSportRequest {
    private String sportName;
    private Integer levelMin;
    private Integer levelMax;
    private String note;
}
