package app.usfit.api.facility.repository;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.dto.FacilityDetailView;
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
            a.roadAddr2,
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



    @Query(value = """
    SELECT
        f.id                               AS id,
        f.name                             AS name,
        f.type_name                        AS typeName,
        addr.sido_nm                       AS sidoNm,
        addr.sigungu_nm                    AS sigunguNm,
        addr.road_addr1                    AS roadAddr1,
        addr.road_addr2                    AS roadAddr2,
        addr.lat                           AS lat,
        addr.lng                           AS lng,
        c.manager_phone                    AS managerPhone,
        f.area_sqm                         AS areaSqm,
        f.indoor_outdoor                   AS indoorOutdoor,
        ST_Distance_Sphere(
            ST_SRID(POINT(:lng, :lat), 4326),
            addr.loc
        ) AS distance
    FROM facility_address addr
    JOIN facility f           ON f.id = addr.facility_id
    LEFT JOIN facility_contact c ON c.facility_id = f.id
    JOIN facility_sport fs     ON fs.facility_id = f.id
    JOIN sports s              ON s.id = fs.sport_id
    WHERE
        (:sportIdsIsEmpty = TRUE OR s.id IN (:sportIds))
        AND ST_Distance_Sphere(
              ST_SRID(POINT(:lng, :lat), 4326),
              addr.loc
            ) <= :radiusKm * 1000
    GROUP BY
        f.id, f.name, f.type_name, addr.sido_nm, addr.sigungu_nm,
        addr.road_addr1, addr.road_addr2, addr.lat, addr.lng, c.manager_phone, f.area_sqm, f.indoor_outdoor
    ORDER BY
        distance ASC
    LIMIT :limit OFFSET :offset
""", nativeQuery = true)
    List<FacilityDetailView> findNearbyFacilitiesNative(
            @Param("lat") double lat,
            @Param("lng") double lng,
            @Param("radiusKm") double radiusKm,
            @Param("sportIds") List<Long> sportIds,
            @Param("sportIdsIsEmpty") boolean sportIdsIsEmpty,
            @Param("limit") int limit,
            @Param("offset") int offset
    );

}
