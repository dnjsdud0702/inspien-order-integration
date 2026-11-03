package com.inspien.integration.ftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// FTP 다운로드 불가 -> 업로드한 영수증을 로컬에도 저장
@Component
public class ReceiptBackup {

    private final Path dir;

    public ReceiptBackup(@Value("${receipt.backup-dir}") String dir) {
        this.dir = Path.of(dir);
    }

    // 재처리 시 내용이 같으므로 덮어쓰기
    public Path save(String fileName, byte[] content) throws IOException {
        Files.createDirectories(dir);
        return Files.write(dir.resolve(fileName), content);
    }
}
