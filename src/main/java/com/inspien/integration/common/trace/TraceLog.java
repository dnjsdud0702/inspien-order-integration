package com.inspien.integration.trace;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 처리이력 파일로 남김 -> 조회: grep A002 logs/trace.log
@Component
public class TraceLog {

    private final Path file;

    public TraceLog(@Value("${trace.file}") String file) {
        this.file = Path.of(file);
    }

    public void ok(String target, String step, String detail) {
        write(target, step, "OK", detail);
    }

    public void fail(String target, String step, String detail) {
        write(target, step, "FAIL", detail);
    }

    // 이력 기록이 실패해도 주문 처리는 계속되어야 하므로 예외를 던지지 않는다.
    private synchronized void write(String target, String step, String result, String detail) {
        String line = String.join(" | ", LocalDateTime.now().toString(), target, step, result, detail);
        System.out.println(line);
        try {
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            Files.writeString(file, line + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            System.err.println("이력 기록 실패: " + e.getMessage());
        }
    }
}
