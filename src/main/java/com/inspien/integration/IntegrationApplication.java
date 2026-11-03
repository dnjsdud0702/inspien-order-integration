package com.inspien.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.inspien.integration.config.Config;
import java.nio.file.Path;


@SpringBootApplication
public class IntegrationApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(IntegrationApplication.class, args);

		String secretDir = System.getenv()
				.getOrDefault("SECRET_DIR", "secret");

		String applicantName = System.getenv()
				.getOrDefault("APPLICANT_NAME", "주원영");

		Config config = Config.load(
				Path.of(secretDir),
				applicantName
		);

		System.out.println("접속 정보 로딩 완료");
		System.out.println("지원자 키: " + config.getApplicantKey());
	}
}

