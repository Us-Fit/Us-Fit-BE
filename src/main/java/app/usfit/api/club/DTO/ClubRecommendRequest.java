package app.usfit.api.club.DTO;

import java.util.List;

public class ClubRecommendRequest {
    private List<String> sports;
    private Long facilityId;
    private Integer limit; // optional

    public ClubRecommendRequest() {}

    public List<String> getSports() { return sports; }
    public void setSports(List<String> sports) { this.sports = sports; }

    public Long getFacilityId() { return facilityId; }
    public void setFacilityId(Long facilityId) { this.facilityId = facilityId; }

    public Integer getLimit() { return limit; }
    public void setLimit(Integer limit) { this.limit = limit; }
}
