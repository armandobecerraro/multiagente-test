package com.smagesci.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Product {
    @JsonProperty("productId")
    private String productId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("description")
    private String description;
    @JsonProperty("category")
    private String category;
    @JsonProperty("basePrice")
    private double basePrice;

    // Constructores, getters y setters
    public Product() {
    }

    public Product(String productId, String name, String description, String category, double basePrice) {
        this.productId = productId;
        this.name = name;
        this.description = description;
        this.category = category;
        this.basePrice = basePrice;
    }

    // Getters y Setters (omitiendo por brevedad)
    public String getProductId() { return productId; }
    public void setProductId(String productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public double getBasePrice() { return basePrice; }
    public void setBasePrice(double basePrice) { this.basePrice = basePrice; }

    @Override
    public String toString() {
        return "Product{" +
               "productId='" + productId + '\'' +
               ", name='" + name + '\'' +
               ", description='" + description + '\'' +
               ", category='" + category + '\'' +
               ", basePrice=" + basePrice +
               '}';
    }
}
