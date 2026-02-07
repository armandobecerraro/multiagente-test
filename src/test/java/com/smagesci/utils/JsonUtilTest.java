package com.smagesci.utils;

import com.smagesci.models.Order;
import com.smagesci.models.OrderItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

class JsonUtilTest {

    private Order testOrder;
    private OrderItem testOrderItem;

    @BeforeEach
    void setUp() {
        testOrderItem = new OrderItem("PROD001", 10, 29.99);
        List<OrderItem> items = Arrays.asList(testOrderItem);
        testOrder = new Order("ORD000001", "CUST001", LocalDateTime.now(), items, "PENDING");
    }

    @Test
    void testToJson_ValidObject_ReturnsJsonString() {
        // Given
        OrderItem item = new OrderItem("PROD001", 5, 19.99);
        
        // When
        String json = JsonUtil.toJson(item);
        
        // Then
        assertNotNull(json);
        assertTrue(json.contains("PROD001"));
        assertTrue(json.contains("19.99"));
        assertTrue(json.contains("5"));
    }

    @Test
    void testToJson_NullObject_ReturnsNull() {
        // When
        String json = JsonUtil.toJson(null);
        
        // Then
        // Jackson returns the string "null" for null input
        assertEquals("null", json);
    }

    @Test
    void testFromJson_ValidJson_ReturnsObject() {
        // Given
        String json = "{\"productId\":\"PROD001\",\"quantity\":5,\"unitPrice\":19.99}";
        
        // When
        OrderItem result = JsonUtil.fromJson(json, OrderItem.class);
        
        // Then
        assertNotNull(result);
        assertEquals("PROD001", result.getProductId());
        assertEquals(5, result.getQuantity());
        assertEquals(19.99, result.getUnitPrice(), 0.01);
    }

    @Test
    void testFromJson_InvalidJson_ReturnsNull() {
        // Given
        String invalidJson = "{invalid json";
        
        // When
        OrderItem result = JsonUtil.fromJson(invalidJson, OrderItem.class);
        
        // Then
        assertNull(result);
    }

    @Test
    void testFromJson_NullJson_ReturnsNull() {
        // When
        OrderItem result = JsonUtil.fromJson(null, OrderItem.class);
        
        // Then
        // Jackson throws IllegalArgumentException for null JSON string, caught and returns null
        assertNull(result);
    }

    @Test
    void testRoundTripSerialization_Order() {
        // When
        String json = JsonUtil.toJson(testOrder);
        Order deserializedOrder = JsonUtil.fromJson(json, Order.class);
        
        // Then
        assertNotNull(json);
        assertNotNull(deserializedOrder);
        assertEquals(testOrder.getOrderId(), deserializedOrder.getOrderId());
        assertEquals(testOrder.getCustomerId(), deserializedOrder.getCustomerId());
        assertEquals(testOrder.getStatus(), deserializedOrder.getStatus());
        assertEquals(testOrder.getItems().size(), deserializedOrder.getItems().size());
        
        OrderItem originalItem = testOrder.getItems().get(0);
        OrderItem deserializedItem = deserializedOrder.getItems().get(0);
        assertEquals(originalItem.getProductId(), deserializedItem.getProductId());
        assertEquals(originalItem.getQuantity(), deserializedItem.getQuantity());
        assertEquals(originalItem.getUnitPrice(), deserializedItem.getUnitPrice(), 0.01);
    }

    @Test
    void testDateTimeSerialization() {
        // Given
        LocalDateTime now = LocalDateTime.now();
        testOrder.setOrderDate(now);
        
        // When
        String json = JsonUtil.toJson(testOrder);
        Order deserializedOrder = JsonUtil.fromJson(json, Order.class);
        
        // Then
        assertNotNull(deserializedOrder);
        assertNotNull(deserializedOrder.getOrderDate());
        // Note: Due to Jackson's date/time handling, we compare strings
        assertEquals(now.toString(), deserializedOrder.getOrderDate().toString());
    }
}
