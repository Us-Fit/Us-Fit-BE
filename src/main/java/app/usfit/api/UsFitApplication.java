package app.usfit.api;

import app.usfit.api.course.service.CourseImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

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
    // 서버 처음 실행할 때만 주석 해제해서 import 실행
//    @Bean
//    @Profile("!test")
//    CommandLineRunner importCourses(CourseImportService service) {
//        return args -> {
//            try{
//                // 로컬 CSV 파일 경로
//                Path csvPath = Path.of("C:/Users/tpgus/OneDrive/usfit-bigdata/svch/course_data.csv");
//
//                // CSV 인코딩 (UTF-8 또는 MS949)
////            Charset charset = StandardCharsets.UTF_8;
//
//                int imported = service.importCsv(csvPath, true, StandardCharsets.UTF_8);
//
//                System.out.println("======================================");
//                System.out.println("  📘 Course Data Import Completed");
//                System.out.println("  → Imported rows: " + imported);
//                System.out.println("======================================");
//            } catch(Exception e){
//                System.out.println("❌ Course import 중 예외 발생!");
//                e.printStackTrace();  // 실제 원인 다 찍힘
//            }
//        };
//    }
}
