package com.inspien.integration.ftp;

import com.inspien.integration.config.Config;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Duration;
import java.util.Set;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;

// 과제 FTP는 덮어쓰기·삭제가 불가능해서, 이미 있는 파일은 다시 올리지 않는다.
public class FtpUploader implements AutoCloseable {

    private final FTPClient client = new FTPClient();
    private final Set<String> existingFiles;

    public FtpUploader(Config config) throws IOException {
        client.setControlEncoding("UTF-8"); // 한글 파일명
        client.setConnectTimeout(10_000);
        client.setDefaultTimeout(30_000);
        client.setDataTimeout(Duration.ofSeconds(30));
        client.connect(config.getFtpHost(), config.getFtpPort());
        if (!client.login(config.getFtpId(), config.getFtpPassword())) {
            throw new IOException("FTP 로그인 실패");
        }
        client.enterLocalPassiveMode();
        client.setFileType(FTP.BINARY_FILE_TYPE);
        if (!client.changeWorkingDirectory(config.getFtpPath())) {
            throw new IOException("FTP 경로 이동 실패");
        }
        String[] names = client.listNames();
        existingFiles = names == null ? Set.of() : Set.of(names);
    }

    // 이미 있는 파일이면 올리지 않고 false를 돌려준다.
    public boolean upload(String fileName, byte[] content) throws IOException {
        if (existingFiles.contains(fileName)) {
            return false;
        }
        if (!client.storeFile(fileName, new ByteArrayInputStream(content))) {
            throw new IOException("FTP 업로드 실패: " + client.getReplyString().strip());
        }
        return true;
    }

    @Override
    public void close() throws IOException {
        if (client.isConnected()) {
            client.disconnect();
        }
    }
}
