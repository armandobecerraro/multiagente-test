package com.smagesci.dao;

import com.smagesci.models.InventoryItem;
import com.smagesci.utils.DatabaseManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryDAOTest {

    @Mock
    private Connection mockConnection;
    
    @Mock
    private PreparedStatement mockStatement;
    
    @Mock
    private ResultSet mockResultSet;

    private InventoryDAO inventoryDAO;

    @BeforeEach
    void setUp() {
        inventoryDAO = new InventoryDAO();
    }

    @Test
    void testFindByProductIdAndLocation_ExistingItem_ReturnsInventoryItem() throws SQLException {
        // Given
        Integer itemId = 1;
        String location = "MAIN_WAREHOUSE";
        
        when(mockResultSet.next()).thenReturn(true);
        when(mockResultSet.getLong("id")).thenReturn(1L);
        when(mockResultSet.getInt("item_id")).thenReturn(itemId);
        when(mockResultSet.getString("location")).thenReturn(location);
        when(mockResultSet.getInt("current_stock")).thenReturn(100);
        when(mockResultSet.getInt("reserved_stock")).thenReturn(10);
        when(mockResultSet.getInt("min_stock_level")).thenReturn(20);
        when(mockResultSet.getInt("max_stock_level")).thenReturn(500);
        
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            Optional<InventoryItem> result = inventoryDAO.findByItemIdAndLocation(itemId, location);

            // Then
            assertTrue(result.isPresent());
            InventoryItem item = result.get();
            assertEquals(1L, item.getInventoryId());
            assertEquals(itemId, item.getItemId());
            assertEquals(location, item.getLocation());
            assertEquals(100, item.getQuantity());
            assertEquals(10, item.getReservedQuantity());
            assertEquals(20, item.getMinStockLevel());
            assertEquals(500, item.getMaxStockLevel());
            assertEquals(90, item.getAvailableQuantity()); // 100 - 10
        }

        verify(mockStatement).setInt(1, itemId);
        verify(mockStatement).setString(2, location);
    }

    @Test
    void testFindByProductIdAndLocation_NonExistingItem_ReturnsEmpty() throws SQLException {
        // Given
        Integer itemId = 999;
        String location = "MAIN_WAREHOUSE";
        
        when(mockResultSet.next()).thenReturn(false);
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            Optional<InventoryItem> result = inventoryDAO.findByItemIdAndLocation(itemId, location);

            // Then
            assertFalse(result.isPresent());
        }
    }

    @Test
    void testUpdateQuantity_ValidUpdate_ReturnsTrue() throws SQLException {
        // Given
        Integer itemId = 1;
        String location = "MAIN_WAREHOUSE";
        int newQuantity = 150;
        
        when(mockStatement.executeUpdate()).thenReturn(1); // 1 row updated
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            boolean result = inventoryDAO.updateQuantity(itemId, location, newQuantity);

            // Then
            assertTrue(result);
        }

        verify(mockStatement).setInt(1, newQuantity);
        verify(mockStatement).setInt(2, itemId);
        verify(mockStatement).setString(3, location);
    }

    @Test
    void testUpdateQuantity_NoRowsUpdated_ReturnsFalse() throws SQLException {
        // Given
        Integer itemId = 999;
        String location = "MAIN_WAREHOUSE";
        int newQuantity = 150;
        
        when(mockStatement.executeUpdate()).thenReturn(0); // 0 rows updated
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            boolean result = inventoryDAO.updateQuantity(itemId, location, newQuantity);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testReserveQuantity_SufficientStock_ReturnsTrue() throws SQLException {
        // Given
        Integer itemId = 1;
        String location = "MAIN_WAREHOUSE";
        int quantityToReserve = 20;
        
        when(mockStatement.executeUpdate()).thenReturn(1); // 1 row updated
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            boolean result = inventoryDAO.reserveQuantity(itemId, location, quantityToReserve);

            // Then
            assertTrue(result);
        }

        verify(mockStatement).setInt(1, quantityToReserve);
        verify(mockStatement).setInt(2, itemId);
        verify(mockStatement).setString(3, location);
        verify(mockStatement).setInt(4, quantityToReserve);
    }

    @Test
    void testReserveQuantity_InsufficientStock_ReturnsFalse() throws SQLException {
        // Given
        Integer itemId = 1;
        String location = "MAIN_WAREHOUSE";
        int quantityToReserve = 1000; // More than available
        
        when(mockStatement.executeUpdate()).thenReturn(0); // 0 rows updated (condition not met)
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            boolean result = inventoryDAO.reserveQuantity(itemId, location, quantityToReserve);

            // Then
            assertFalse(result);
        }
    }

    @Test
    void testFindByProductId_MultipleLocations_ReturnsAllItems() throws SQLException {
        // Given
        Integer itemId = 1;
        
        when(mockResultSet.next()).thenReturn(true, true, false); // Two results, then no more
        when(mockResultSet.getLong("id")).thenReturn(1L, 2L);
        when(mockResultSet.getInt("item_id")).thenReturn(itemId, itemId);
        when(mockResultSet.getString("location")).thenReturn("MAIN_WAREHOUSE", "BACKUP_WAREHOUSE");
        when(mockResultSet.getInt("current_stock")).thenReturn(100, 50);
        when(mockResultSet.getInt("reserved_stock")).thenReturn(10, 5);
        when(mockResultSet.getInt("min_stock_level")).thenReturn(20, 10);
        when(mockResultSet.getInt("max_stock_level")).thenReturn(500, 200);
        
        when(mockStatement.executeQuery()).thenReturn(mockResultSet);
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            List<InventoryItem> results = inventoryDAO.findByItemId(itemId);

            // Then
            assertEquals(2, results.size());
            assertEquals("MAIN_WAREHOUSE", results.get(0).getLocation());
            assertEquals("BACKUP_WAREHOUSE", results.get(1).getLocation());
            assertEquals(100, results.get(0).getQuantity());
            assertEquals(50, results.get(1).getQuantity());
        }
    }

    @Test
    void testInsertInventoryItem_ValidItem_ReturnsTrue() throws SQLException {
        // Given
        InventoryItem item = new InventoryItem(1, "MAIN_WAREHOUSE", 100);
        item.setMinStockLevel(20);
        item.setMaxStockLevel(500);
        
        when(mockStatement.executeUpdate()).thenReturn(1); // 1 row inserted
        when(mockConnection.prepareStatement(anyString())).thenReturn(mockStatement);

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            boolean result = inventoryDAO.insertInventoryItem(item);

            // Then
            assertTrue(result);
        }

        verify(mockStatement).setInt(1, 1);
        verify(mockStatement).setString(2, "MAIN_WAREHOUSE");
        verify(mockStatement).setInt(3, 100);
        verify(mockStatement).setInt(4, 0);  // reserved_quantity
        verify(mockStatement).setInt(5, 20); // min_stock_level
        verify(mockStatement).setInt(6, 500); // max_stock_level
    }

    @Test
    void testDatabaseException_HandleGracefully() throws SQLException {
        // Given
        Integer itemId = 1;
        String location = "MAIN_WAREHOUSE";
        
        when(mockConnection.prepareStatement(anyString())).thenThrow(new SQLException("Database error"));

        // When
        try (MockedStatic<DatabaseManager> mockedDbManager = mockStatic(DatabaseManager.class)) {
            mockedDbManager.when(DatabaseManager::getConnection).thenReturn(mockConnection);
            
            Optional<InventoryItem> result = inventoryDAO.findByItemIdAndLocation(itemId, location);

            // Then
            assertFalse(result.isPresent());
        }
    }
}
