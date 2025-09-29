package app.usfit.api.facility.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * FacilityContact (담당자 연락처) 엔티티
 * - 시설 담당자 이름/전화번호를 보관
 */
@Entity
@Table(name = "facility_contact")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FacilityContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 담당자 레코드 PK

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facility_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_facility_contact_facility"))
    private Facility facility; // 시설 FK

    @Column(length = 200)
    private String managerName;  // 담당자명 (RSPNSBLTY_NM)

    @Column(length = 100)
    private String managerPhone; // 담당자 전화번호 (RSPNSBLTY_TEL_NO)
}
