package app.usfit.api.concurrency;

import static app.usfit.api.concurrency.ConcurrencyReport.fmt;
import static app.usfit.api.concurrency.ConcurrencyReport.ratio;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataAccessException;

import app.usfit.api.RecruitPlayer.dto.ApplicationApplyRequest;
import app.usfit.api.RecruitPlayer.dto.ApplicationStatusRequest;
import app.usfit.api.RecruitPlayer.entity.RecruitApplication;
import app.usfit.api.RecruitPlayer.service.RecruitApplicationService;

/// <summary>
/// 용병 모집 동시성 실험
/// - 비교용 전략(수정 전 코드, 비관적 락, 낙관적 락 등)은 같은 SQL 흐름을 JDBC로 재현
/// - "현재 코드" 전략은 실제 RecruitApplicationService를 그대로 호출
/// </summary>
class RecruitConcurrencyTest extends ConcurrencyTestSupport {

    private static final ApplicationStatusRequest ACCEPT = new ApplicationStatusRequest(RecruitApplication.Status.ACCEPTED);
    private static final int OPTIMISTIC_MAX_RETRY = 3;

    @Autowired
    private RecruitApplicationService service;

    @FunctionalInterface
    private interface AcceptAction {
        void accept(long postId, long applicationId, Tally tally) throws Exception;
    }

    private record Strategy(String name, boolean usesCounter, AcceptAction action) {}

    // =====================================================================
    // 실험 1. 정원 N명 모집글에 서로 다른 신청서 2N건을 동시에 수락
    // =====================================================================
    @Test
    @DisplayName("실험 1: 정원이 있는 수락 - 전략별 정원 초과 비교")
    void seatCapacity() throws Exception {
        int capacity = THREADS / 2;
        long writer = createUser();
        long sport = createSport();
        List<Long> applicants = createUsers(THREADS);

        List<Strategy> strategies = List.of(
                new Strategy("A. 조회 후 검사 (수정 전 코드)", false, (p, a, t) -> legacyCheckThenAct(p, a)),
                new Strategy("B. 비관적 락 (SELECT ... FOR UPDATE)", false, (p, a, t) -> pessimisticLock(p, a)),
                new Strategy("C. 낙관적 락 (accepted_count CAS, 재시도 " + OPTIMISTIC_MAX_RETRY + "회)", true, this::optimisticLock),
                new Strategy("D. 조건부 UPDATE (JDBC 재현)", true, (p, a, t) -> conditionalUpdate(p, a)),
                new Strategy("D. 조건부 UPDATE (실제 서비스 호출)", true, (p, a, t) -> service.changeStatus(p, a, writer, ACCEPT))
        );
        List<Tally> tallies = strategies.stream().map(s -> new Tally(s.name())).toList();

        for (int trial = 0; trial < TRIALS; trial++) {
            // 전략을 trial마다 번갈아 실행해서 시간대에 따른 편차가 한 전략에 몰리지 않게 함
            for (int s = 0; s < strategies.size(); s++) {
                Strategy strategy = strategies.get(s);
                Tally tally = tallies.get(s);

                long post = createPost(writer, sport, capacity);
                List<Long> apps = applicants.stream().map(a -> createApplication(post, a)).toList();

                RunResult r = runConcurrently(THREADS, i -> () -> {
                    strategy.action().accept(post, apps.get(i), tally);
                    return null;
                });

                int accepted = acceptedRows(post);
                tally.add("accepted", accepted);
                if (accepted < capacity) tally.add("underfill", 1);
                if (strategy.usesCounter() && acceptedCounter(post) != accepted) tally.add("counterMismatch", 1);
                tally.record(r, accepted > capacity);
            }
        }

        List<List<String>> rows = new ArrayList<>();
        for (int s = 0; s < strategies.size(); s++) {
            Tally t = tallies.get(s);
            rows.add(List.of(
                    t.name,
                    "**" + ratio(t.violations, t.trials) + "**",
                    ratio((int) t.sum("underfill"), t.trials),
                    strategies.get(s).usesCounter() ? ratio((int) t.sum("counterMismatch"), t.trials) : "-",
                    fmt(t.avg("accepted")) + " / " + capacity,
                    String.valueOf(t.sum("retries")),
                    fmt(t.avgElapsed()),
                    String.valueOf(t.p95Elapsed())
            ));
        }

        new ConcurrencyReport("01-seat-capacity", "실험 1. 정원이 있는 수락: 전략별 비교")
                .line(environment())
                .line("- 시나리오: 정원 " + capacity + "명인 모집글에 서로 다른 WAITING 신청서 " + THREADS
                        + "건을 " + THREADS + "개 스레드가 동시에 수락, 전략마다 " + TRIALS + "회 반복")
                .line("- 정원 초과: 수락된 신청서 수 > 정원 / 정원 미달: 자리가 남았는데 거절됨 / 카운터 불일치: accepted_count ≠ 실제 ACCEPTED 행 수")
                .line("- A~C와 'D (JDBC 재현)'은 같은 SQL 흐름을 JDBC로 실행 → 소요 시간 비교는 이 네 가지끼리만 의미 있음")
                .line("- 'D (실제 서비스 호출)'은 RecruitApplicationService.changeStatus를 그대로 호출 (엔티티 조회·권한 검사·응답 매핑 포함) → 정합성 검증용")
                .section("결과")
                .table(List.of("전략", "정원 초과 trial", "정원 미달 trial", "카운터 불일치", "평균 수락 인원",
                        "재시도 합계", "trial 평균(ms)", "trial p95(ms)"), rows)
                .failureReasons(tallies)
                .write();

        Tally pessimistic = tallies.get(1);
        Tally conditionalJdbc = tallies.get(3);
        Tally current = tallies.get(4);
        assertThat(pessimistic.violations).as("비관적 락 정원 초과").isZero();
        assertThat(conditionalJdbc.violations).as("조건부 UPDATE(JDBC) 정원 초과").isZero();
        assertThat(current.violations).as("조건부 UPDATE 정원 초과").isZero();
        assertThat(current.sum("underfill")).as("조건부 UPDATE 정원 미달").isZero();
        assertThat(current.sum("counterMismatch")).as("조건부 UPDATE 카운터 불일치").isZero();
    }

    /// 수정 전 코드: ACCEPTED 수를 세고 정원과 비교한 뒤 상태 변경 (검사와 변경 사이에 다른 트랜잭션이 끼어들 수 있음)
    private void legacyCheckThenAct(long postId, long applicationId) {
        tx().executeWithoutResult(s -> {
            int max = jdbc.queryForObject("SELECT max_member FROM recruit_posts WHERE post_id = ?", Integer.class, postId);
            if (acceptedRows(postId) >= max) throw new ExpectedRejection("정원 마감");
            jdbc.update("UPDATE recruit_applications SET status = 'ACCEPTED' WHERE application_id = ?", applicationId);
        });
    }

    /// 비관적 락: 모집글 행을 FOR UPDATE로 잠근 뒤 검사 (같은 모집글 수락은 전부 직렬화)
    private void pessimisticLock(long postId, long applicationId) {
        tx().executeWithoutResult(s -> {
            int max = jdbc.queryForObject("SELECT max_member FROM recruit_posts WHERE post_id = ? FOR UPDATE", Integer.class, postId);
            if (acceptedRows(postId) >= max) throw new ExpectedRejection("정원 마감");
            jdbc.update("UPDATE recruit_applications SET status = 'ACCEPTED' WHERE application_id = ?", applicationId);
        });
    }

    /// 낙관적 락: 읽은 accepted_count가 그대로일 때만 +1 (버전 컬럼 역할), 충돌 시 재시도
    private void optimisticLock(long postId, long applicationId, Tally tally) {
        for (int attempt = 0; attempt <= OPTIMISTIC_MAX_RETRY; attempt++) {
            Boolean done = tx().execute(s -> {
                var row = jdbc.queryForMap("SELECT accepted_count, max_member FROM recruit_posts WHERE post_id = ?", postId);
                int count = ((Number) row.get("accepted_count")).intValue();
                int max = ((Number) row.get("max_member")).intValue();
                if (count >= max) throw new ExpectedRejection("정원 마감");
                int updated = jdbc.update(
                        "UPDATE recruit_posts SET accepted_count = ? WHERE post_id = ? AND accepted_count = ?",
                        count + 1, postId, count);
                if (updated == 0) return false;
                jdbc.update("UPDATE recruit_applications SET status = 'ACCEPTED' WHERE application_id = ?", applicationId);
                return true;
            });
            if (Boolean.TRUE.equals(done)) return;
            if (attempt < OPTIMISTIC_MAX_RETRY) tally.add("retries", 1);
        }
        throw new ExpectedRejection("재시도 초과 (낙관적 락 충돌)");
    }

    /// 조건부 UPDATE: 서비스와 같은 순서 (신청서 상태 전이 -> 자리 확보, 실패 시 롤백)
    private void conditionalUpdate(long postId, long applicationId) {
        tx().executeWithoutResult(s -> {
            int transitioned = jdbc.update(
                    "UPDATE recruit_applications SET status = 'ACCEPTED' WHERE application_id = ? AND status = 'WAITING'",
                    applicationId);
            if (transitioned == 0) throw new ExpectedRejection("이미 처리된 신청서");
            int reserved = jdbc.update(
                    "UPDATE recruit_posts SET accepted_count = accepted_count + 1 WHERE post_id = ? AND accepted_count < max_member",
                    postId);
            if (reserved == 0) throw new ExpectedRejection("정원 마감");
        });
    }

    // =====================================================================
    // 실험 2. 같은 신청서 1건을 동시에 수락 (중복 클릭/재전송)
    // =====================================================================
    @Test
    @DisplayName("실험 2: 같은 신청서 동시 수락 - accepted_count 이중 증가")
    void sameApplicationDoubleAccept() throws Exception {
        int capacity = THREADS / 2;
        long writer = createUser();
        long sport = createSport();
        long applicant = createUser();

        List<Strategy> strategies = List.of(
                new Strategy("조건부 UPDATE: 자리만 (1차 수정)", true, (p, a, t) -> seatOnly(p, a)),
                new Strategy("조건부 UPDATE: 자리 + 상태 전이 (현재 코드)", true, (p, a, t) -> service.changeStatus(p, a, writer, ACCEPT))
        );
        List<Tally> tallies = strategies.stream().map(s -> new Tally(s.name())).toList();

        for (int trial = 0; trial < TRIALS; trial++) {
            for (int s = 0; s < strategies.size(); s++) {
                Strategy strategy = strategies.get(s);
                Tally tally = tallies.get(s);

                long post = createPost(writer, sport, capacity);
                long app = createApplication(post, applicant);

                RunResult r = runConcurrently(THREADS, i -> () -> {
                    strategy.action().accept(post, app, tally);
                    return null;
                });

                int counter = acceptedCounter(post);
                tally.add("counter", counter);
                tally.record(r, counter != acceptedRows(post));
            }
        }

        List<List<String>> rows = tallies.stream().map(t -> List.of(
                t.name,
                "**" + ratio(t.violations, t.trials) + "**",
                fmt(t.avg("counter")),
                fmt(t.avgElapsed())
        )).toList();

        new ConcurrencyReport("02-same-application", "실험 2. 같은 신청서 동시 수락")
                .line(environment())
                .line("- 시나리오: WAITING 신청서 1건을 " + THREADS + "개 스레드가 동시에 수락, 전략마다 " + TRIALS + "회 반복")
                .line("- 기대값: 수락 1명, accepted_count = 1")
                .section("결과")
                .table(List.of("전략", "카운터 불일치 trial", "평균 accepted_count", "trial 평균(ms)"), rows)
                .failureReasons(tallies)
                .write();

        assertThat(tallies.get(1).violations).as("상태 전이까지 조건부로 처리하면 카운터 불일치 없음").isZero();
    }

    /// 1차 수정: 자리는 조건부 UPDATE로 확보하지만 신청서 상태는 읽은 값 기준으로 판단
    private void seatOnly(long postId, long applicationId) {
        tx().executeWithoutResult(s -> {
            String status = jdbc.queryForObject(
                    "SELECT status FROM recruit_applications WHERE application_id = ?", String.class, applicationId);
            if ("ACCEPTED".equals(status)) return;
            int updated = jdbc.update(
                    "UPDATE recruit_posts SET accepted_count = accepted_count + 1 WHERE post_id = ? AND accepted_count < max_member",
                    postId);
            if (updated == 0) throw new ExpectedRejection("정원 마감");
            jdbc.update("UPDATE recruit_applications SET status = 'ACCEPTED' WHERE application_id = ?", applicationId);
        });
    }

    // =====================================================================
    // 실험 3. 같은 사용자가 같은 모집글에 동시에 지원
    // =====================================================================
    @Test
    @DisplayName("실험 3: 중복 지원 - exists 검사 vs UNIQUE")
    void duplicateApply() throws Exception {
        long writer = createUser();
        long sport = createSport();
        long applicant = createUser();

        Tally legacy = new Tally("exists 검사 후 INSERT (UNIQUE 없음, 수정 전)");
        Tally current = new Tally("exists 검사 + UNIQUE(post_id, user_id) (현재 코드)");
        List<Long> legacyPosts = new ArrayList<>();

        // 수정 전 상태 재현: UNIQUE 제거 (FK가 유니크 인덱스를 쓰고 있을 수 있어 post_id 단독 인덱스를 먼저 만듦)
        tryExecute("CREATE INDEX idx_ra_post_only ON recruit_applications (post_id)");
        jdbc.execute("ALTER TABLE recruit_applications DROP INDEX uk_recruit_application_post_user");
        try {
            for (int trial = 0; trial < TRIALS; trial++) {
                long post = createPost(writer, sport, THREADS);
                legacyPosts.add(post);
                RunResult r = runConcurrently(THREADS, i -> () -> {
                    legacyApply(post, applicant);
                    return null;
                });
                int count = applicationCount(post, applicant);
                legacy.add("rows", count);
                legacy.record(r, count > 1);
            }
        } finally {
            // 중복 행을 지워야 UNIQUE를 다시 걸 수 있음
            legacyPosts.forEach(p -> jdbc.update("DELETE FROM recruit_applications WHERE post_id = ?", p));
            jdbc.execute("ALTER TABLE recruit_applications ADD CONSTRAINT uk_recruit_application_post_user UNIQUE (post_id, user_id)");
        }

        for (int trial = 0; trial < TRIALS; trial++) {
            long post = createPost(writer, sport, THREADS);
            RunResult r = runConcurrently(THREADS, i -> () ->
                    service.apply(post, applicant, new ApplicationApplyRequest("concurrency")));
            int count = applicationCount(post, applicant);
            current.add("rows", count);
            current.record(r, count != 1);
        }

        List<Tally> tallies = List.of(legacy, current);
        List<List<String>> rows = tallies.stream().map(t -> List.of(
                t.name,
                "**" + ratio(t.violations, t.trials) + "**",
                fmt(t.avg("rows")),
                fmt(t.avgElapsed())
        )).toList();

        new ConcurrencyReport("03-duplicate-apply", "실험 3. 중복 지원")
                .line(environment())
                .line("- 시나리오: 같은 사용자가 같은 모집글에 " + THREADS + "개 스레드로 동시에 지원, " + TRIALS + "회 반복")
                .line("- 기대값: 신청서 1건")
                .section("결과")
                .table(List.of("방식", "중복 생성 trial", "평균 신청서 수", "trial 평균(ms)"), rows)
                .failureReasons(tallies)
                .write();

        assertThat(current.violations).as("UNIQUE 적용 후 중복 지원 없음").isZero();
    }

    /// 수정 전 코드: 존재 여부 검사 후 INSERT
    private void legacyApply(long postId, long applicantId) {
        tx().executeWithoutResult(s -> {
            int exists = applicationCount(postId, applicantId);
            if (exists > 0) throw new ExpectedRejection("이미 지원함");
            jdbc.update("INSERT INTO recruit_applications (post_id, user_id, status) VALUES (?, ?, 'WAITING')",
                    postId, applicantId);
        });
    }

    private int applicationCount(long postId, long applicantId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM recruit_applications WHERE post_id = ? AND user_id = ?",
                Integer.class, postId, applicantId);
    }

    private void tryExecute(String sql) {
        try {
            jdbc.execute(sql);
        } catch (DataAccessException ignored) {
            // 이미 존재하면 무시
        }
    }

    private String environment() {
        return "- 환경: MySQL " + mysqlVersion() + " (Testcontainers), 격리수준 " + isolationLevel()
                + ", 스레드 " + THREADS + ", 반복 " + TRIALS + "회, 커넥션 풀 40";
    }
}
