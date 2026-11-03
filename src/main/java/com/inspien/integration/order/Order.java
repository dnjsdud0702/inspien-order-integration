package com.inspien.integration.order;

public record Order(String orderId, String userId, String itemId, String name, String address,
                    String itemName, String price) {

    public Order withOrderId(String orderId) {
        return new Order(orderId, userId, itemId, name, address, itemName, price);
    }
}
