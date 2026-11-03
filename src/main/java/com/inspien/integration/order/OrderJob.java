package com.inspien.integration.order;

import com.inspien.integration.common.trace.TraceLog;
import com.inspien.integration.config.Config;
import com.inspien.integration.ftp.FtpUploader;
import com.inspien.integration.shipment.ShipmentService;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

// STATUS가 N인 주문을 주기적으로 처리 -> 실패한 주문은 N으로 남아 다음 주기에 다시 처리
@Component
@RequiredArgsConstructor
public class OrderJob {

    private final Config config;
    private final OrderDao dao;
    private final ShipmentService shipmentService;
    private final TraceLog traceLog;

    @Scheduled(fixedDelayString = "${job.interval-ms}")
    public void run() {
        try {
            List<Order> orders = dao.findPendingOrders();
            if (orders.isEmpty()) {
                return;
            }
            try (FtpUploader ftp = new FtpUploader(config)) {
                orders.forEach(order -> process(order, ftp));
            }
        } catch (Exception e) {
            traceLog.fail("-", "JOB", e.getMessage());
        }
    }

    // 영수증 업로드 → 운송 입력 + Y 변경 순서로 처리한다. 영수증이 실패하면 운송 입력도 하지 않는다.
    private void process(Order order, FtpUploader ftp) {
        String step = "RECEIPT";
        try {
            String fileName = config.getApplicantName() + "_" + order.orderId() + ".txt";
            boolean uploaded = ftp.upload(fileName, receipt(order));
            traceLog.ok(order.orderId(), step, uploaded ? fileName : fileName + " (이미 업로드됨)");

            step = "SHIPMENT";
            traceLog.ok(order.orderId(), step, shipmentService.ship(order));
        } catch (Exception e) {
            traceLog.fail(order.orderId(), step, e.getMessage());
        }
    }

    private byte[] receipt(Order order) {
        String text = String.join("\n",
                "주문번호: " + order.orderId(),
                "고객ID: " + order.userId(),
                "고객명: " + order.name(),
                "주소: " + order.address(),
                "상품ID: " + order.itemId(),
                "상품명: " + order.itemName(),
                "금액: " + order.price(),
                "");
        return text.getBytes(StandardCharsets.UTF_8);
    }
}
