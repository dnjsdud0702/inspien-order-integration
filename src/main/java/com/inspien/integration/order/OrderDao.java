package com.inspien.integration.order;

import com.inspien.integration.common.IdGenerator;
import com.inspien.integration.config.Config;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Repository
@RequiredArgsConstructor
public class OrderDao {

    // ORDER_TB 체크 제약조건: STATUS는 N, Y만 허용
    private static final String PENDING = "N";
    private static final String SHIPPED = "Y";

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
                id = IdGenerator.next(id);
                saved.add(order.withOrderId(id));
            }
            jdbc.batchUpdate(sql, saved.stream()
                    .map(o -> new Object[] {o.orderId(), config.getApplicantKey(), o.userId(), o.itemId(), o.name(),
                            o.address(), o.itemName(), o.price(), PENDING})
                    .toList());
            return saved;
        });
    }

    public List<Order> findPendingOrders() {
        String sql = "SELECT ORDER_ID, USER_ID, ITEM_ID, NAME, ADDRESS, ITEM_NAME, PRICE FROM " + config.getOrderTable()
                + " WHERE APPLICANT_KEY = ? AND STATUS = ? ORDER BY ORDER_ID";
        return jdbc.query(sql, (rs, rowNum) -> new Order(rs.getString("ORDER_ID"), rs.getString("USER_ID"),
                rs.getString("ITEM_ID"), rs.getString("NAME"), rs.getString("ADDRESS"), rs.getString("ITEM_NAME"),
                rs.getString("PRICE")), config.getApplicantKey(), PENDING);
    }

    public void markShipped(String orderId) {
        jdbc.update("UPDATE " + config.getOrderTable() + " SET STATUS = ? WHERE ORDER_ID = ? AND APPLICANT_KEY = ?",
                SHIPPED, orderId, config.getApplicantKey());
    }

    String maxOrderId() {
        return jdbc.queryForObject("SELECT MAX(ORDER_ID) FROM " + config.getOrderTable() + " WHERE APPLICANT_KEY = ?",
                String.class, config.getApplicantKey());
    }
}
