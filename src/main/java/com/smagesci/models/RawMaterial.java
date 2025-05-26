package com.smagesci.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RawMaterial {
    @JsonProperty("materialId")
    private String materialId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("supplierId")
    private String supplierId;
    @JsonProperty("unitOfMeasure")
    private String unitOfMeasure;
    @JsonProperty("costPerUnit")
    private double costPerUnit;
    @JsonProperty("currentStock")
    private int currentStock;
    @JsonProperty("minStockLevel")
    private int minStockLevel;

    // Constructors
    public RawMaterial() {
    }

    public RawMaterial(String materialId, String name, String supplierId, 
                      String unitOfMeasure, double costPerUnit, int currentStock, int minStockLevel) {
        this.materialId = materialId;
        this.name = name;
        this.supplierId = supplierId;
        this.unitOfMeasure = unitOfMeasure;
        this.costPerUnit = costPerUnit;
        this.currentStock = currentStock;
        this.minStockLevel = minStockLevel;
    }

    // Getters and Setters
    public String getMaterialId() { return materialId; }
    public void setMaterialId(String materialId) { this.materialId = materialId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getUnitOfMeasure() { return unitOfMeasure; }
    public void setUnitOfMeasure(String unitOfMeasure) { this.unitOfMeasure = unitOfMeasure; }
    public double getCostPerUnit() { return costPerUnit; }
    public void setCostPerUnit(double costPerUnit) { this.costPerUnit = costPerUnit; }
    public int getCurrentStock() { return currentStock; }
    public void setCurrentStock(int currentStock) { this.currentStock = currentStock; }
    public int getMinStockLevel() { return minStockLevel; }
    public void setMinStockLevel(int minStockLevel) { this.minStockLevel = minStockLevel; }

    public boolean isLowStock() {
        return currentStock <= minStockLevel;
    }

    @Override
    public String toString() {
        return "RawMaterial{" +
               "materialId='" + materialId + '\'' +
               ", name='" + name + '\'' +
               ", supplierId='" + supplierId + '\'' +
               ", unitOfMeasure='" + unitOfMeasure + '\'' +
               ", costPerUnit=" + costPerUnit +
               ", currentStock=" + currentStock +
               ", minStockLevel=" + minStockLevel +
               '}';
    }
}
