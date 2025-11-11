package app.usfit.api.facility.repository;

import app.usfit.api.facility.dto.FacilityDetailDto;
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

    //facility id로 facility 상세 페이지에 필요한 내용들 출력
    @Query("""
        select new app.usfit.api.facility.dto.FacilityDetailDto(
            f.id,
            f.name,
            f.typeName,
            a.sidoNm,
            a.sigunguNm,
            a.roadAddr1,
            a.lat,
            a.lng,
            b.managerPhone,
            f.areaSqm,
            f.indoorOutdoor
        )
        from Facility f
        left join FacilityAddress a
            on a.facility = f
        left join FacilityContact b
            on b.facility = f
        where f.id = :id
    """)
    FacilityDetailDto getFacilityInfo(@Param("id") Long id);

    //facility id List를 넘겨주면 facility entity list 반환
    @Query("""
           select f
           from Facility f
           where f.id in :ids
           order by f.id asc
           """
    )
    List<Facility> findFacilityById(@Param("ids") Collection<Long> ids);

    //도로 facility 찾기
    @Query("""
           select distinct a.facility
           from FacilityAddress a
           where a.sidoCd = :sidoCd
           """
    )
    List<Facility> findFacilityBySidoCd(@Param("sidoCd") String sidoCd);

    //시로 facility 찾기
    @Query("""
           select distinct a.facility
           from FacilityAddress a
           where a.sigunguCd = :sigunguCd
           """
    )
    List<Facility> findFacilityBySigunguCd(@Param("sigunguCd") String sigunguCd);

    //facility type cd 로 facility 찾기
    @Query("""
           select f
           from Facility f
           where f.typeCode = :typeCode
           """
    )
    List<Facility> findFacilityByTypeCd(@Param("typeCode") String typeCode);
}
