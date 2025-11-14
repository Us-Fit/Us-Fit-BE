package app.usfit.api;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@SpringBootApplication
public class UsFitApplication {
	public static void main(String[] args) {
		SpringApplication.run(UsFitApplication.class, args);
	}

	@Bean
    CommandLineRunner printDb(Environment env) {
        return args -> {
            System.out.println(">>> spring.datasource.url = " + env.getProperty("spring.datasource.url"));
            System.out.println(">>> ENV SPRING_DATASOURCE_URL = " + System.getenv("SPRING_DATASOURCE_URL"));
            System.out.println(">>> ENV DB_URL = " + System.getenv("DB_URL"));
            System.out.println(">>> JVM prop spring.datasource.url = " + System.getProperty("spring.datasource.url"));
            System.out.println(">>> Active profiles = " + String.join(",", env.getActiveProfiles()));
        };
    }
	//facility data가 database에 없을 시 처음 서버 작동에만 주석 해제
//	@Bean
//	@Profile("!test")
//	CommandLineRunner importFacilities(FacilityImportService service) {
//		return args -> {
//			// CSV 파일 경로 지정 (절대경로 또는 상대경로)
//			Path csvPath = Path.of("C:/Users/tpgus/OneDrive/usfit-bigdata/data.csv");
//
//			// 인코딩 설정: 파일이 MS949면 StandardCharsets.UTF_8 대신 Charset.forName("MS949")
//			int imported = service.importCsv(csvPath, true, StandardCharsets.UTF_8);
//			System.out.println("✅ Imported rows = " + imported);
//		};
//	}
}
