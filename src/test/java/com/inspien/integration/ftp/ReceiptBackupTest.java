package com.inspien.integration.ftp;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ReceiptBackupTest {

    @TempDir
    Path dir;

    @Test
    void 영수증을_로컬_폴더에_저장한다() throws Exception {
        ReceiptBackup backup = new ReceiptBackup(dir.resolve("receipts").toString());
        byte[] content = "주문번호: A001\n".getBytes(StandardCharsets.UTF_8);

        Path saved = backup.save("주원영_A001.txt", content);

        assertThat(saved).hasFileName("주원영_A001.txt");
        assertThat(Files.readAllBytes(saved)).isEqualTo(content);
    }

    @Test
    void 같은_파일을_다시_저장하면_덮어쓴다() throws Exception {
        ReceiptBackup backup = new ReceiptBackup(dir.toString());
        backup.save("주원영_A001.txt", "old".getBytes(StandardCharsets.UTF_8));

        Path saved = backup.save("주원영_A001.txt", "new".getBytes(StandardCharsets.UTF_8));

        assertThat(Files.readString(saved)).isEqualTo("new");
    }
}
