package app.usfit.api.facility;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.repository.FacilityRepository;
import app.usfit.api.facility.repository.FacilitySportRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
public class FindFacility {
    @Autowired
    FacilityRepository facilityRepo;

    @Autowired
    FacilitySportRepository facilitySportRepo;

    @Test
    @DisplayName("경기도의 모든 시설을 찾는다 facility id를 반환")
    void findIdsBySido(){
        List<Facility> facilities = facilityRepo.findFacilityBySidoCd("4100000000");
        for (Facility facility : facilities) {
            System.out.println("시설 이름: " + facility.getName());
        }
        assertThat(facilities).isNotNull();
    }

    @Test
    @DisplayName("facility type code 로 어떤 종목의 시설인지 찾고 facility id를 반환")
    void findIdsByTypeCode(){
        List<Facility> facilities = facilityRepo.findFacilityByTypeCd("N1701"); //농구 시설 반환
        for (Facility facility : facilities) {
            System.out.println("시설 이름: " + facility.getName());
        }
        assertThat(facilities).isNotNull();
    }

    @Test
    @DisplayName("sport name으로 facility들 찾기")
    void findFacilityBySportName() {
        List<Facility> facilities = facilitySportRepo.findFacilitiesBySportName("농구");

        for (Facility facility : facilities) {
            System.out.println("시설 이름: " + facility.getName());
        }

        assertThat(facilities).isNotEmpty();
    }

    @Test
    @DisplayName("sport name과 sido로 (도) facility들 찾기")
    void findFacilityBySportNameAndSido(){
        List<Facility> facilities = facilitySportRepo.findFacilitiesBySportNameAndSido("체력단련장", "경기도");
        System.out.println("체력단련장 + 경기도");
        for (Facility facility : facilities) {
            System.out.println("시설 이름: " + facility.getName());
        }

        assertThat(facilities).isNotEmpty();
    }

    @Test
    @DisplayName("sport name과 sigunguCd로 (시) facility 찾기")
    void findFacilityBySportNameAndSigungu(){
        List<FacilityDetailDto> dto = facilitySportRepo.findFacilitiesBySportNameAndSigungu("간이운동장", "김천시");

        for (FacilityDetailDto d : dto) {
            System.out.println(d);
        }

        assertThat(dto).isNotEmpty();
    }

    @Test
    @DisplayName("facility id로 facility detail 정보 넘기기")
    void getFacilityDetailById() {
        Long id = 1L;

        FacilityDetailDto dto = facilityRepo.getFacilityInfo(id);

        System.out.println(dto);

        assertThat(dto).isNotNull();
    }
}
