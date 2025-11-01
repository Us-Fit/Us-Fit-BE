package app.usfit.api.facility.repository;

import app.usfit.api.facility.entity.Facility;
import app.usfit.api.facility.entity.FacilitySport;
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

    @Query("""
    SELECT DISTINCT f
    FROM FacilitySport fs
    JOIN fs.sport s
    JOIN fs.facility f
    JOIN FacilityAddress addr ON addr.facility = f
    WHERE s.name = :sportName
      AND addr.sigunguNm = :sigunguNm
    """)
    List<Facility> findFacilitiesBySportNameAndSigungu(
            @Param("sportName") String sportName,
            @Param("sigunguNm") String sigunguNm
    );
}
