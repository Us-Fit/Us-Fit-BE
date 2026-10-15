package app.usfit.api.concurrency;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntFunction;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import org.testcontainers.containers.MySQLContainer;

/// <summary>
/// 동시성 실험 공통 기반
/// - 실제 MySQL(Testcontainers)에서 실행: H2 등 인메모리 DB는 락/격리수준 동작이 달라 결과를 신뢰할 수 없음
/// - N개 스레드를 CountDownLatch로 묶어 같은 순간에 출발시킴
/// - 실행: ./gradlew concurrencyTest  (Docker 실행 중이어야 함)
/// </summary>
@SpringBootTest
@ActiveProfiles("concurrency")
@Tag("concurrency")
abstract class ConcurrencyTestSupport {

    // 테스트 클래스 간 컨테이너/스프링 컨텍스트 공유 (싱글톤 컨테이너, 종료는 Testcontainers Ryuk가 처리)
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0.36")
            .withDatabaseName("usfit")
            .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci");

    static {
        MYSQL.start();
    }

    @DynamicPropertySource
    static void datasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    protected static final int THREADS = Integer.getInteger("concurrency.threads", 20);
    protected static final int TRIALS = Integer.getInteger("concurrency.trials", 30);

    @Autowired
    protected JdbcTemplate jdbc;

    @Autowired
    private PlatformTransactionManager transactionManager;

    protected TransactionTemplate tx() {
        return new TransactionTemplate(transactionManager);
    }

    // ===== 동시 실행 =====

    /// threads개의 작업을 동시에 출발시키고 결과를 집계
    protected RunResult runConcurrently(int threads, IntFunction<Callable<?>> taskFactory) throws InterruptedException {
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch ready = new CountDownLatch(threads);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threads);
        AtomicInteger success = new AtomicInteger();
        Map<String, AtomicInteger> failures = new ConcurrentHashMap<>();

        for (int i = 0; i < threads; i++) {
            Callable<?> task = taskFactory.apply(i);
            pool.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    task.call();
                    success.incrementAndGet();
                } catch (Exception e) {
                    failures.computeIfAbsent(reason(e), k -> new AtomicInteger()).incrementAndGet();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();
        long begin = System.nanoTime();
        start.countDown();
        boolean finished = done.await(60, TimeUnit.SECONDS);
        long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - begin);
        pool.shutdownNow();
        if (!finished) {
            throw new IllegalStateException("동시 실행이 60초 안에 끝나지 않음 (데드락 의심)");
        }

        Map<String, Integer> failureCounts = new TreeMap<>();
        failures.forEach((k, v) -> failureCounts.put(k, v.get()));
        return new RunResult(success.get(), failureCounts, elapsedMs);
    }

    private static String reason(Throwable e) {
        if (e instanceof ResponseStatusException rse) {
            return rse.getStatusCode().value() + " " + rse.getReason();
        }
        if (e instanceof ExpectedRejection er) {
            return er.getMessage();
        }
        Throwable root = e;
        while (root.getCause() != null) root = root.getCause();
        return root.getClass().getSimpleName();
    }

    protected record RunResult(int success, Map<String, Integer> failures, long elapsedMs) {}

    /// 측정용 전략 구현에서 "정상적인 거절"을 표현 (정원 마감, 중복 등)
    protected static class ExpectedRejection extends RuntimeException {
        ExpectedRejection(String message) { super(message, null, false, false); }
    }

    // ===== 원본 로그 (집계 전 trial별 측정값, 리포트 수치의 근거) =====

    private static final Set<String> RAW_STARTED = ConcurrentHashMap.newKeySet();

    /// trial 1회 측정값을 콘솔에 한 줄로 출력하고 raw/{experiment}.csv에 누적 (kv: 이름, 값 순서로 나열)
    protected void logTrial(String experiment, String strategy, int trial, RunResult r, Object... kv) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("strategy", strategy);
        fields.put("trial", trial + 1);
        fields.put("success", r.success());
        fields.put("failed", r.failures().values().stream().mapToInt(Integer::intValue).sum());
        fields.put("elapsedMs", r.elapsedMs());
        for (int i = 0; i + 1 < kv.length; i += 2) fields.put(String.valueOf(kv[i]), kv[i + 1]);

        StringBuilder line = new StringBuilder("[RAW] ").append(experiment);
        fields.forEach((k, v) -> line.append(" | ").append(k).append('=').append(v));
        System.out.println(line);

        try {
            Path csv = Path.of(System.getProperty("concurrency.reportDir", "build/reports/concurrency"), "raw", experiment + ".csv");
            Files.createDirectories(csv.getParent());
            if (RAW_STARTED.add(experiment)) {
                Files.writeString(csv, String.join(",", fields.keySet()) + "\n");
            }
            String row = fields.values().stream().map(v -> "\"" + String.valueOf(v).replace("\"", "\"\"") + "\"")
                    .collect(Collectors.joining(","));
            Files.writeString(csv, row + "\n", StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /// 실행 직후 DB 상태를 SQL로 직접 조회해 결과를 그대로 출력 (집계 코드를 거치지 않은 증거)
    protected void dumpQuery(String label, String sql, Object... args) {
        List<Map<String, Object>> rows = jdbc.queryForList(sql, args);
        StringBuilder sb = new StringBuilder("\n[DB] ").append(label).append('\n')
                .append("  SQL   : ").append(sql.replaceAll("\\s+", " ").trim()).append('\n')
                .append("  params: ").append(Arrays.toString(args)).append('\n');
        if (rows.isEmpty()) {
            sb.append("  (0 rows)\n");
        } else {
            sb.append("  | ").append(String.join(" | ", rows.get(0).keySet())).append(" |\n");
            for (Map<String, Object> row : rows) {
                sb.append("  | ").append(row.values().stream().map(String::valueOf).collect(Collectors.joining(" | "))).append(" |\n");
            }
            sb.append("  (").append(rows.size()).append(" rows)\n");
        }
        System.out.println(sb);
    }

    // ===== 실험 집계 =====

    /// 한 전략의 trial 결과 누적
    protected static class Tally {
        final String name;
        int trials;
        int violations;
        final List<Long> elapsed = new ArrayList<>();
        // 작업 스레드에서 동시에 증가시키므로 ConcurrentHashMap
        final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
        final Map<String, Integer> failureReasons = new TreeMap<>();

        Tally(String name) { this.name = name; }

        void record(RunResult r, boolean violated) {
            trials++;
            if (violated) violations++;
            elapsed.add(r.elapsedMs());
            r.failures().forEach((k, v) -> failureReasons.merge(k, v, Integer::sum));
        }

        void add(String counter, long value) {
            counters.computeIfAbsent(counter, k -> new AtomicLong()).addAndGet(value);
        }

        long sum(String counter) {
            AtomicLong v = counters.get(counter);
            return v == null ? 0 : v.get();
        }

        double avg(String counter) {
            return trials == 0 ? 0 : (double) sum(counter) / trials;
        }

        double avgElapsed() {
            return elapsed.stream().mapToLong(Long::longValue).average().orElse(0);
        }

        long p95Elapsed() {
            if (elapsed.isEmpty()) return 0;
            List<Long> sorted = elapsed.stream().sorted().toList();
            return sorted.get((int) Math.ceil(sorted.size() * 0.95) - 1);
        }
    }

    // ===== 테스트 데이터 (JDBC로 직접 삽입: 측정 대상 코드와 무관한 준비 단계) =====

    protected long insert(String sql, Object... args) {
        var keyHolder = new GeneratedKeyHolder();
        jdbc.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        }, keyHolder);
        return keyHolder.getKey().longValue();
    }

    private static final AtomicLong SEQ = new AtomicLong();

    /// 프로필까지 있는 사용자 (서비스 응답 매핑 시 프로필 조회가 필요함)
    protected long createUser() {
        long n = SEQ.incrementAndGet();
        long userId = insert("INSERT INTO users (email, provider) VALUES (?, 'LOCAL')", "user" + n + "@test.local");
        jdbc.update("INSERT INTO user_profile (user_id, nickname, gender) VALUES (?, ?, ?)", userId, "user" + n, true);
        return userId;
    }

    protected List<Long> createUsers(int count) {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < count; i++) ids.add(createUser());
        return ids;
    }

    protected long createSport() {
        long n = SEQ.incrementAndGet();
        return insert("INSERT INTO sports (code, name) VALUES (?, ?)", "S" + n, "sport" + n);
    }

    protected long createPost(long writerId, long sportId, int maxMember) {
        Timestamp future = Timestamp.valueOf(LocalDateTime.now().plusDays(7));
        return insert("""
                INSERT INTO recruit_posts
                    (writer_user_id, sport_type_id, title, facility_id, recruit_deadline, activity_start_time,
                     activity_duration_minutes, max_member, accepted_count, is_active)
                VALUES (?, ?, 'concurrency', 1, ?, ?, 60, ?, 0, true)
                """, writerId, sportId, future, future, maxMember);
    }

    protected long createApplication(long postId, long applicantId) {
        return insert("INSERT INTO recruit_applications (post_id, user_id, status) VALUES (?, ?, 'WAITING')",
                postId, applicantId);
    }

    protected int acceptedRows(long postId) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM recruit_applications WHERE post_id = ? AND status = 'ACCEPTED'",
                Integer.class, postId);
    }

    protected int acceptedCounter(long postId) {
        return jdbc.queryForObject("SELECT accepted_count FROM recruit_posts WHERE post_id = ?", Integer.class, postId);
    }

    protected String mysqlVersion() {
        return jdbc.queryForObject("SELECT VERSION()", String.class);
    }

    protected String isolationLevel() {
        return jdbc.queryForObject("SELECT @@transaction_isolation", String.class);
    }
}
