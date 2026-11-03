package com.inspien.integration.common.trace;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TraceLogTest {

    @TempDir
    Path dir;

    @Test
    void 처리_결과를_한_줄씩_이어서_기록한다() throws Exception {
        Path file = dir.resolve("logs/trace.log");
        TraceLog traceLog = new TraceLog(file.toString());

        traceLog.ok("A002", "ORDER_SAVE", "USER01/ITEM01");
        traceLog.fail("-", "VALIDATE", "U9/I1: HEADER 없음");

        List<String> lines = Files.readAllLines(file);
        assertThat(lines).hasSize(2);
        assertThat(lines.get(0)).endsWith("| A002 | ORDER_SAVE | OK | USER01/ITEM01");
        assertThat(lines.get(1)).endsWith("| - | VALIDATE | FAIL | U9/I1: HEADER 없음");
    }
}
