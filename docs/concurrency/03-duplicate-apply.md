# 실험 3. 중복 지원

- 실행 시각: 2026-10-15 14:09:59
- 환경: MySQL 8.0.36 (Testcontainers), 격리수준 REPEATABLE-READ, 스레드 20, 반복 30회, 커넥션 풀 40
- 시나리오: 같은 사용자가 같은 모집글에 20개 스레드로 동시에 지원, 30회 반복
- 기대값: 신청서 1건

## 결과

| 방식 | 중복 생성 trial | 평균 신청서 수 | trial 평균(ms) |
|---|---|---|---|
| exists 검사 후 INSERT (UNIQUE 없음, 수정 전) | **30/30** | 20.0 | 10.0 |
| exists 검사 + UNIQUE(post_id, user_id) (현재 코드) | **0/30** | 1.0 | 25.2 |

<details><summary>요청 실패 사유 (전체 trial 합계)</summary>

| 전략 | 사유 | 건수 |
|---|---|---|
| exists 검사 후 INSERT (UNIQUE 없음, 수정 전) | - | 0 |
| exists 검사 + UNIQUE(post_id, user_id) (현재 코드) | 400 이미 지원한 모집글입니다. | 570 |

</details>
