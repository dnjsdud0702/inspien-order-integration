package com.inspien.integration.shipment;

import com.inspien.integration.common.IdGenerator;
import com.inspien.integration.config.Config;
import com.inspien.integration.order.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class ShipmentDao {

    private final JdbcTemplate jdbc;
    private final Config config;

    public String insert(Order order) {
        String shipmentId = IdGenerator.next(maxShipmentId());
        String sql = "INSERT INTO " + config.getShipmentTable()
                + " (SHIPMENT_ID, APPLICANT_KEY, ORDER_ID, ITEM_ID, ADDRESS) VALUES (?, ?, ?, ?, ?)";
        jdbc.update(sql, shipmentId, config.getApplicantKey(), order.orderId(), order.itemId(), order.address());
        return shipmentId;
    }

    private String maxShipmentId() {
        return jdbc.queryForObject("SELECT MAX(SHIPMENT_ID) FROM " + config.getShipmentTable()
                + " WHERE APPLICANT_KEY = ?", String.class, config.getApplicantKey());
    }
}
