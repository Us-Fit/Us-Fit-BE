package app.usfit.api.concurrency;

import static app.usfit.api.concurrency.ConcurrencyReport.fmt;
import static app.usfit.api.concurrency.ConcurrencyReport.ratio;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import app.usfit.api.common.enums.AuthProviderEnum;
import app.usfit.api.user.entity.User;
import app.usfit.api.user.service.login.SocialUserService;

/// <summary>
/// 첫 소셜 로그인 동시성 실험 (같은 카카오 계정으로 첫 로그인 요청이 동시에 들어오는 경우)
/// </summary>
class SocialLoginConcurrencyTest extends ConcurrencyTestSupport {

    @Autowired
    private SocialUserService socialUserService;

    @Test
    @DisplayName("실험 4: 첫 로그인 중복 계정 - 조회 후 저장 vs UNIQUE + 재조회")
    void firstLoginDuplicateAccount() throws Exception {
        Tally legacy = new Tally("조회 후 저장 (UNIQUE 없음, 수정 전)");
        Tally current = new Tally("UNIQUE(provider, provider_id) + 위반 시 재조회 (현재 코드)");
        List<String> legacyIds = new ArrayList<>();

        // 수정 전 상태 재현: UNIQUE 제거
        jdbc.execute("ALTER TABLE users DROP INDEX uk_users_provider_provider_id");
        try {
            for (int trial = 0; trial < TRIALS; trial++) {
                String providerId = "legacy-" + trial + "-" + System.nanoTime();
                legacyIds.add(providerId);
                RunResult r = runConcurrently(THREADS, i -> () -> {
                    legacyFindOrCreate(providerId);
                    return null;
                });
                int count = userCount(providerId);
                legacy.add("rows", count);
                legacy.record(r, count > 1);
                logTrial("04-social-first-login", legacy.name, trial, r, "accounts", count, "duplicated", count > 1);
                if (trial == 0) {
                    dumpQuery("실험 4 / " + legacy.name + " / trial 1 직후 계정",
                            "SELECT id, provider, provider_id FROM users WHERE provider = 'KAKAO' AND provider_id = ?", providerId);
                }
            }
        } finally {
            // 중복 행을 지워야 UNIQUE를 다시 걸 수 있음
            legacyIds.forEach(id -> jdbc.update("DELETE FROM users WHERE provider = 'KAKAO' AND provider_id = ?", id));
            jdbc.execute("ALTER TABLE users ADD CONSTRAINT uk_users_provider_provider_id UNIQUE (provider, provider_id)");
        }

        int differentUserTrials = 0;
        for (int trial = 0; trial < TRIALS; trial++) {
            String providerId = "current-" + trial + "-" + System.nanoTime();
            Set<Long> returnedIds = ConcurrentHashMap.newKeySet();
            RunResult r = runConcurrently(THREADS, i -> () -> {
                User user = socialUserService.findOrCreate(AuthProviderEnum.KAKAO, providerId, () -> User.builder()
                        .email("kakao_" + providerId + "@placeholder.local")
                        .provider(AuthProviderEnum.KAKAO)
                        .providerId(providerId)
                        .build());
                returnedIds.add(user.getId());
                return null;
            });
            int count = userCount(providerId);
            current.add("rows", count);
            // 계정이 1개여도 일부 요청이 실패(로그인 에러)했다면 실패로 봄
            current.record(r, count != 1 || !r.failures().isEmpty());
            if (returnedIds.size() != 1) differentUserTrials++;
            logTrial("04-social-first-login", current.name, trial, r,
                    "accounts", count, "distinctUserIdsReturned", returnedIds.size(), "duplicated", count > 1);
            if (trial == 0) {
                dumpQuery("실험 4 / " + current.name + " / trial 1 직후 계정",
                        "SELECT id, provider, provider_id FROM users WHERE provider = 'KAKAO' AND provider_id = ?", providerId);
            }
        }

        List<Tally> tallies = List.of(legacy, current);
        List<List<String>> rows = tallies.stream().map(t -> List.of(
                t.name,
                "**" + ratio(t.violations, t.trials) + "**",
                fmt(t.avg("rows")),
                fmt(t.avgElapsed())
        )).toList();

        new ConcurrencyReport("04-social-first-login", "실험 4. 첫 소셜 로그인 중복 계정")
                .line("- 환경: MySQL " + mysqlVersion() + " (Testcontainers), 격리수준 " + isolationLevel()
                        + ", 스레드 " + THREADS + ", 반복 " + TRIALS + "회")
                .line("- 시나리오: 같은 (KAKAO, providerId)로 " + THREADS + "개 요청이 동시에 첫 로그인")
                .line("- 기대값: 계정 1개, 모든 요청이 같은 사용자로 로그인 성공")
                .section("결과")
                .table(List.of("방식", "실패 trial (중복 계정 또는 로그인 에러)", "평균 계정 수", "trial 평균(ms)"), rows)
                .failureReasons(tallies)
                .write();

        assertThat(current.violations).as("UNIQUE + 재조회 후 중복 계정/로그인 실패 없음").isZero();
        assertThat(differentUserTrials).as("모든 요청이 같은 사용자 ID를 받음").isZero();
    }

    /// 수정 전 코드: findByProviderAndProviderId -> 없으면 save (각각 별도 트랜잭션, 핸들러에 트랜잭션 없음)
    private void legacyFindOrCreate(String providerId) {
        if (userCount(providerId) > 0) return;
        jdbc.update("INSERT INTO users (email, provider, provider_id) VALUES (?, 'KAKAO', ?)",
                "kakao_" + providerId + "@placeholder.local", providerId);
    }

    private int userCount(String providerId) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE provider = 'KAKAO' AND provider_id = ?",
                Integer.class, providerId);
    }
}
