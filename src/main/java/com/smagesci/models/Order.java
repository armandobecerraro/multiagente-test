package com.smagesci.models;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;
import java.util.List;

public class Order {
    @JsonProperty("orderId")
    private String orderId;
    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("orderDate")
    private LocalDateTime orderDate;
    @JsonProperty("items")
    private List<OrderItem> items;
    @JsonProperty("status")
    private String status; // e.g., PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELED

    // Constructores, getters y setters
    public Order() {
    }

    public Order(String orderId, String customerId, LocalDateTime orderDate, List<OrderItem> items, String status) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.orderDate = orderDate;
        this.items = items;
        this.status = status;
    }

    // Getters y Setters (omitiendo por brevedad, pero deben estar aquí)
    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public LocalDateTime getOrderDate() { return orderDate; }
    public void setOrderDate(LocalDateTime orderDate) { this.orderDate = orderDate; }
    public List<OrderItem> getItems() { return items; }
    public void setItems(List<OrderItem> items) { this.items = items; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Order{" +
               "orderId='" + orderId + '\'' +
               ", customerId='" + customerId + '\'' +
               ", orderDate=" + orderDate +
               ", items=" + items +
               ", status='" + status + '\'' +
               '}';
    }
}
