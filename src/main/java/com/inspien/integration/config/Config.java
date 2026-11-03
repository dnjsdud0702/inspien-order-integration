package com.inspien.integration.config;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;



/** 채용 API 응답(response.json)을 휴대폰 번호(phone.txt)로 복호화해 접속 정보를 만든다. */
@Getter
public final class Config {

    private final String applicantKey;
    private final String applicantName;
    private final String dbUrl;
    private final String dbId;
    private final String dbPassword;
    private final String orderTable;
    private final String shipmentTable;
    private final String ftpHost;
    private final int ftpPort;
    private final String ftpId;
    private final String ftpPassword;
    private final String ftpPath;

    private Config(JsonNode response, String phone, String applicantName) throws Exception {
        JsonNode order = response.get("ORDER_TB_CONN");
        JsonNode shipment = response.get("SHIPMENT_TB_CONN");
        JsonNode ftp = response.get("FTP_CONN");

        this.applicantKey = response.get("APPLICANT_KEY").asText();
        this.applicantName = applicantName;
        // 주문과 운송 테이블은 같은 DB, 같은 계정으로 제공되어 접속 정보를 하나만 사용한다.
        this.dbUrl = decrypt(order.get("URL").asText(), phone);
        this.dbId = decrypt(order.get("ID").asText(), phone);
        this.dbPassword = decrypt(order.get("PASSWORD").asText(), phone);
        this.orderTable = decrypt(order.get("TABLE").asText(), phone);
        this.shipmentTable = decrypt(shipment.get("TABLE").asText(), phone);
        this.ftpHost = decrypt(ftp.get("URL").asText(), phone);
        this.ftpPort = Integer.parseInt(decrypt(ftp.get("PORT").asText(), phone));
        this.ftpId = decrypt(ftp.get("ID").asText(), phone);
        this.ftpPassword = decrypt(ftp.get("PASSWORD").asText(), phone);
        this.ftpPath = decrypt(ftp.get("PATH").asText(), phone);
    }

    public static Config load(Path secretDir, String applicantName) throws Exception {
        String phone = Files.readString(secretDir.resolve("phone.txt")).strip();
        JsonNode response = new ObjectMapper().readTree(Files.readAllBytes(secretDir.resolve("response.json")));
        return new Config(response, phone, applicantName);
    }

    // public String getApplicantKey() {
    //     return applicantKey;
    // }

    // public String getOrderTable() {
    //     return orderTable;
    // }

    // public String getApplicantName() {
    //     return applicantName;
    // }

    // public String getFtpHost() {
    //     return ftpHost;
    // }

    // public int getFtpPort() {
    //     return ftpPort;
    // }

    // public String getFtpId() {
    //     return ftpId;
    // }

    // public String getFtpPassword() {
    //     return ftpPassword;
    // }

    // public String getFtpPath() {
    //     return ftpPath;
    // }

    /** 키: 휴대폰 번호 UTF-8 바이트 -> SHA-1 -> 앞 16바이트. 알고리즘: AES-128/ECB/PKCS5Padding. */
    static String decrypt(String base64, String phone) throws Exception {
        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
        cipher.init(Cipher.DECRYPT_MODE, secretKey(phone));
        return new String(cipher.doFinal(Base64.getDecoder().decode(base64)), StandardCharsets.UTF_8);
    }

    static SecretKeySpec secretKey(String phone) throws Exception {
        byte[] hash = MessageDigest.getInstance("SHA-1").digest(phone.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(Arrays.copyOf(hash, 16), "AES");
    }
}
