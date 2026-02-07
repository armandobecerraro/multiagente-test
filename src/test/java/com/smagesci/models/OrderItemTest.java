package com.smagesci.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class OrderItemTest {
    
    @Test
    void testOrderItemCreation() {
        OrderItem item = new OrderItem("PROD001", 5, 19.99);
        assertEquals("PROD001", item.getProductId());
        assertEquals(5, item.getQuantity());
        assertEquals(19.99, item.getUnitPrice(), 0.001);
    }
    
    @Test
    void testOrderItemDefaultConstructor() {
        OrderItem item = new OrderItem();
        assertNotNull(item);
    }
    
    @Test
    void testOrderItemSetters() {
        OrderItem item = new OrderItem();
        item.setProductId("PROD002");
        item.setQuantity(10);
        item.setUnitPrice(29.99);
        
        assertEquals("PROD002", item.getProductId());
        assertEquals(10, item.getQuantity());
        assertEquals(29.99, item.getUnitPrice(), 0.001);
    }
    
    @Test
    void testOrderItemToString() {
        OrderItem item = new OrderItem("PROD001", 5, 19.99);
        String str = item.toString();
        assertTrue(str.contains("PROD001"));
        assertTrue(str.contains("5"));
        assertTrue(str.contains("19.99"));
    }
}
