# 동시성 통합 테스트 결과

- 실행: `./gradlew concurrencyTest` (Docker 필요, Testcontainers MySQL 8.0)
- 조건: 20스레드 동시 실행 × 30회 반복, 격리수준 REPEATABLE-READ
- 실행 일시: 2026-10-15

## 결과 요약

| 실험 | 수정 전 | 현재 코드 | 요약 | 원본 로그 발췌 |
|---|---|---|---|---|
| 1. 정원 10명 모집글에 20건 동시 수락 → 정원 초과 | 30/30 | 0/30 | [md](01-seat-capacity.md) · [png](01-seat-capacity.png) | [png](log-01-seat-capacity.png) |
| 2. 같은 신청서 동시 수락 → accepted_count 불일치 | 30/30 (1차 수정) | 0/30 | [md](02-same-application.md) · [png](02-same-application.png) | [png](log-02-same-application.png) |
| 3. 같은 사용자 동시 중복 지원 → 신청서 중복 | 30/30 | 0/30 | [md](03-duplicate-apply.md) · [png](03-duplicate-apply.png) | [png](log-03-duplicate-apply.png) |
| 4. 같은 소셜 계정 동시 첫 로그인 → 계정 중복 | 30/30 | 0/30 | [md](04-social-first-login.md) · [png](04-social-first-login.png) | [png](log-04-social-first-login.png) |

테스트 통과 요약 (Gradle 생성): [00-test-summary.png](00-test-summary.png) — 4 tests, 0 failures

## 원본 자료 (`raw/`)

요약 표의 수치는 아래 원본에서 집계한 값입니다. 모두 테스트 실행 중 그대로 출력·생성된 파일이며 수정하지 않았습니다.

| 파일 | 내용 |
|---|---|
| [raw/console.log](raw/console.log) | `./gradlew concurrencyTest --console=plain` 콘솔 출력 전체 |
| [raw/*.csv](raw/) | trial 1회당 한 줄씩 측정값 (성공/실패 수, 소요 시간, 수락 인원, 위반 여부) |
| [raw/junit/*.xml](raw/junit/) | Gradle이 생성한 JUnit 결과 (테스트별 통과 여부, 실행 시간, 표준 출력 포함) |

- 콘솔 로그의 `[RAW]` 줄: trial별 측정값 (CSV와 같은 내용)
- 콘솔 로그의 `[DB]` 블록: 각 전략의 trial 1 직후 테스트가 SQL로 DB를 직접 조회한 결과
  - 예) 수정 전 코드: `max_member = 10`, `accepted_rows = 20` (정원 10명에 20명 수락)
- `log-*.png`는 console.log에서 해당 실험 줄을 뽑아 원문 그대로 보여주는 캡처이며, 왼쪽 숫자가 원본 파일의 줄 번호입니다.
