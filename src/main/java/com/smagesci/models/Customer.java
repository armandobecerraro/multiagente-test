package com.smagesci.models;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Customer {
    @JsonProperty("customerId")
    private String customerId;
    @JsonProperty("name")
    private String name;
    @JsonProperty("contactEmail")
    private String contactEmail;
    @JsonProperty("address")
    private String address;

    // Constructores
    public Customer() {
    }

    public Customer(String customerId, String name, String contactEmail, String address) {
        this.customerId = customerId;
        this.name = name;
        this.contactEmail = contactEmail;
        this.address = address;
    }

    // Getters y Setters
    public String getCustomerId() { return customerId; }
    public void setCustomerId(String customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getContactEmail() { return contactEmail; }
    public void setContactEmail(String contactEmail) { this.contactEmail = contactEmail; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    @Override
    public String toString() {
        return "Customer{" +
               "customerId='" + customerId + '\'' +
               ", name='" + name + '\'' +
               ", contactEmail='" + contactEmail + '\'' +
               ", address='" + address + '\'' +
               '}';
    }
}
