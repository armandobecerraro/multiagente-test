package com.smagesci.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {
    
    @Test
    void testCustomerCreation() {
        Customer customer = new Customer("C001", "Test Customer", "test@email.com", "123 Test St");
        assertEquals("C001", customer.getCustomerId());
        assertEquals("Test Customer", customer.getName());
        assertEquals("test@email.com", customer.getContactEmail());
        assertEquals("123 Test St", customer.getAddress());
    }
    
    @Test
    void testCustomerDefaultConstructor() {
        Customer customer = new Customer();
        assertNotNull(customer);
    }
    
    @Test
    void testCustomerSettersAndGetters() {
        Customer c = new Customer();
        c.setCustomerId("C002");
        c.setName("Another Customer");
        c.setContactEmail("another@email.com");
        c.setAddress("456 Another St");
        assertEquals("C002", c.getCustomerId());
        assertEquals("Another Customer", c.getName());
    }
    
    @Test
    void testCustomerToString() {
        Customer c = new Customer("C001", "Test", "test@test.com", "addr");
        assertNotNull(c.toString());
        assertTrue(c.toString().contains("C001"));
    }
}
