package app.usfit.api.sport.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "sports",
        indexes = {
                @Index(name = "idx_sport_code", columnList = "code"),
                @Index(name = "idx_sport_name", columnList = "name")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_sport_code", columnNames = {"code"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Sport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;                    // PK

    @Column(nullable = false, length = 100)
    private String code;                // 종목 코드 (유니크) -> 시설 부분에 업종코드와 연결시키고 싶음 생각 필요

    @Column(nullable = false, length = 150)
    private String name;                // 종목 명
}