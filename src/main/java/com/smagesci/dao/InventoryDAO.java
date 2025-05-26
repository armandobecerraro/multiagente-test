package com.smagesci.dao;

import com.smagesci.models.InventoryItem;
import com.smagesci.utils.DatabaseManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InventoryDAO {
    
    private static final Logger log = LoggerFactory.getLogger(InventoryDAO.class);
    
    public Optional<InventoryItem> findByItemIdAndLocation(Integer itemId, String location) {
        String sql = "SELECT id, item_id, location, current_stock, reserved_stock, " +
                    "min_stock_level, max_stock_level FROM inventory WHERE item_id = ? AND location = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, itemId);
            stmt.setString(2, location);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    InventoryItem item = new InventoryItem(
                        rs.getLong("id"),
                        rs.getInt("item_id"),
                        rs.getString("location"),
                        rs.getInt("current_stock"),
                        rs.getInt("reserved_stock"),
                        rs.getInt("min_stock_level"),
                        rs.getInt("max_stock_level")
                    );
                    return Optional.of(item);
                }
            }
        } catch (SQLException e) {
            log.error("Error finding inventory item for item {} at location {}", itemId, location, e);
        }
        
        return Optional.empty();
    }
    
    public List<InventoryItem> findByItemId(Integer itemId) {
        String sql = "SELECT id, item_id, location, current_stock, reserved_stock, " +
                    "min_stock_level, max_stock_level FROM inventory WHERE item_id = ?";
        List<InventoryItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, itemId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    InventoryItem item = new InventoryItem(
                        rs.getLong("id"),
                        rs.getInt("item_id"),
                        rs.getString("location"),
                        rs.getInt("current_stock"),
                        rs.getInt("reserved_stock"),
                        rs.getInt("min_stock_level"),
                        rs.getInt("max_stock_level")
                    );
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            log.error("Error finding inventory items for item {}", itemId, e);
        }
        
        return items;
    }
    
    public List<InventoryItem> findAll() {
        String sql = "SELECT id, item_id, location, current_stock, reserved_stock, " +
                    "min_stock_level, max_stock_level FROM inventory";
        List<InventoryItem> items = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                InventoryItem item = new InventoryItem(
                    rs.getLong("id"),
                    rs.getInt("item_id"),
                    rs.getString("location"),
                    rs.getInt("current_stock"),
                    rs.getInt("reserved_stock"),
                    rs.getInt("min_stock_level"),
                    rs.getInt("max_stock_level")
                );
                items.add(item);
            }
        } catch (SQLException e) {
            log.error("Error finding all inventory items", e);
        }
        
        return items;
    }
    
    public boolean updateQuantity(Integer itemId, String location, int newQuantity) {
        String sql = "UPDATE inventory SET current_stock = ?, last_updated = CURRENT_TIMESTAMP " +
                    "WHERE item_id = ? AND location = ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, newQuantity);
            stmt.setInt(2, itemId);
            stmt.setString(3, location);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                log.info("Updated inventory for item {} at location {} to quantity {}", 
                        itemId, location, newQuantity);
                return true;
            }
        } catch (SQLException e) {
            log.error("Error updating inventory quantity for item {} at location {}", 
                     itemId, location, e);
        }
        
        return false;
    }
    
    public boolean reserveQuantity(Integer itemId, String location, int quantityToReserve) {
        String sql = "UPDATE inventory SET reserved_stock = reserved_stock + ?, " +
                    "last_updated = CURRENT_TIMESTAMP " +
                    "WHERE item_id = ? AND location = ? AND (current_stock - reserved_stock) >= ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantityToReserve);
            stmt.setInt(2, itemId);
            stmt.setString(3, location);
            stmt.setInt(4, quantityToReserve);
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                log.info("Reserved {} units of item {} at location {}", 
                        quantityToReserve, itemId, location);
                return true;
            } else {
                log.warn("Could not reserve {} units of item {} at location {} - insufficient stock", 
                        quantityToReserve, itemId, location);
            }
        } catch (SQLException e) {
            log.error("Error reserving inventory for item {} at location {}", 
                     itemId, location, e);
        }
        
        return false;
    }
    
    public List<InventoryItem> findLowStockItems() {
        String sql = "SELECT id, item_id, location, current_stock, reserved_stock, " +
                    "min_stock_level, max_stock_level FROM inventory WHERE current_stock <= min_stock_level";
        List<InventoryItem> lowStockItems = new ArrayList<>();
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                InventoryItem item = new InventoryItem(
                    rs.getLong("id"),
                    rs.getInt("item_id"),
                    rs.getString("location"),
                    rs.getInt("current_stock"),
                    rs.getInt("reserved_stock"),
                    rs.getInt("min_stock_level"),
                    rs.getInt("max_stock_level")
                );
                lowStockItems.add(item);
            }
        } catch (SQLException e) {
            log.error("Error finding low stock items", e);
        }
        
        return lowStockItems;
    }
    
    public boolean insertInventoryItem(InventoryItem item) {
        String sql = "INSERT INTO inventory (item_id, location, current_stock, reserved_stock, " +
                    "min_stock_level, max_stock_level) VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, item.getItemId());
            stmt.setString(2, item.getLocation());
            stmt.setInt(3, item.getQuantity());
            stmt.setInt(4, item.getReservedQuantity());
            stmt.setInt(5, item.getMinStockLevel());
            stmt.setInt(6, item.getMaxStockLevel());
            
            int rowsInserted = stmt.executeUpdate();
            if (rowsInserted > 0) {
                log.info("Inserted new inventory item for item {} at location {}", 
                        item.getItemId(), item.getLocation());
                return true;
            }
        } catch (SQLException e) {
            log.error("Error inserting inventory item", e);
        }
        
        return false;
    }
    
    /**
     * Reduce the quantity of an inventory item at a specific location
     */
    public boolean reduceQuantity(Integer itemId, String location, int quantityToReduce) {
        String sql = "UPDATE inventory SET current_stock = current_stock - ? " +
                    "WHERE item_id = ? AND location = ? AND current_stock >= ?";
        
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, quantityToReduce);
            stmt.setInt(2, itemId);
            stmt.setString(3, location);
            stmt.setInt(4, quantityToReduce); // Ensure we don't reduce below 0
            
            int rowsUpdated = stmt.executeUpdate();
            if (rowsUpdated > 0) {
                log.info("Reduced quantity by {} for item {} at location {}", 
                        quantityToReduce, itemId, location);
                return true;
            } else {
                log.warn("Could not reduce quantity - insufficient stock for item {} at location {}", 
                        itemId, location);
                return false;
            }
        } catch (SQLException e) {
            log.error("Error reducing quantity for item {} at location {}", itemId, location, e);
            return false;
        }
    }
}
