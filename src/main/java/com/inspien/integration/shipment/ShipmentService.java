package com.inspien.integration.shipment;

import com.inspien.integration.order.Order;
import com.inspien.integration.order.OrderDao;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ShipmentService {

    private final ShipmentDao shipmentDao;
    private final OrderDao orderDao;

    @Transactional
    public String ship(Order order) {
        String shipmentId = shipmentDao.insert(order);
        orderDao.markShipped(order.orderId());
        return shipmentId;
    }
}
