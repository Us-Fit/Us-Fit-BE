package app.usfit.api.facility;

import app.usfit.api.facility.repository.FacilityRepository;
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
    FacilityRepository repo;

    @Test
    @DisplayName("경기도의 모든 시설을 찾는다 facility id를 반환")
    void findIdsBySido(){
        List<Long> ids = repo.findFacilityIdsBySidoCd("4100000000");
        System.out.println("facility ids = " + ids);
        assertThat(ids).isNotNull();
    }
}
