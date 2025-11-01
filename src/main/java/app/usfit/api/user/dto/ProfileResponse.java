package app.usfit.api.user.dto;

import java.time.LocalDate;
import java.util.List;

public record ProfileResponse (
    String nickname,
    Boolean gender,
    Double height,
    Double weight,
    LocalDate birthDate,
    String preferredArea,
    Double lat,
    Double lng,
    List<InterestOutput> interests
) {

}
