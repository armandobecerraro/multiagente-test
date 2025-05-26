package com.smagesci;

import com.smagesci.dao.InventoryDAO;
import com.smagesci.models.InventoryItem;
import com.smagesci.utils.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Clase de prueba para verificar la conectividad con la base de datos PostgreSQL
 * y el funcionamiento de los DAOs del sistema SMAGESCI
 */
public class DatabaseConnectionTest {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseConnectionTest.class);
    
    public static void main(String[] args) {
        System.out.println("🔍 PROBANDO CONECTIVIDAD CON BASE DE DATOS SMAGESCI");
        System.out.println("===================================================");
        
        // Test 1: Conexión básica
        testBasicConnection();
        
        // Test 2: Verificar tablas existentes
        testTablesExistence();
        
        // Test 3: Consultar datos de ejemplo
        testSampleData();
        
        // Test 4: Probar DAO de inventario
        testInventoryDAO();
        
        System.out.println("\n🎉 PRUEBAS DE BASE DE DATOS COMPLETADAS");
    }
    
    private static void testBasicConnection() {
        System.out.println("\n📋 Test 1: Conexión básica a PostgreSQL...");
        
        try (Connection conn = DatabaseManager.getConnection()) {
            if (conn != null && !conn.isClosed()) {
                log.info("✅ Conexión establecida exitosamente");
                log.info("Database URL: {}", conn.getMetaData().getURL());
                log.info("Database Product: {}", conn.getMetaData().getDatabaseProductName());
                log.info("Database Version: {}", conn.getMetaData().getDatabaseProductVersion());
                System.out.println("✅ Conexión a PostgreSQL exitosa");
            } else {
                log.error("❌ No se pudo establecer la conexión");
                System.out.println("❌ Error en conexión a PostgreSQL");
            }
        } catch (SQLException e) {
            log.error("❌ Error conectando a la base de datos: {}", e.getMessage());
            System.out.println("❌ Error: " + e.getMessage());
        }
    }
    
    private static void testTablesExistence() {
        System.out.println("\n📋 Test 2: Verificando existencia de tablas...");
        
        String[] expectedTables = {
            "products", "customers", "orders", "order_items", "inventory", 
            "raw_materials", "production_lines", "production_jobs", 
            "suppliers", "shipments", "system_events"
        };
        
        try (Connection conn = DatabaseManager.getConnection()) {
            for (String table : expectedTables) {
                String sql = "SELECT EXISTS (SELECT FROM information_schema.tables WHERE table_name = ?)";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, table);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next() && rs.getBoolean(1)) {
                            System.out.println("✅ Tabla '" + table + "' existe");
                            log.info("Tabla '{}' encontrada", table);
                        } else {
                            System.out.println("❌ Tabla '" + table + "' NO existe");
                            log.warn("Tabla '{}' no encontrada", table);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            log.error("Error verificando tablas: {}", e.getMessage());
            System.out.println("❌ Error verificando tablas: " + e.getMessage());
        }
    }
    
    private static void testSampleData() {
        System.out.println("\n📋 Test 3: Consultando datos de ejemplo...");
        
        // Test productos
        try (Connection conn = DatabaseManager.getConnection()) {
            String sql = "SELECT COUNT(*) as count FROM products";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✅ Productos en BD: " + count);
                    log.info("Total productos: {}", count);
                }
            }
            
            // Test clientes
            sql = "SELECT COUNT(*) as count FROM customers";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✅ Clientes en BD: " + count);
                    log.info("Total clientes: {}", count);
                }
            }
            
            // Test inventario
            sql = "SELECT COUNT(*) as count FROM inventory";
            try (PreparedStatement stmt = conn.prepareStatement(sql);
                 ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    int count = rs.getInt("count");
                    System.out.println("✅ Items de inventario: " + count);
                    log.info("Total items inventario: {}", count);
                }
            }
            
        } catch (SQLException e) {
            log.error("Error consultando datos de ejemplo: {}", e.getMessage());
            System.out.println("❌ Error consultando datos: " + e.getMessage());
        }
    }
    
    private static void testInventoryDAO() {
        System.out.println("\n📋 Test 4: Probando InventoryDAO...");
        
        InventoryDAO dao = new InventoryDAO();
        
        try {
            // Test finding all inventory items
            List<InventoryItem> allItems = dao.findAll();
            System.out.println("✅ Total inventory items: " + allItems.size());
            
            if (!allItems.isEmpty()) {
                InventoryItem firstItem = allItems.get(0);
                System.out.println("✅ First item details:");
                System.out.println("   ID: " + firstItem.getInventoryId());
                System.out.println("   Item ID: " + firstItem.getItemId());
                System.out.println("   Location: " + firstItem.getLocation());
                System.out.println("   Stock: " + firstItem.getQuantity());
                
                // Test finding by item ID and location
                Optional<InventoryItem> item = dao.findByItemIdAndLocation(firstItem.getItemId(), firstItem.getLocation());
                if (item.isPresent()) {
                    InventoryItem inventoryItem = item.get();
                    System.out.println("✅ Found specific item: " + inventoryItem.getItemId() + " at " + inventoryItem.getLocation());
                    log.info("InventoryDAO test exitoso - Item: {}, Cantidad: {}", 
                            inventoryItem.getItemId(), inventoryItem.getQuantity());
                }
            }
            
            // Test finding by specific item ID
            List<InventoryItem> items = dao.findByItemId(1001);
            System.out.println("✅ Items with ID 1001: " + items.size());
            log.info("Búsqueda por item 1001: {} items", items.size());
            
            // Test finding low stock items
            List<InventoryItem> lowStockItems = dao.findLowStockItems();
            System.out.println("✅ Low stock items: " + lowStockItems.size());
            
        } catch (Exception e) {
            log.error("Error en InventoryDAO test: {}", e.getMessage());
            System.out.println("❌ Error en InventoryDAO: " + e.getMessage());
        }
    }
}
