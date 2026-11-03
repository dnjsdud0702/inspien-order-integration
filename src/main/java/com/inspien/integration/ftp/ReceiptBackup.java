package com.inspien.integration.ftp;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

// 과제 FTP는 다운로드가 불가능해서, 업로드한 영수증과 같은 내용을 로컬에도 남긴다.
@Component
public class ReceiptBackup {

    private final Path dir;

    public ReceiptBackup(@Value("${receipt.backup-dir}") String dir) {
        this.dir = Path.of(dir);
    }

    // 재처리 시 같은 파일을 다시 써도 내용이 같으므로 덮어쓴다.
    public Path save(String fileName, byte[] content) throws IOException {
        Files.createDirectories(dir);
        return Files.write(dir.resolve(fileName), content);
    }
}
