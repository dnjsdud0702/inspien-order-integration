package com.inspien.integration.order;

import com.inspien.integration.config.Config;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@RequiredArgsConstructor
public class OrderDao {

    private static final String PENDING = "N";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final Config config;


    public synchronized List<Order> insertOrders(List<Order> orders) {
        String sql = "INSERT INTO " + config.getOrderTable() + " (ORDER_ID, APPLICANT_KEY, USER_ID, ITEM_ID, NAME,"
                + " ADDRESS, ITEM_NAME, PRICE, STATUS) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        return transaction.execute(status -> {
            List<Order> saved = new ArrayList<>();
            String id = maxOrderId();
            for (Order order : orders) {
                id = nextId(id);
                saved.add(order.withOrderId(id));
            }
            jdbc.batchUpdate(sql, saved.stream()
                    .map(o -> new Object[] {o.orderId(), config.getApplicantKey(), o.userId(), o.itemId(), o.name(),
                            o.address(), o.itemName(), o.price(), PENDING})
                    .toList());
            return saved;
        });
    }

    String maxOrderId() {
        return jdbc.queryForObject("SELECT MAX(ORDER_ID) FROM " + config.getOrderTable() + " WHERE APPLICANT_KEY = ?",
                String.class, config.getApplicantKey());
    }

    /** 대문자 1자 + 숫자 3자리. A001 ... A999 다음은 B001이다. */
    static String nextId(String current) {
        if (current == null) {
            return "A001";
        }
        char letter = current.charAt(0);
        int number = Integer.parseInt(current.substring(1));
        return number < 999 ? letter + String.format("%03d", number + 1) : (char) (letter + 1) + "001";
    }
}
