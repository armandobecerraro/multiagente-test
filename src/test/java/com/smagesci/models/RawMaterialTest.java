package com.smagesci.models;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class RawMaterialTest {
    
    @Test
    void testRawMaterialCreation() {
        RawMaterial material = new RawMaterial("MAT001", "Steel", "SUPP001", "kg", 5.50, 1000, 100);
        assertEquals("MAT001", material.getMaterialId());
        assertEquals("Steel", material.getName());
        assertEquals("SUPP001", material.getSupplierId());
        assertEquals("kg", material.getUnitOfMeasure());
        assertEquals(5.50, material.getCostPerUnit(), 0.001);
        assertEquals(1000, material.getCurrentStock());
        assertEquals(100, material.getMinStockLevel());
    }
    
    @Test
    void testRawMaterialDefaultConstructor() {
        RawMaterial material = new RawMaterial();
        assertNotNull(material);
    }
    
    @Test
    void testRawMaterialSetters() {
        RawMaterial material = new RawMaterial();
        material.setMaterialId("MAT002");
        material.setName("Aluminum");
        material.setSupplierId("SUPP002");
        material.setUnitOfMeasure("ton");
        material.setCostPerUnit(10.75);
        material.setCurrentStock(500);
        material.setMinStockLevel(50);
        
        assertEquals("MAT002", material.getMaterialId());
        assertEquals("Aluminum", material.getName());
        assertEquals("SUPP002", material.getSupplierId());
        assertEquals("ton", material.getUnitOfMeasure());
        assertEquals(10.75, material.getCostPerUnit(), 0.001);
        assertEquals(500, material.getCurrentStock());
        assertEquals(50, material.getMinStockLevel());
    }
    
    @Test
    void testIsLowStock() {
        RawMaterial material = new RawMaterial("MAT001", "Steel", "SUPP001", "kg", 5.50, 90, 100);
        assertTrue(material.isLowStock());
        
        material.setCurrentStock(150);
        assertFalse(material.isLowStock());
    }
    
    @Test
    void testRawMaterialToString() {
        RawMaterial material = new RawMaterial("MAT001", "Steel", "SUPP001", "kg", 5.50, 1000, 100);
        String str = material.toString();
        assertTrue(str.contains("MAT001"));
        assertTrue(str.contains("Steel"));
        assertTrue(str.contains("SUPP001"));
    }
}
