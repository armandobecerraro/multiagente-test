package com.smagesci.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class InventoryItemTest {
    
    @Test
    void testInventoryItemCreationWithThreeArgs() {
        InventoryItem item = new InventoryItem(1, "WAREHOUSE_A", 100);
        assertEquals(1, item.getItemId());
        assertEquals("WAREHOUSE_A", item.getLocation());
        assertEquals(100, item.getQuantity());
        assertEquals(0, item.getReservedQuantity());
        assertEquals(0, item.getMinStockLevel());
        assertEquals(1000, item.getMaxStockLevel());
    }
    
    @Test
    void testInventoryItemCreationWithAllArgs() {
        InventoryItem item = new InventoryItem(1L, 2, "WAREHOUSE_B", 200, 50, 10, 500);
        assertEquals(1L, item.getInventoryId());
        assertEquals(2, item.getItemId());
        assertEquals("WAREHOUSE_B", item.getLocation());
        assertEquals(200, item.getQuantity());
        assertEquals(50, item.getReservedQuantity());
        assertEquals(10, item.getMinStockLevel());
        assertEquals(500, item.getMaxStockLevel());
    }
    
    @Test
    void testInventoryItemDefaultConstructor() {
        InventoryItem item = new InventoryItem();
        assertNotNull(item);
    }
    
    @Test
    void testInventoryItemSetters() {
        InventoryItem item = new InventoryItem();
        item.setInventoryId(10L);
        item.setItemId(20);
        item.setLocation("WAREHOUSE_C");
        item.setQuantity(300);
        item.setReservedQuantity(75);
        item.setMinStockLevel(20);
        item.setMaxStockLevel(600);
        
        assertEquals(10L, item.getInventoryId());
        assertEquals(20, item.getItemId());
        assertEquals("WAREHOUSE_C", item.getLocation());
        assertEquals(300, item.getQuantity());
        assertEquals(75, item.getReservedQuantity());
        assertEquals(20, item.getMinStockLevel());
        assertEquals(600, item.getMaxStockLevel());
    }
    
    @Test
    void testGetAvailableQuantity() {
        InventoryItem item = new InventoryItem(1, "WAREHOUSE_A", 100);
        item.setReservedQuantity(30);
        assertEquals(70, item.getAvailableQuantity());
    }
    
    @Test
    void testIsLowStock() {
        InventoryItem item = new InventoryItem(1, "WAREHOUSE_A", 10);
        item.setMinStockLevel(15);
        assertTrue(item.isLowStock());
        
        item.setQuantity(20);
        assertFalse(item.isLowStock());
    }
    
    @Test
    void testIsOverStock() {
        InventoryItem item = new InventoryItem(1, "WAREHOUSE_A", 1100);
        item.setMaxStockLevel(1000);
        assertTrue(item.isOverStock());
        
        item.setQuantity(900);
        assertFalse(item.isOverStock());
    }
    
    @Test
    void testInventoryItemToString() {
        InventoryItem item = new InventoryItem(1, "WAREHOUSE_A", 100);
        String str = item.toString();
        assertTrue(str.contains("WAREHOUSE_A"));
        assertTrue(str.contains("100"));
    }
}
