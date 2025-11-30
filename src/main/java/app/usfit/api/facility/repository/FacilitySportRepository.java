package app.usfit.api.facility.repository;

import app.usfit.api.facility.dto.FacilityDetailDto;
import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.entity.FacilitySport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FacilitySportRepository extends JpaRepository<FacilitySport, Long> {
    //sport name으로 facility 찾기
    @Query("""
            SELECT DISTINCT fs.facility
            FROM FacilitySport fs
            JOIN fs.sport s
            WHERE s.name = :name
            """
    )
    List<Facility> findFacilitiesBySportName(@Param("name") String name);

    //sport code로 facility 찾기
    @Query("""
    SELECT DISTINCT fs.facility
    FROM FacilitySport fs
    JOIN fs.sport s
    WHERE s.code = :code
    """)
    List<Facility> findFacilitiesBySportCode(@Param("code") String code);

    //sport name과 facility sidoNm으로 facility 찾기
    @Query("""
    SELECT DISTINCT f
    FROM FacilitySport fs
    JOIN fs.sport s
    JOIN fs.facility f
    JOIN FacilityAddress addr ON addr.facility = f
    WHERE s.name = :sportName
      AND addr.sidoNm = :sidoNm
""")
    List<Facility> findFacilitiesBySportNameAndSido(
            @Param("sportName") String sportName,
            @Param("sidoNm") String sidoNm
    );

    //sport name과 facility sigunguNm으로 facility 찾기
    @Query("""
    SELECT DISTINCT new app.usfit.api.facility.dto.FacilityDetailDto(
        f.id,
        f.name,
        f.typeName,
        addr.sidoNm,
        addr.sigunguNm,
        addr.roadAddr1,
        addr.roadAddr2,
        addr.lat,
        addr.lng,
        contact.managerPhone,
        f.areaSqm,
        f.indoorOutdoor
    )
    FROM FacilitySport fs
    JOIN fs.sport s
    JOIN fs.facility f
    JOIN f.addresses addr
    LEFT JOIN f.contacts contact
    WHERE s.name = :sportName
      AND addr.sigunguNm = :sigunguNm AND addr.sidoNm = :sidoNm
    """)
    List<FacilityDetailDto> findFacilitiesBySportNameAndSigungu(
            @Param("sportName") String sportName,
            @Param("sidoNm") String sidoNm,
            @Param("sigunguNm") String sigunguNm
    );

    //sport name과 facility sigunguNm으로 facility 찾기 pagination 기능 추가
    @Query(value = """
    SELECT DISTINCT new app.usfit.api.facility.dto.FacilityDetailDto(
        f.id,
        f.name,
        f.typeName,
        addr.sidoNm,
        addr.sigunguNm,
        addr.roadAddr1,
        addr.roadAddr2,
        addr.lat,
        addr.lng,
        contact.managerPhone,
        f.areaSqm,
        f.indoorOutdoor
    )
    FROM FacilitySport fs
    JOIN fs.sport s
    JOIN fs.facility f
    JOIN f.addresses addr
    LEFT JOIN f.contacts contact
    WHERE s.name = :sportName
      AND addr.sigunguNm = :sigunguNm AND addr.sidoNm = :sidoNm
    """,
            countQuery = """
    SELECT COUNT(DISTINCT f.id)
    FROM FacilitySport fs
    JOIN fs.sport s
    JOIN fs.facility f
    JOIN f.addresses addr
    WHERE s.name = :sportName
      AND addr.sigunguNm = :sigunguNm AND addr.sidoNm = :sidoNm
    """
    )
    Page<FacilityDetailDto> findFacilitiesBySportNameAndSigunguPage(
            @Param("sportName") String sportName,
            @Param("sidoNm") String sidoNm,
            @Param("sigunguNm") String sigunguNm,
            Pageable pageable
    );

}
