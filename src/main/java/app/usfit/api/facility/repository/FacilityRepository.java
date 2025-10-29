package app.usfit.api.facility.repository;

import app.usfit.api.facility.entity.Facility;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    @Query("""
        select f
        from Facility f
        where f.name = :name
        and exists (
            select 1 from FacilityAddress a
            where a.facility = f and a.roadAddr1 = :roadAddr1
        )
        order by f.id asc
    """)
    List<Facility> findAllByNameAndRoadAddr1(@Param("name") String name,
                                             @Param("roadAddr1") String roadAddr1);

    //facility id List를 넘겨주면 facility entity list 반환
    @Query("""
           select f
           from Facility f
           where f.id in :ids
           order by f.id asc
           """
    )
    List<Facility> findFacilityById(@Param("ids") Collection<Long> ids);

    //시, 도 로 facility id 찾기
    @Query("""
           select distinct a.facility.id
           from FacilityAddress a
           where a.sidoCd = :sidoCd
           """
    )
    List<Long> findFacilityIdsBySidoCd(@Param("sidoCd") String sidoCd);

    //facility type cd 로 facility id 찾기
    @Query("""
           select f.id
           from Facility f
           where f.typeCode = :typeCode
           """
    )
    List<Long> findFacilityIdsByTypeCd(@Param("typeCode") String typeCode);
}
