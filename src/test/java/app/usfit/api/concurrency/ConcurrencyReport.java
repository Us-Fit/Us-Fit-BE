package app.usfit.api.concurrency;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/// <summary>
/// 실험 결과를 마크다운으로 기록 (build/reports/concurrency/{name}.md + 콘솔 출력)
/// </summary>
class ConcurrencyReport {

    private final String name;
    private final StringBuilder sb = new StringBuilder();

    ConcurrencyReport(String name, String title) {
        this.name = name;
        sb.append("# ").append(title).append("\n\n")
          .append("- 실행 시각: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append('\n');
    }

    ConcurrencyReport line(String text) {
        sb.append(text).append('\n');
        return this;
    }

    ConcurrencyReport section(String title) {
        sb.append("\n## ").append(title).append("\n\n");
        return this;
    }

    ConcurrencyReport table(List<String> header, List<List<String>> rows) {
        sb.append("| ").append(String.join(" | ", header)).append(" |\n");
        sb.append("|").append("---|".repeat(header.size())).append('\n');
        for (List<String> row : rows) {
            sb.append("| ").append(String.join(" | ", row)).append(" |\n");
        }
        sb.append('\n');
        return this;
    }

    /// 전략별 실패(거절) 사유 분포
    ConcurrencyReport failureReasons(List<ConcurrencyTestSupport.Tally> tallies) {
        List<List<String>> rows = new ArrayList<>();
        for (var t : tallies) {
            if (t.failureReasons.isEmpty()) {
                rows.add(List.of(t.name, "-", "0"));
            }
            t.failureReasons.forEach((reason, count) -> rows.add(List.of(t.name, reason, String.valueOf(count))));
        }
        sb.append("<details><summary>요청 실패 사유 (전체 trial 합계)</summary>\n\n");
        table(List.of("전략", "사유", "건수"), rows);
        sb.append("</details>\n");
        return this;
    }

    void write() {
        String dir = System.getProperty("concurrency.reportDir", "build/reports/concurrency");
        try {
            Path path = Path.of(dir, name + ".md");
            Files.createDirectories(path.getParent());
            Files.writeString(path, sb.toString());
            System.out.println("\n" + sb + "\n>>> report: " + path.toAbsolutePath());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static String ratio(int part, int whole) {
        return part + "/" + whole;
    }

    static String fmt(double v) {
        return String.format("%.1f", v);
    }
}
