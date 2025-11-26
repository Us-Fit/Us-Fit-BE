# 🏋️‍♂️ UsFit – 운동으로 연결되는 우리

> **스포츠 시설 정보부터 동호회·용병 매칭까지 한 번에!**  
> 공공데이터와 커뮤니티 기능을 묶어 체육활동 참여를 촉진하는 통합 스포츠 플랫폼

---

## 📱 주요 화면 (Screenshots)
(앱 주요 화면 캡처 추가 예정)

---

## 👥 팀구성

<table align="center">
  <tbody>
    <tr>
      <th>Team Leader</th>
      <th>Team Member</th>
      <th>Team Member</th>
      <th>Team Member</th>
    </tr>
    <tr>
      <td align="center">
        <a href="https://github.com/gitIt-sehyeon">
          <img src="https://github.com/gitIt-sehyeon.png?size=100" width="100px" alt="정세현"/>
          <br />
          <b>정세현</b>
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/YoonB-dev">
          <img src="https://github.com/YoonB-dev.png?size=100" width="100px" alt="이윤형"/>
          <br />
          <b>이윤형</b>
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/kimtaeyeon04">
          <img src="https://github.com/kimtaeyeon04.png?size=100" width="100px" alt="김태연"/>
          <br />
          <b>김태연</b>
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/BROWNIE-TARTE">
          <img src="https://github.com/BROWNIE-TARTE.png?size=100" width="100px" alt="안예준"/>
          <br />
          <b>안예준</b>
        </a>
      </td>
    </tr>
    <tr>
      <td align="center">
        <a href="" target="_blank">개인 리포트</a>
      </td>
      <td align="center">
        <a href="" target="_blank">개인 리포트</a>
      </td>
      <td align="center">
        <a href="" target="_blank">개인 리포트</a>
      </td>
      <td align="center">
        <a href="" target="_blank">개인 리포트</a>
      </td>
    </tr>
  </tbody>
</table>

---

## 📖 프로젝트 개요

**UsFit** 백엔드는 Spring Boot 3 기반의 REST API 서버로 다음과 같은 도메인을 제공합니다.

- **시설 & 강좌**: 공공데이터 기반 시설/강좌 CSV를 배치로 적재하고, 위치·종목 조건으로 검색
- **동호회 커뮤니티**: 클럽 생성, 가입 승인, 멤버 역할/권한 관리, 강제 탈퇴/위임 등 운영 도구
- **용병 매칭**: 모집글 작성·조회, 신청/수락/거절, 내 모집글/내 신청 현황 조회
- **리뷰**: 시설·강좌 대상 리뷰 CRUD + AWS S3 이미지 업로드/삭제
- **회원 & 인증**: 자체 회원가입/로그인, Kakao OAuth2.0, Google(스텁), JWT 기반 보호 API, 사용자 프로필/관심 종목 관리

> 🎯 **목표**: 체육시설 접근성을 높이고, 개인 → 커뮤니티 → 지역사회로 이어지는 건강한 운동 문화를 확산

---

## 🧭 백엔드 기능 맵

| 영역 | 주요 엔드포인트 | 설명 |
| --- | --- | --- |
| 시설(`FacilityController`) | `GET /api/facility/{id}` `GET /api/facility/search` `GET /api/facility/nearby` | 시설 상세·조건·반경 검색. 종목 필터링은 `SportRepository`와 연계 |
| 강좌(`CourseController`) | `GET /api/course/search` | `itemNm + 시/군/구` 조건으로 공공 강좌 조회 |
| 동호회(`Club*Controller`) | `POST /api/clubs` `GET /api/clubs` `GET/POST/PATCH/DELETE /api/club-info/**` | 클럽 생성, 멤버 역할 변경, 관리자 권한 부여/회수, 탈퇴/강퇴 등 운영 기능 |
| 가입요청(`ClubJoinController`) | `POST /api/club/{id}/requests` `GET /api/club/{id}/requests` `POST /api/club/join/requests/{id}/decision` | 가입 신청/승인/거절 및 중복 신청 방지 |
| 용병(`RecruitPlayer*Controller`) | `POST /api/recruits` `GET /api/recruits` `GET /api/recruits/me` | 모집글 CRUD (활성 상태, 내 글 조회) |
| 지원(`RecruitApplicationController`) | `POST /api/recruits/{postId}/applications` `PATCH .../{applicationId}` `GET .../me` | 신청/목록/상태 변경/내 신청 조회 |
| 리뷰(`ReviewController`) | `POST/PUT/DELETE /api/review` `GET /api/review` | 멀티파트 요청으로 리뷰 본문과 이미지 동시 처리, AWS S3에 업로드 |
| 사용자(`UserController`, `ProfileController`) | `POST /api/user/signup` `POST /api/user/login` `GET/POST /api/user/profile` | 회원가입/로그인/JWT 발급, 프로필(신체정보 + 관심운동) 업서트 |

OpenAPI(Swagger UI)는 `http://localhost:8080/swagger-ui/index.html`에서 JWT 베어러 인증으로 테스트할 수 있습니다.

---

## 🔐 인증 & 보안

- **JWT**: `JwtTokenProvider`가 `jwt.secret`, `jwt.access-token-validity-seconds` 값을 사용해 액세스 토큰 생성/검증
- **보안 필터**: `JwtAuthenticationFilter`가 `/api/user/login`, `/api/user/signup`, `/api/facility/**`, `/api/course/**` 등 화이트리스트를 제외한 모든 요청을 보호
- **OAuth**: `KakaoLoginHandler`가 auth code → access token 교환 후 사용자 upsert, `GoogleLoginHandler`는 토큰 연동 전까지 placeholder 로직
- **CORS**: `SecurityConfig`에서 `http://localhost:3000`, `http://3.27.134.2:8080` 도메인을 허용

---

## 🏗 시스템 아키텍처

```mermaid
graph LR
  UserApp[React Native App] -->|REST API| Backend[(Spring Boot API Server)]
  Backend --> DB[(MySQL 8.x / RDS)]
  Backend --> S3[(AWS S3 - 리뷰 이미지)]
  Backend --> Kakao[(Kakao OAuth2.0)]
```

---

## 🧱 프로젝트 구조

```
.
├── build.gradle              # Spring Boot 3.5, Java 21, JPA, JWT, AWS SDK
├── src
│   ├── main
│   │   ├── java/app/usfit/api
│   │   │   ├── facility/      # 시설 엔티티·검색·CSV import
│   │   │   ├── course/        # 강좌 엔티티·검색·CSV import
│   │   │   ├── club/          # 클럽, 멤버, join workflow
│   │   │   ├── RecruitPlayer/ # 용병 모집글 & 신청
│   │   │   ├── review/        # 리뷰 + S3 이미지 업로드
│   │   │   ├── user/          # 회원, 프로필, 로그인 전략
│   │   │   ├── security/jwt/  # JWT 필터/토큰
│   │   │   └── config/        # Security, Swagger, S3 설정
│   │   └── resources
│   │       ├── application.properties
│   │       └── static/kakao-test.html
│   └── test/java/app/usfit/api
└── README.md
```

---

## 🛠 기술 스택

- **Language**: Java 21
- **Framework**: Spring Boot 3.5, Spring Security, Spring Data JPA, Spring Validation
- **Persistence**: MySQL 8.x, Hibernate, H2 (테스트)
- **Infra & Library**: AWS S3 SDK v2, jjwt 0.11, SpringDoc OpenAPI, Apache Commons CSV
- **Build**: Gradle 8 (Wrapper)

---

## ⚙️ 환경 변수

| 변수 | 설명 | 기본값 (`application.properties`) |
| --- | --- | --- |
| `DB_URL`, `DB_USER`, `DB_PASS` | MySQL 접속 정보 | `jdbc:mysql://localhost:3306/UsFit`, `root`, `Willylee0309!` |
| `JWT_SECRET` | JWT 서명 키 (Base64 권장) | 없음 (필수) |
| `JWT_ACCESS_TTL` | 액세스 토큰 TTL(초) | 없음 (필수) |
| `KAKAO_CLIENT_ID`, `KAKAO_CLIENT_SECRET`, `KAKAO_REDIRECT_URI`, `KAKAO_TOKEN_URI`, ... | Kakao OAuth 설정 | 빈 값 → 환경별 주입 |
| `cloud.aws.s3.bucket`, `cloud.aws.s3.region` | 리뷰 이미지 업로드 대상 버킷/리전 | `usfit-s3-bucket`, `ap-southeast-2` |

> 운영 환경에서는 `.env` 혹은 시스템 환경 변수로 위 값을 덮어써 주세요.

---

## 🚀 로컬 실행

1. **사전 준비**  
   - JDK 21, MySQL 8.x, AWS CLI 자격 증명 (S3 업로드용), Kakao REST API 키
2. **DB 스키마**  
   ```sql
   CREATE DATABASE UsFit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
   ```
3. **의존성 설치 & 테스트**
   ```bash
   ./gradlew clean test
   ```
4. **서버 실행**
   ```bash
   ./gradlew bootRun
   ```
5. **API 문서 확인**  
   - Swagger UI: `http://localhost:8080/swagger-ui/index.html`
   - OpenAPI JSON: `http://localhost:8080/v3/api-docs`

---

## 🗂 데이터 적재 (CSV Import)

대용량 공공데이터는 별도의 서비스에서 일괄 적재합니다.

- `FacilityImportService`  
  - 중복 방지를 위해 이름+도로명 주소 키로 upsert  
  - CSV 헤더 기반으로 주소/좌표/연락처/면적/실내외 여부 등을 매핑  
  - `importCsv(MultipartFile, boolean upsert, Charset)` 또는 `importCsv(Path, boolean upsert, Charset)`

- `CourseImportService`  
  - `COURSE_BEGIN_DE`가 2025년인 행만 필터링  
  - Batch size 1000으로 JPA `EntityManager` flush/clear  
  - `mapRecordToCourse`에서 모든 주소/강좌 정보를 매핑

`CommandLineRunner`, `@Scheduled` 작업, 혹은 임시 admin API에서 위 서비스를 호출해 주입할 수 있습니다.

---

## 🧪 테스트 & 품질

- 유닛/통합 테스트: `./gradlew test`
- Swagger 문서로 엔드포인트 통합 검증
- Validator/JPA 에러는 IDE 또는 Spring Boot 에러 로그를 통해 확인

---

## 🗺 로드맵 & TODO

- React Native 앱과의 실시간 인터랙션 (지도, 채팅) 연계
- Google OAuth 정식 연동 및 Refresh Token 발급
- 클럽/모집글 검색 필터 고도화, Elasticsearch 연동
- AWS ECS / RDS / CloudFront 배포 및 Observability (CloudWatch, X-Ray)

---

## 📱 주요 화면 (Screenshots)

(앱 주요 화면 캡처 예정)

---
