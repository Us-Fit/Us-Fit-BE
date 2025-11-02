package app.usfit.api.facility.controller;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.repository.FacilityRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/facility")
public class FacilityController {

    private FacilityRepository facilityRepository;

    public FacilityController(FacilityRepository facilityRepository){
        this.facilityRepository = facilityRepository;
    }

    @GetMapping("/{id}")
    public FacilityDetailDto getFacilityDetail(@PathVariable Long id){
        return facilityRepository.getFacilityInfo(id);
    }
}
