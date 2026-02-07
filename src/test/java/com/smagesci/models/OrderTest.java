package com.smagesci.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.util.Arrays;

class OrderTest {
    
    @Test
    void testOrderCreation() {
        Order order = new Order("ORD001", "CUST001", LocalDateTime.now(), null, "PENDING");
        assertNotNull(order);
        assertEquals("ORD001", order.getOrderId());
        assertEquals("CUST001", order.getCustomerId());
        assertEquals("PENDING", order.getStatus());
    }
    
    @Test
    void testOrderDefaultConstructor() {
        Order order = new Order();
        assertNotNull(order);
        assertNull(order.getOrderId());
    }
    
    @Test
    void testOrderSetters() {
        Order order = new Order();
        order.setOrderId("ORD002");
        order.setCustomerId("CUST002");
        order.setStatus("PROCESSING");
        assertEquals("ORD002", order.getOrderId());
        assertEquals("CUST002", order.getCustomerId());
        assertEquals("PROCESSING", order.getStatus());
    }
    
    @Test
    void testOrderToString() {
        Order order = new Order("ORD001", "CUST001", LocalDateTime.now(), null, "PENDING");
        String str = order.toString();
        assertTrue(str.contains("ORD001"));
        assertTrue(str.contains("CUST001"));
        assertTrue(str.contains("PENDING"));
    }
}
