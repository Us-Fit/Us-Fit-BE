-- =====================================================================
-- 동시성 수정(PR #72) 운영 DB 반영 스크립트
--
-- 배경
--  - ddl-auto=update는 새 컬럼(accepted_count)을 만들어도 기존 값을 채워주지 않음
--    -> 이미 ACCEPTED 신청서가 있는 모집글은 0부터 시작해 정원보다 더 받게 됨
--  - 기존 데이터에 중복이 있으면 UNIQUE 생성이 실패하는데, 기본 설정에서는 로그만 남고 넘어감
--
-- 실행 순서
--  0) 백업
--  1) 사전 점검(STEP 1) 결과가 모두 0행인지 확인 -> 아니면 정리 후 진행
--  2) STEP 2~4 실행
--  3) 검증(STEP 5)
--
-- 서버 배포(ddl-auto=update) 후에 실행해도 되고, 배포 전에 실행해도 됨
-- (배포 전이면 STEP 2의 컬럼 추가 주석을 해제)
-- =====================================================================


-- ---------------------------------------------------------------------
-- STEP 1. 사전 점검 (모두 0행이어야 UNIQUE 추가 가능)
-- ---------------------------------------------------------------------

-- 1-1) 같은 소셜 계정(provider, provider_id) 중복
SELECT provider, provider_id, COUNT(*) AS cnt, GROUP_CONCAT(id ORDER BY id) AS user_ids
FROM users
WHERE provider_id IS NOT NULL
GROUP BY provider, provider_id
HAVING COUNT(*) > 1;

-- 1-2) 같은 사용자의 같은 모집글 중복 지원
SELECT post_id, user_id, COUNT(*) AS cnt, GROUP_CONCAT(application_id ORDER BY application_id) AS application_ids
FROM recruit_applications
GROUP BY post_id, user_id
HAVING COUNT(*) > 1;

-- 1-3) (참고) 과거 버그로 이미 정원을 초과해 수락된 모집글
SELECT p.post_id, p.max_member, COUNT(a.application_id) AS accepted_rows
FROM recruit_posts p
JOIN recruit_applications a ON a.post_id = p.post_id AND a.status = 'ACCEPTED'
GROUP BY p.post_id, p.max_member
HAVING COUNT(a.application_id) > p.max_member;
-- 결과가 있으면 서비스 정책에 맞게 정리 필요 (자동 정리하지 않음)
-- 정리하지 않으면 해당 모집글은 accepted_count >= max_member가 되어 추가 수락만 막힘


-- ---------------------------------------------------------------------
-- STEP 2. accepted_count 컬럼 (서버 배포 전에 실행하는 경우에만 주석 해제)
-- ---------------------------------------------------------------------
-- ALTER TABLE recruit_posts ADD COLUMN accepted_count INT NOT NULL DEFAULT 0;


-- ---------------------------------------------------------------------
-- STEP 3. accepted_count를 실제 ACCEPTED 신청서 수로 맞추기
-- ---------------------------------------------------------------------
UPDATE recruit_posts p
SET p.accepted_count = (
    SELECT COUNT(*)
    FROM recruit_applications a
    WHERE a.post_id = p.post_id
      AND a.status = 'ACCEPTED'
);


-- ---------------------------------------------------------------------
-- STEP 4. UNIQUE 제약 추가 (이미 있으면 "Duplicate key name" 에러 -> 무시해도 됨)
-- ---------------------------------------------------------------------
ALTER TABLE users
    ADD CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id);

ALTER TABLE recruit_applications
    ADD CONSTRAINT uk_recruit_application_post_user UNIQUE (post_id, user_id);


-- ---------------------------------------------------------------------
-- STEP 5. 검증
-- ---------------------------------------------------------------------

-- 5-1) 제약이 실제로 생겼는지 (각 2행씩 나와야 함)
SHOW INDEX FROM users WHERE Key_name = 'uk_users_provider_provider_id';
SHOW INDEX FROM recruit_applications WHERE Key_name = 'uk_recruit_application_post_user';

-- 5-2) accepted_count와 실제 ACCEPTED 수가 다른 모집글 (0행이어야 함)
SELECT p.post_id, p.accepted_count,
       (SELECT COUNT(*) FROM recruit_applications a
        WHERE a.post_id = p.post_id AND a.status = 'ACCEPTED') AS accepted_rows
FROM recruit_posts p
HAVING p.accepted_count <> accepted_rows;
