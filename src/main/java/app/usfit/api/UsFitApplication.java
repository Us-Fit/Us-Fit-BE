package app.usfit.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import app.usfit.api.user.service.UserService;

@SpringBootApplication
public class UsFitApplication implements CommandLineRunner {
	
	@Autowired
	private UserService userService;
	
	public static void main(String[] args) {
		SpringApplication.run(UsFitApplication.class, args);
	}

	@Override
	public void run(String... args) throws Exception {
		// 애플리케이션 시작 시 User 테이블의 데이터 개수를 로그로 출력
		long userCount = userService.countUsers();
		System.out.println("===========================================");
		System.out.println("📊 DATABASE CONNECTION TEST");
		System.out.println("👥 Current User count in database: " + userCount);
		System.out.println("===========================================");
	}
}
