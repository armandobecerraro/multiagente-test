package com.smagesci.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static Properties dbProperties;
    
    static {
        loadDatabaseProperties();
    }
    
    private static void loadDatabaseProperties() {
        dbProperties = new Properties();
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                dbProperties.load(is);
                log.info("Database properties loaded successfully");
                
                // Check if required properties are present and not empty
                if (dbProperties.getProperty("db.url") == null || dbProperties.getProperty("db.url").trim().isEmpty() ||
                    dbProperties.getProperty("db.username") == null || dbProperties.getProperty("db.username").trim().isEmpty() ||
                    dbProperties.getProperty("db.password") == null) {
                    log.warn("Application.properties file is empty or missing required properties, using fallback values");
                    setFallbackProperties();
                }
            } else {
                log.error("Could not find application.properties file");
                setFallbackProperties();
            }
        } catch (IOException e) {
            log.error("Error loading database properties", e);
            setFallbackProperties();
        }
    }
    
    private static void setFallbackProperties() {
        dbProperties.setProperty("db.url", "jdbc:postgresql://localhost:5432/supply_chain");
        dbProperties.setProperty("db.username", "postgres");
        dbProperties.setProperty("db.password", "password");
        log.info("Using fallback database properties");
    }
    
    public static Connection getConnection() throws SQLException {
        String url = dbProperties.getProperty("db.url");
        String username = dbProperties.getProperty("db.username");
        String password = dbProperties.getProperty("db.password");
        
        if (url == null || username == null || password == null) {
            throw new SQLException("Database configuration is incomplete");
        }
        
        try {
            Class.forName("org.postgresql.Driver");
            return DriverManager.getConnection(url, username, password);
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL driver not found", e);
        }
    }
    
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Database connection closed");
            } catch (SQLException e) {
                log.error("Error closing database connection", e);
            }
        }
    }
}
