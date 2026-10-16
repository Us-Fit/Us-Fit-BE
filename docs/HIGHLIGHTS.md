# 리뷰어를 위한 하이라이트 — 이윤형 담당 부분

> UsFit 백엔드는 2인이 함께 만들었습니다. 이 문서는 **제가 맡은 부분과, 그중 먼저 보면 좋은 코드**를 정리한 것입니다.

- 포트폴리오 (노션): [이윤형 포트폴리오](https://political-minnow-95d.notion.site/YunHyeong-Lee-16b076a1d5e98068b985da37e737538e) → UsFit 페이지에 항목별 상세 정리
- 동시성 테스트 리포트: [docs/concurrency](concurrency/README.md)

---

## 1. 담당 범위

| 담당 | 범위 |
| --- | --- |
| **본인** | 인증(로컬·카카오 로그인, JWT, 보안 설정), 프로필, 동호회(생성·가입 신청·승인·권한·이미지·모집글·추천), 용병 모집·신청과 동시성 제어 |
| 팀원 | 공공데이터 적재, 시설·강좌 검색, 리뷰, 동호회 활동, 인프라(AWS) |

`src/main/java` 기준 9,373줄 중 **5,496줄**을 작성했습니다 (`git blame`, dev 브랜치 기준, DTO 포함).

---

## 2. 추천하는 확인 순서

### ① 정원이 있는 수락 — 동시성 제어 방식을 측정해서 고르기

정원 10명 모집글에 수락 20건이 동시에 들어오는 상황을 Testcontainers MySQL에서 재현하고, 비관적 락·낙관적 락·조건부 UPDATE를 같은 조건(20건 동시 × 30회)으로 비교해 조건부 UPDATE를 적용했습니다. 정원 초과 30/30 → 0/30.

| 볼 곳 | 확인 포인트 |
| --- | --- |
| [`reserveSeat` · `releaseSeat`](../src/main/java/app/usfit/api/RecruitPlayer/repository/RecruitPlayerPostRepository.java#L20-L27) | 정원 검사와 증가를 한 문장으로 (0행이면 정원 마감) |
| [`transitionStatus`](../src/main/java/app/usfit/api/RecruitPlayer/repository/RecruitApplicationRepository.java#L18-L22) | 신청서는 기대한 상태일 때만 전이 (같은 신청서 동시 수락 차단) |
| [`changeStatus`](../src/main/java/app/usfit/api/RecruitPlayer/service/RecruitApplicationService.java#L125-L171) | 상태 전이 → 자리 확보 순서로 잠금 순서 고정, 정원이 차면 롤백으로 상태 전이도 취소 |
| [`acceptedCount`](../src/main/java/app/usfit/api/RecruitPlayer/entity/RecruitPlayerPost.java#L55-L57) | `updatable = false`로 엔티티 저장 시 오래된 값으로 덮어쓰지 않음 |
| [`RecruitConcurrencyTest`](../src/test/java/app/usfit/api/concurrency/RecruitConcurrencyTest.java) | 방식별 비교 실험. `./gradlew concurrencyTest`로 리포트 재생성 |

### ② 같은 요청이 여러 번 와도 한 번만

| 볼 곳 | 확인 포인트 |
| --- | --- |
| [`SocialUserService.findOrCreate`](../src/main/java/app/usfit/api/user/service/login/SocialUserService.java#L29-L40) | 첫 소셜 로그인이 동시에 와도 계정 하나. 제약 위반 시 먼저 생성된 계정을 다시 조회, 트랜잭션은 `NOT_SUPPORTED`로 분리 |
| [`User`](../src/main/java/app/usfit/api/user/entity/User.java#L24-L27) · [`RecruitApplication`](../src/main/java/app/usfit/api/RecruitPlayer/entity/RecruitApplication.java#L25-L28) | `UNIQUE(provider, provider_id)`, `UNIQUE(post_id, user_id)` — 중복 방지의 최종 방어선은 DB 제약 |
| [운영 DB 반영 스크립트](../scripts/sql/2026-10-15_concurrency_constraints.sql) | 기존 데이터 중복 점검 → 제약 추가 → `SHOW INDEX` 확인 |

### ③ 확장을 고려한 도메인 설계

| 볼 곳 | 확인 포인트 |
| --- | --- |
| [`AuthService` 라우팅](../src/main/java/app/usfit/api/user/service/AuthService.java#L27-L69) · [`LoginHandler`](../src/main/java/app/usfit/api/user/service/login/LoginHandler.java) | 로그인 수단별 구현체를 Map으로 라우팅. 새 수단은 클래스 추가만으로 |
| [`KakaoLoginHandler`](../src/main/java/app/usfit/api/user/service/login/KakaoLoginHandler.java) | 앱이 받은 인가 코드로 카카오 토큰을 직접 교환, `(provider, providerId)`로 계정 식별 |
| [`changeMemberRole` 외 권한 처리](../src/main/java/app/usfit/api/club/service/ClubinfoService.java#L105-L320) | OWNER / ADMIN / MEMBER 권한 계층, 역할 변경은 ADMIN·MEMBER만 허용 |
| [`recommendClubs`](../src/main/java/app/usfit/api/club/service/ClubService.java#L291-L474) | 조건을 단계적으로 완화하며 id만 조회 → 엔티티는 IN 쿼리 한 번 → Map으로 순서 복원 |
| [`UserProfile`](../src/main/java/app/usfit/api/user/entity/UserProfile.java#L29-L96) · [`getSimpleProfile`](../src/main/java/app/usfit/api/user/service/ProfileService.java#L144-L145) | `@MapsId` 공유 PK와 `Persistable`, 조회 실패가 OSIV의 EntityManager를 비우지 않도록 `noRollbackFor` |

---

## 3. 알고 있는 한계

- 추천 쿼리에서 컬럼에 `LOWER()`를 씌운 비교는 인덱스를 타지 못합니다. 데이터가 늘면 정규화 컬럼이 필요합니다.
- Access Token만 쓰는 무상태 인증이라, 탈퇴·정지 계정의 토큰을 즉시 막으려면 별도 저장소(예: Redis)에 토큰 버전을 두는 방식이 필요합니다.
- 구글 로그인은 핸들러 자리만 만들어 둔 상태(미구현)입니다.
