package com.inspien.integration.order;

import com.inspien.integration.common.trace.TraceLog;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderParser parser;
    private final OrderDao dao;
    private final TraceLog traceLog;

    public record ReceiveResult(List<Order> saved, List<String> errors) {
    }

    public ReceiveResult receive(String xml) throws Exception {
        OrderParser.Result result;
        try {
            result = parser.parse(xml);
        } catch (IllegalArgumentException e) {
            traceLog.fail("-", "RECEIVE", e.getMessage());
            throw e;
        }
        result.errors().forEach(error -> traceLog.fail("-", "VALIDATE", error));

        List<Order> saved;
        try {
            saved = result.orders().isEmpty() ? List.of() : dao.insertOrders(result.orders());
        } catch (RuntimeException e) {
            traceLog.fail("-", "ORDER_SAVE", e.getMessage());
            throw e;
        }
        saved.forEach(order -> traceLog.ok(order.orderId(), "ORDER_SAVE", order.userId() + "/" + order.itemId()));

        return new ReceiveResult(saved, result.errors());
    }
}
