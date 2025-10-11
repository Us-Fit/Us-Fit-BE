package app.usfit.api.facility.repository;

import app.usfit.api.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    @Query("""
      select f from Facility f
      join f.addresses a
      where f.name = :name
        and a.roadAddr1 = :roadAddr1
    """)
    Optional<Facility> findByNameAndRoadAddr1(@Param("name") String name,
                                              @Param("roadAddr1") String roadAddr1);

}
