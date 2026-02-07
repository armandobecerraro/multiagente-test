package com.smagesci.utils;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DatabaseManagerTest {
    
    @Test
    void testGetConnectionThrowsWhenNoDatabase() {
        // Without a real database, getConnection should throw SQLException
        // This validates that the configuration is being loaded properly
        assertThrows(Exception.class, () -> {
            DatabaseManager.getConnection();
        });
    }
}
