package com.smagesci.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OrderItem {
    @JsonProperty("productId")
    private String productId;
    @JsonProperty("quantity")
    private int quantity;
    @JsonProperty("unitPrice")
    private double unitPrice; // Precio al momento de la orden

    // Constructores, getters y setters
    public OrderItem() {
    }

    public OrderItem(String productId, int quantity, double unitPrice) {
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
    }

    // Getters y Setters (omitiendo por brevedad)
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    @Override
    public String toString() {
        return "OrderItem{" +
               "productId='" + productId + '\'' +
               ", quantity=" + quantity +
               ", unitPrice=" + unitPrice +
               '}';
    }
}
