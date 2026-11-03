package com.inspien.integration.config;

import java.nio.file.Path;
import javax.sql.DataSource;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AppConfig {

    @Bean
    public Config config() throws Exception {
        String secretDir = System.getenv().getOrDefault("SECRET_DIR", "secret");
        String applicantName = System.getenv().getOrDefault("APPLICANT_NAME", "주원영");
        return Config.load(Path.of(secretDir), applicantName);
    }

    // DB 접속 설정
    @Bean
    public DataSource dataSource(Config config) {
        return DataSourceBuilder.create()
                .url(config.getDbUrl())
                .username(config.getDbId())
                .password(config.getDbPassword())
                .build();
    }

    @Bean
    public CommandLineRunner connectionCheck(Config config) {
        return args -> ConnectionCheck.run(config);
    }
}
