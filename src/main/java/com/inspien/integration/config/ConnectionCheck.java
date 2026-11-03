package com.inspien.integration.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import org.apache.commons.net.ftp.FTPClient;

/** 복호화한 접속 정보로 DB와 FTP에 실제 접속되는지 확인한다. 데이터는 읽기만 한다. */
public final class ConnectionCheck {

    private ConnectionCheck() {
    }

    public static void run(Config config) {
        checkDb(config);
        checkFtp(config);
    }

    private static void checkDb(Config config) {
        try (Connection connection = DriverManager.getConnection(config.getDbUrl(), config.getDbId(), config.getDbPassword())) {
            System.out.println("[DB] 접속 성공");
            for (String table : new String[] {config.getOrderTable(), config.getShipmentTable()}) {
                String sql = "SELECT COUNT(*) FROM " + table + " WHERE APPLICANT_KEY = ?";
                try (PreparedStatement statement = connection.prepareStatement(sql)) {
                    statement.setString(1, config.getApplicantKey());
                    try (ResultSet rs = statement.executeQuery()) {
                        rs.next();
                        System.out.println("[DB] " + table + " 내 데이터 " + rs.getInt(1) + "건");
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("[DB] 실패: " + e.getMessage());
        }
    }

    private static void checkFtp(Config config) {
        FTPClient ftp = new FTPClient();
        ftp.setControlEncoding("UTF-8"); // 한글 파일명
        try {
            ftp.setConnectTimeout(10_000);
            ftp.connect(config.getFtpHost(), config.getFtpPort());
            if (!ftp.login(config.getFtpId(), config.getFtpPassword())) {
                System.out.println("[FTP] 로그인 실패");
                return;
            }
            ftp.enterLocalPassiveMode();
            if (!ftp.changeWorkingDirectory(config.getFtpPath())) {
                System.out.println("[FTP] 업로드 경로 이동 실패");
                return;
            }
            String[] names = ftp.listNames();
            long mine = names == null ? 0
                    : java.util.Arrays.stream(names).filter(name -> name.startsWith(config.getApplicantName())).count();
            System.out.println("[FTP] 접속 성공, 내 파일 " + mine + "개");
        } catch (Exception e) {
            System.out.println("[FTP] 실패: " + e.getMessage());
        } finally {
            try {
                if (ftp.isConnected()) {
                    ftp.disconnect();
                }
            } catch (Exception ignored) {
                // 확인용 연결이라 종료 실패는 무시한다
            }
        }
    }
}
