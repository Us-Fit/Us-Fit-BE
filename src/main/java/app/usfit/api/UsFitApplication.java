package app.usfit.api;

import app.usfit.api.facility.service.FacilityImportService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

@SpringBootApplication
public class UsFitApplication {
	public static void main(String[] args) {
		SpringApplication.run(UsFitApplication.class, args);
	}

	//facility data가 database에 없을 시 처음 서버 작동에만 주석 해제
	//file이 너무 큰 관계로 임시 dataset 생성 99개의 행만 추출
	/*@Bean
	CommandLineRunner importFacilities(FacilityImportService service) {
		return args -> {
			// CSV 파일 경로 지정 (절대경로 또는 상대경로)
			Path csvPath = Path.of("C:/Users/tpgus/Desktop/2025-2/usfit-bigdata/dataTemp.csv");

			// 인코딩 설정: 파일이 MS949면 StandardCharsets.UTF_8 대신 Charset.forName("MS949")
			int imported = service.importCsv(csvPath, true, StandardCharsets.UTF_8);
			System.out.println("✅ Imported rows = " + imported);
		};
	}*/
}
