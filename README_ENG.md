[한국어](./README.md) | English

# UsFit - Connecting People Through Sports

<div align="center">

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5.6-brightgreen.svg)
![Java](https://img.shields.io/badge/Java-21-orange.svg)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue.svg)
![License](https://img.shields.io/badge/License-MIT-yellow.svg)

**A Sports Life Integration Platform Powered by Public Data from the Korea Sports Promotion Foundation**

From public sports facilities to club activities and mercenary (pickup player) matching – all in one app.

UsFit combines public data and community features to boost sports participation across Korea.

</div>

---

## Table of Contents

- [Project Overview](#project-overview)
- [Core Values](#core-values)
- [Main Features](#main-features)
- [Public Data Utilization](#public-data-utilization)
- [Tech Stack](#tech-stack)
- [System Architecture](#system-architecture)
- [Database ERD](#database-erd)
- [Project Structure](#project-structure)
- [Team](#team)
- [Setup & Run](#setup--run)
- [Public Data CSV Import](#public-data-csv-import)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Expected Impact](#expected-impact)
- [Future Work](#future-work)
- [Contact & Contribution](#contact--contribution)
- [License](#license)
- [Acknowledgements](#acknowledgements)

---

## Project Overview

**UsFit** is an integrated sports life platform that leverages public sports facility and course data provided by the **Korea Sports Promotion Foundation (KSPF)**.

Users can easily:

- Discover public sports facilities
- Join local sports clubs
- Recruit or join mercenary (pickup) players for games

### Competition Context

- **Data Source**: Public datasets from the KSPF via the Public Data Portal
    - Nationwide public sports facility information (location, sports type, facility size, etc.)
    - Public sports course information (program name, operation period, fee, etc.)
- **Problem Recognition**:
    - Public sports facility information is fragmented and hard to search
    - People who work out alone struggle to find teammates or opponents
    - Lack of structured information about local sports clubs creates a barrier to participation
- **Our Solution**:
  An **all-in-one platform** that combines:
    - Public data–based facility & course search
    - Community features (clubs, reviews, mercenary matching)

---

## Core Values

### 1. **Improved Accessibility to Public Data**

We systematically clean and organize KSPF public data so that anyone can easily search sports facilities and courses nationwide.

### 2. **Finding Workout Buddies**

Through the mercenary (pickup player) system, users can quickly recruit needed players and meet new workout partners.

### 3. **Activation of Sports Clubs**

UsFit provides structured club management, including member registration, role assignment, and activity records.

### 4. **Local Community Connection**

Reviews and ratings allow users to share their experience and help foster active local sports communities.

---

## Main Features

### 1. **Public Sports Facility Search**

- **Location-based Search**: Find facilities within N km from the current location
- **Filter-based Search**: Filter by sport, city/gu, indoor/outdoor, facility size
- **Detailed Information**: Address, contact info, area, and map integration

### 2. **Public Sports Course Search**

- Search courses from KSPF datasets
- View course name, region, operation period, and fee
- Automatically filter for **latest courses (e.g., 2025)**

### 3. **Club Community**

- **Club Creation & Management**: Create clubs, register sports types, manage introductions
- **Membership System**: Join request / approval / rejection workflow
- **Roles & Permissions**: Admin, regular members, etc.
- **Member Management**: Leave, expel, delegate admin role

### 4. **Mercenary (Pickup Player) Matching**

- **Recruitment Post Creation**: Date, time, place, required number of players
- **Application Management**: View applicants, accept/reject
- **My Activity**: View posts I created or applied to
- **Status Management**: Automatically manage recruitment status (open/closed)

### 5. **Facility & Course Reviews**

- **Write Reviews**: Rating, text content, image upload (stored in AWS S3)
- **View Reviews**: List reviews by facility or course
- **Manage Reviews**: Edit and delete

### 6. **User Authentication**

- **Local Sign-up / Login**: Form-based signup/signin
- **Social Login**: Kakao OAuth 2.0 integration
- **JWT**: Token-based authentication for secure API access
- **Profile Management**: Store body info and preferred sports

---

## Public Data Utilization

### Data Source

- **Provider**: Korea Sports Promotion Foundation (KSPF)
- **Format**: CSV
- **Scale**: ~140K facility records, ~200K course records nationwide

### Data Processing Pipeline

```
1. Collect CSV files from KSPF
   ↓
2. Clean & validate using Spring Batch
   ↓
3. Store in MySQL database
   ↓
4. Expose through REST APIs
   ↓
5. Provide real-time search via the mobile app
```

### Data Transformation

| Raw Data Field | Processing | Usage |
| --- | --- | --- |
| Facility name, address | Deduplication, coordinate conversion | Location-based search |
| Sport type | Standardization and categorization | Sport-based filtering |
| Contact, website | Format normalization & validation | Display to end users |
| Course schedule | Date parsing, latest-data filtering | Show currently running courses |

### Data Update Strategy

- **Initial Load**: Bulk import CSVs using `CommandLineRunner`
- **Regular Updates**: Periodic CSV download & upsert
- **Duplicate Handling**: Use (facility name + address) as a composite unique key

---

## Tech Stack

### Backend

<img src="https://img.shields.io/badge/Java_21-007396?style=for-the-badge&logo=openjdk&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Boot_3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Data_JPA-6DB33F?style=for-the-badge&logo=spring&logoColor=white"/> <img src="https://img.shields.io/badge/Hibernate-59666C?style=for-the-badge&logo=hibernate&logoColor=white"/>

### Database

<img src="https://img.shields.io/badge/MySQL_8-4479A1?style=for-the-badge&logo=mysql&logoColor=white"/> <img src="https://img.shields.io/badge/AWS_RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white"/>

### Security & Auth

<img src="https://img.shields.io/badge/JWT-000000?style=for-the-badge&logo=jsonwebtokens&logoColor=white"/> <img src="https://img.shields.io/badge/Spring_Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white"/> <img src="https://img.shields.io/badge/OAuth_2.0-4285F4?style=for-the-badge&logo=google&logoColor=white"/>

### API & Docs

<img src="https://img.shields.io/badge/REST_API-005571?style=for-the-badge&logo=apache&logoColor=white"/> <img src="https://img.shields.io/badge/SpringDoc_OpenAPI-85EA2D?style=for-the-badge&logo=swagger&logoColor=white"/> <img src="https://img.shields.io/badge/Swagger-85EA2D?style=for-the-badge&logo=swagger&logoColor=black"/>

### External Integrations

<img src="https://img.shields.io/badge/Kakao_Login-FFCD00?style=for-the-badge&logo=kakaotalk&logoColor=000"/> <img src="https://img.shields.io/badge/Kakao_Map_API-FFCD00?style=for-the-badge&logo=kakaotalk&logoColor=000"/> <img src="https://img.shields.io/badge/Apache_Commons_CSV-D22128?style=for-the-badge&logo=apache&logoColor=white"/>

### Infrastructure

<img src="https://img.shields.io/badge/AWS_EC2-FF9900?style=for-the-badge&logo=amazonec2&logoColor=white"/> <img src="https://img.shields.io/badge/AWS_RDS-527FFF?style=for-the-badge&logo=amazonrds&logoColor=white"/> <img src="https://img.shields.io/badge/AWS_S3-569A31?style=for-the-badge&logo=amazons3&logoColor=white"/>

### Build & Tools

<img src="https://img.shields.io/badge/Gradle_8-02303A?style=for-the-badge&logo=gradle&logoColor=white"/> <img src="https://img.shields.io/badge/Lombok-CA4245?style=for-the-badge&logoColor=white"/> <img src="https://img.shields.io/badge/Git-F05032?style=for-the-badge&logo=git&logoColor=white"/> <img src="https://img.shields.io/badge/GitHub-181717?style=for-the-badge&logo=github&logoColor=white"/> <img src="https://img.shields.io/badge/Postman-FF6C37?style=for-the-badge&logo=postman&logoColor=white"/> <img src="https://img.shields.io/badge/IntelliJ_IDEA-000000?style=for-the-badge&logo=intellijidea&logoColor=white"/>

---

## System Architecture

<img width="800" alt="System Architecture" src="images/system-architecture.png" />

### Architecture Highlights

- **3-Tier Architecture**: Clear separation of Presentation, Business Logic, and Data Access
- **RESTful APIs**: Intuitive API design using standard HTTP methods
- **JWT-based Authentication**: Stateless token approach for scalability
- **JPA/Hibernate**: Object-oriented data access and DB independence
- **AWS Infrastructure**: EC2 (server), RDS (database), and S3 (storage) for reliable operations

---

## Database ERD

<img width="800" alt="Database ERD" src="images/ERD.png" />

**Key Entities:**

- **User**: User information & profile
- **Club**: Sports club and its members
- **Facility**: Public sports facility
- **Course**: Public sports course
- **RecruitPlayer**: Mercenary recruitment & applications
- **Review**: Facility / course reviews
- **Sport**: Sport types

**Key Relationships (examples):**

- User ↔ UserProfile (1:1)
- User ↔ Club ↔ ClubMember (M:N)
- Club ↔ Sport (M:N via ClubSport)
- Facility ↔ Sport (M:N via FacilitySport)
- RecruitPlayerPost ↔ User (M:N via RecruitApplication)

---

### Project Structure

```json
Us-Fit-BE/
├── src/
│   ├── main/
│   │   ├── java/app/usfit/api/
│   │   │   ├── club/                    # Club domain
│   │   │   │   ├── controller/          # Club REST controllers
│   │   │   │   ├── dto/                 # Request/response DTOs
│   │   │   │   ├── entity/              # Club entities (Club, ClubMember, ClubJoin, ...)
│   │   │   │   ├── repository/          # Club JPA repositories
│   │   │   │   ├── service/             # Club business logic
│   │   │   │   └── activity/            # Club activity domain
│   │   │   │       ├── controller/      # Activity REST controllers
│   │   │   │       ├── dto/             # Activity DTOs
│   │   │   │       ├── entity/          # Activity entities (Activity, ActivityMember, ...)
│   │   │   │       ├── repository/      # Activity repositories
│   │   │   │       └── service/         # Activity business logic
│   │   │   │
│   │   │   ├── facility/                # Facility domain
│   │   │   │   ├── controller/          # Facility search APIs
│   │   │   │   ├── dto/                 # Facility DTOs
│   │   │   │   ├── entity/              # Facility entities (Facility, FacilityAddress, ...)
│   │   │   │   ├── repository/          # Facility repositories (including location queries)
│   │   │   │   └── service/             # Search logic & CSV import
│   │   │   │
│   │   │   ├── course/                  # Course domain
│   │   │   │   ├── controller/          # Course search APIs
│   │   │   │   ├── dto/                 # Course DTOs
│   │   │   │   ├── entity/              # Course entities
│   │   │   │   ├── repository/          # Course repositories
│   │   │   │   └── service/             # Search & CSV import logic
│   │   │   │
│   │   │   ├── RecruitPlayer/           # Mercenary (pickup) domain
│   │   │   │   ├── controller/          # Recruitment APIs
│   │   │   │   ├── dto/                 # Recruitment DTOs
│   │   │   │   ├── entity/              # Recruitment & application entities
│   │   │   │   ├── repository/          # Recruitment repositories
│   │   │   │   └── service/             # Matching logic
│   │   │   │
│   │   │   ├── review/                  # Review domain
│   │   │   │   ├── controller/          # Review APIs
│   │   │   │   ├── dto/                 # Review DTOs
│   │   │   │   ├── entity/              # Review entities
│   │   │   │   ├── repository/          # Review repositories
│   │   │   │   └── service/             # Review logic & S3 upload
│   │   │   │
│   │   │   ├── sport/                   # Sport domain
│   │   │   │   ├── controller/          # Sport APIs
│   │   │   │   ├── dto/                 # Sport DTOs
│   │   │   │   ├── entity/              # Sport entities
│   │   │   │   ├── repository/          # Sport repositories
│   │   │   │   └── service/             # Sport management logic
│   │   │   │
│   │   │   ├── user/                    # User domain
│   │   │   │   ├── controller/          # User APIs
│   │   │   │   ├── dto/                 # User & profile DTOs
│   │   │   │   ├── entity/              # User, UserProfile, UserInterestSport
│   │   │   │   ├── repository/          # User repositories
│   │   │   │   └── service/             # User management logic
│   │   │   │
│   │   │   ├── oauth/                   # OAuth integration
│   │   │   │   ├── service/             # Kakao login handlers
│   │   │   │   └── dto/                 # OAuth DTOs
│   │   │   │
│   │   │   ├── security/                # Security config
│   │   │   │   ├── JwtAuthenticationFilter.java  # JWT filter
│   │   │   │   ├── JwtTokenProvider.java         # JWT generator/validator
│   │   │   │   └── SecurityConfig.java           # Spring Security configuration
│   │   │   │
│   │   │   ├── config/                  # Application configs
│   │   │   │   ├── SwaggerConfig.java   # OpenAPI configuration
│   │   │   │   ├── S3Config.java        # AWS S3 configuration
│   │   │   │   └── WebConfig.java       # CORS, etc.
│   │   │   │
│   │   │   ├── common/                  # Common utilities
│   │   │   │   ├── exception/           # Custom exceptions
│   │   │   │   └── response/            # Standard API response format
│   │   │   │
│   │   │   └── UsFitApplication.java    # Spring Boot main class
│   │   │
│   │   └── resources/
│   │       ├── application.properties       # Main config (DB, JWT, S3, OAuth)
│   │       ├── application-test.properties  # Test config
│   │       └── static/                      # Static resources (e.g., OAuth testing)
│   │
│   └── test/
│       └── java/app/usfit/api/              # Unit & integration tests
│
├── build.gradle
├── settings.gradle
├── gradlew, gradlew.bat
└── README.md

```

## Team

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
          <img src="https://github.com/gitIt-sehyeon.png?size=100" width="100px" alt="Jung Se-hyeon"/>
          <br />
          <b>정세현</b>
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/YoonB-dev">
          <img src="https://github.com/YoonB-dev.png?size=100" width="100px" alt="Lee Yoon-hyeong"/>
          <br />
          <b>이윤형</b>
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/kimtaeyeon04">
          <img src="https://github.com/kimtaeyeon04.png?size=100" width="100px" alt="Kim Tae-yeon"/>
          <br />
          <b>김태연</b>
        </a>
      </td>
      <td align="center">
        <a href="https://github.com/BROWNIE-TARTE">
          <img src="https://github.com/BROWNIE-TARTE.png?size=100" width="100px" alt="Ahn Ye-jun"/>
          <br />
          <b>안예준</b>
        </a>
      </td>
    </tr>
    <tr>
      <td align="center">
        <a href="" target="_blank">Personal Report</a>
      </td>
      <td align="center">
        <a href="" target="_blank">Personal Report</a>
      </td>
      <td align="center">
        <a href="" target="_blank">Personal Report</a>
      </td>
      <td align="center">
        <a href="" target="_blank">Personal Report</a>
      </td>
    </tr>
  </tbody>
</table>

---

## Setup & Run

### Prerequisites

- **JDK 21+**
- **MySQL 8.0+**
- **Gradle 8.x** (wrapper included)
- **AWS Account** (for S3/RDS)
- **Kakao Developers Account** (for OAuth)

### 1. Database Setup

```sql
-- Create database
CREATE DATABASE UsFit CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- Optional: create dedicated user
CREATE USER 'usfit_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON UsFit.* TO 'usfit_user'@'localhost';
FLUSH PRIVILEGES;

```

### 2. Environment Variables / Properties

You can set these in `application.properties` or as system environment variables:

| Variable | Description | Example |
| --- | --- | --- |
| `DB_URL` | MySQL URL | `jdbc:mysql://localhost:3306/UsFit?useSSL=false&serverTimezone=Asia/Seoul` |
| `DB_USER` | MySQL username | `root` / `usfit_user` |
| `DB_PASS` | MySQL password | `your_password` |
| `JWT_SECRET` | JWT signing key (Base64) | `your_secret_key_base64_encoded` |
| `JWT_ACCESS_TTL` | JWT access token TTL (s) | `86400` (24h) |
| `KAKAO_CLIENT_ID` | Kakao REST API key | `your_kakao_rest_api_key` |
| `KAKAO_CLIENT_SECRET` | Kakao client secret | `your_kakao_client_secret` |
| `KAKAO_REDIRECT_URI` | Kakao redirect URI | `http://localhost:8080/oauth2/callback/kakao` |
| `AWS_S3_BUCKET` | S3 bucket name | `usfit-s3-bucket` |
| `AWS_REGION` | AWS region | `ap-southeast-2` |
| `AWS_ACCESS_KEY_ID` | AWS access key | `your_aws_access_key` |
| `AWS_SECRET_ACCESS_KEY` | AWS secret key | `your_aws_secret_key` |

### 3. Run the Application

### Windows (cmd)

```bash
gradlew.bat clean build
gradlew.bat bootRun

```

### Linux / macOS

```bash
./gradlew clean build
./gradlew bootRun

```

### 4. Server Check

- Server: `http://3.27.134.2:8080`
- Swagger UI: `http://3.27.134.2:8080/swagger-ui/index.html#`

---

## Public Data CSV Import

### Facility Data Import

```java
// Example usage of FacilityImportService
@Autowired
private FacilityImportService facilityImportService;

Path csvPath = Paths.get("path/to/facility_data.csv");
facilityImportService.importCsv(csvPath, true, StandardCharsets.UTF_8);

```

**Key Features:**

- Duplicate prevention using (facility name + road address)
- Automatic mapping based on CSV header
- Automatic parsing of latitude/longitude

### Course Data Import

```java
@Autowired
private CourseImportService courseImportService;

Path csvPath = Paths.get("path/to/course_data.csv");
courseImportService.importCsv(csvPath, true, StandardCharsets.UTF_8);

```

**Key Features:**

- Filters to only import latest (e.g., 2025) courses
- Batch insert (e.g., 1000 rows per batch) for performance
- Automatic parsing of course name, operation period, fee, etc.

### Initial Data Load with CommandLineRunner

```java
@Bean
@Profile("dev")
CommandLineRunner initData(FacilityImportService facilityService,
                           CourseImportService courseService) {
    return args -> {
        facilityService.importCsv(Paths.get("data/facilities.csv"), true, StandardCharsets.UTF_8);
        courseService.importCsv(Paths.get("data/courses.csv"), true, StandardCharsets.UTF_8);
        log.info("Initial public data load completed");
    };
}

```

---

## API Documentation

### Swagger UI

Once the server is running:

```
http://3.27.134.2:8080/swagger-ui/index.html#

```

### Main API Endpoints (Examples)

### Facility API

- `GET /api/facility/{id}` – Get facility details
- `GET /api/facility/search` – Search by sport, region, etc.
- `GET /api/facility/nearby` – Search facilities within N km from a given location

### Course API

- `GET /api/course/search` – Search courses by name, region, etc.

### Club API

- `POST /api/clubs` – Create a club
- `GET /api/clubs` – List clubs
- `POST /api/club/{id}/requests` – Request to join a club
- `POST /api/club/join/requests/{id}/decision` – Approve / reject join request
- `GET /api/club-info/{id}` – Get club details
- `PATCH /api/club-info/{id}/member/{memberId}` – Change member role

### Club Activity API

- `POST /api/clubs/{clubId}/activities` – Create activity
- `GET /api/clubs/{clubId}/activities` – List activities
- `POST /api/clubs/{clubId}/activities/{activityId}/join` – Join activity
- `GET /api/clubs/{clubId}/activities/{activityId}/members` – List activity members
- `POST /api/clubs/{clubId}/activities/members/{activityMemberId}/status` – Change member status

### Mercenary Recruitment API

- `POST /api/recruits` – Create recruitment post
- `GET /api/recruits` – List recruitment posts
- `GET /api/recruits/me` – Posts created by me
- `POST /api/recruits/{postId}/applications` – Apply to a post
- `PATCH /api/recruits/applications/{applicationId}` – Change application status
- `GET /api/recruits/applications/me` – Applications submitted by me

### Review API

- `POST /api/review` – Create review (with image upload)
- `GET /api/review` – List reviews
- `PUT /api/review/{id}` – Update review
- `DELETE /api/review/{id}` – Delete review

### User API

- `POST /api/user/signup` – Sign up
- `POST /api/user/login` – Login (issue JWT)
- `GET /api/user/profile` – Get profile
- `POST /api/user/profile` – Create / update profile
- `GET /api/oauth/kakao` – Kakao login

### JWT Authentication (Swagger)

1. Login via `/api/user/login` and get a JWT access token
2. Click `Authorize` in Swagger UI
3. Enter `Bearer {token}` (include a space after `Bearer`)
4. Call secured APIs

---

## Testing

### Run Tests

```bash
gradlew.bat test

```

### Coverage Report (JaCoCo)

```bash
gradlew.bat jacocoTestReport

```

Report path: `build/reports/jacoco/test/html/index.html`

---

## Expected Impact

### Social Value

1. **Expanded Use of Public Data**
    - Increases accessibility of KSPF data
    - Helps democratize access to sports facility information
2. **Healthier Exercise Culture**
    - Activates local communities through clubs
    - Enables solo exercisers to join team sports via mercenary matching
3. **Increased Utilization of Public Facilities**
    - Easier search leads to higher utilization
    - Review feedback loop helps improve facility quality

### Technical Value

1. **Scalable Architecture**
    - Domain-driven design that can evolve into microservices
    - RESTful API supports various clients (web, mobile)
2. **Data-driven Decisions**
    - Analyze user activity for service improvement
    - Analyze demand by region/sport
3. **Potential as an Open Platform**
    - OpenAPI (Swagger) simplifies external integration
    - Can be expanded as a public sports API platform

---

## Future Work

- [ ]  **AI-based Recommendation System**: Recommend facilities and clubs based on user preferences
- [ ]  **Real-time Notifications**: FCM-based notifications for club activities and mercenary matches
- [ ]  **Payment Integration**: Club membership fees and facility reservation payments
- [ ]  **Chat Functionality**: Club chat rooms and 1:1 chat for matches
- [ ]  **Admin Dashboard**: Visual statistics for users and facilities
- [ ]  **Multi-language Support**: English, Chinese, etc. for foreign users

---

## Contact & Contribution

### Project Inquiries

- Issues: [GitHub Issues](https://github.com/Us-Fit/Us-Fit-BE/issues)
- Email: Team representative email

### How to Contribute

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

---

## License

This project is licensed under the MIT License. See the `LICENSE` file for details.

---

## Acknowledgements

- **Korea Sports Promotion Foundation (KSPF)**: For providing high-quality public sports data
- **Kakao Developers**: For Kakao Map API & OAuth
- **AWS**: For cloud infrastructure
- **Spring Community**: For the framework and documentation

---

<div align="center">

**UsFit – Connecting people through sports, building a healthier Korea together.**

Made with ❤️ by Us-Fit Team

</div>
```
