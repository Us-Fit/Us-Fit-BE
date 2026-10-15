# 실험 4. 첫 소셜 로그인 중복 계정

- 실행 시각: 2026-10-15 14:10:00
- 환경: MySQL 8.0.36 (Testcontainers), 격리수준 REPEATABLE-READ, 스레드 20, 반복 30회
- 시나리오: 같은 (KAKAO, providerId)로 20개 요청이 동시에 첫 로그인
- 기대값: 계정 1개, 모든 요청이 같은 사용자로 로그인 성공

## 결과

| 방식 | 실패 trial (중복 계정 또는 로그인 에러) | 평균 계정 수 | trial 평균(ms) |
|---|---|---|---|
| 조회 후 저장 (UNIQUE 없음, 수정 전) | **30/30** | 20.0 | 9.2 |
| UNIQUE(provider, provider_id) + 위반 시 재조회 (현재 코드) | **0/30** | 1.0 | 31.3 |

<details><summary>요청 실패 사유 (전체 trial 합계)</summary>

| 전략 | 사유 | 건수 |
|---|---|---|
| 조회 후 저장 (UNIQUE 없음, 수정 전) | - | 0 |
| UNIQUE(provider, provider_id) + 위반 시 재조회 (현재 코드) | - | 0 |

</details>
