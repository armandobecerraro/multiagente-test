package com.smagesci.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class InventoryItem {
    @JsonProperty("inventoryId")
    private Long inventoryId;
    @JsonProperty("itemId")
    private Integer itemId;
    @JsonProperty("location")
    private String location;
    @JsonProperty("quantity")
    private int quantity;
    @JsonProperty("reservedQuantity")
    private int reservedQuantity;
    @JsonProperty("minStockLevel")
    private int minStockLevel;
    @JsonProperty("maxStockLevel")
    private int maxStockLevel;

    // Constructors
    public InventoryItem() {
    }

    public InventoryItem(Integer itemId, String location, int quantity) {
        this.itemId = itemId;
        this.location = location;
        this.quantity = quantity;
        this.reservedQuantity = 0;
        this.minStockLevel = 0;
        this.maxStockLevel = 1000;
    }

    public InventoryItem(Long inventoryId, Integer itemId, String location, int quantity, 
                        int reservedQuantity, int minStockLevel, int maxStockLevel) {
        this.inventoryId = inventoryId;
        this.itemId = itemId;
        this.location = location;
        this.quantity = quantity;
        this.reservedQuantity = reservedQuantity;
        this.minStockLevel = minStockLevel;
        this.maxStockLevel = maxStockLevel;
    }

    // Getters and Setters
    public Long getInventoryId() { return inventoryId; }
    public void setInventoryId(Long inventoryId) { this.inventoryId = inventoryId; }
    public Integer getItemId() { return itemId; }
    public void setItemId(Integer itemId) { this.itemId = itemId; }
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getReservedQuantity() { return reservedQuantity; }
    public void setReservedQuantity(int reservedQuantity) { this.reservedQuantity = reservedQuantity; }
    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }
    public int getMaxStockLevel() { return maxStockLevel; }
    public void setMaxStockLevel(int maxStockLevel) { this.maxStockLevel = maxStockLevel; }

    public int getAvailableQuantity() {
        return quantity - reservedQuantity;
    }

    public boolean isLowStock() {
        return quantity <= minStockLevel;
    }

    public boolean isOverStock() {
        return quantity >= maxStockLevel;
    }

    @Override
    public String toString() {
        return "InventoryItem{" +
               "inventoryId=" + inventoryId +
               ", itemId=" + itemId +
               ", location='" + location + '\'' +
               ", quantity=" + quantity +
               ", reservedQuantity=" + reservedQuantity +
               ", minStockLevel=" + minStockLevel +
               ", maxStockLevel=" + maxStockLevel +
               '}';
    }
}
