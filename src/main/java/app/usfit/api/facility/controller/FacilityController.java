package app.usfit.api.facility.controller;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.facility.repository.FacilitySportRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facility")
public class FacilityController {

    private FacilityRepository facilityRepository;
    private FacilitySportRepository facilitySportRepository;

    public FacilityController(FacilityRepository facilityRepository, FacilitySportRepository facilitySportRepository){
        this.facilityRepository = facilityRepository;
        this.facilitySportRepository = facilitySportRepository;
    }

    @GetMapping("/{id}")
    public FacilityDetailDto getFacilityDetail(@PathVariable Long id){
        return facilityRepository.getFacilityInfo(id);
    }

    @GetMapping("/search")
    public List<FacilityDetailDto> getFacilitiesBySportAndSigungu(
            @RequestParam String sportName,
            @RequestParam String sigunguNm
    ) {
        return facilitySportRepository.findFacilitiesBySportNameAndSigungu(sportName, sigunguNm);
    }
}
