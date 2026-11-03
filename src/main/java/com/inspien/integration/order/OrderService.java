package com.inspien.integration.order;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderParser parser;
    private final OrderDao dao;

    public record ReceiveResult(List<Order> saved, List<String> errors) {
    }

    public ReceiveResult receive(String xml) throws Exception {
        OrderParser.Result result = parser.parse(xml);

        List<Order> saved = result.orders().isEmpty() ? List.of() : dao.insertOrders(result.orders());

        return new ReceiveResult(saved, result.errors());
    }
}
