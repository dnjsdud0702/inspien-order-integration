package com.inspien.integration.order;

import java.nio.charset.Charset;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private static final Charset EUC_KR = Charset.forName("EUC-KR");

    private final OrderService orderService;

    @PostMapping(value = "/orders", consumes = {"application/xml", "text/xml"})
    public ResponseEntity<Map<String, Object>> receive(@RequestBody byte[] body) throws Exception {
        OrderService.ReceiveResult result = orderService.receive(new String(body, EUC_KR));

        Map<String, Object> response = Map.of(
                "saved", result.saved().stream().map(Order::orderId).toList(),
                "errors", result.errors());
        return ResponseEntity.status(result.saved().isEmpty() ? 400 : 200).body(response);
    }

    // XML 형식이 깨진 경우 
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> badRequest(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
    }
}
