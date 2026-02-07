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
        
        // First priority: Environment variables
        String envUrl = System.getenv("DB_URL");
        String envUsername = System.getenv("DB_USERNAME");
        String envPassword = System.getenv("DB_PASSWORD");
        
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            dbProperties.setProperty("db.url", envUrl);
            dbProperties.setProperty("db.username", envUsername != null ? envUsername : "");
            dbProperties.setProperty("db.password", envPassword != null ? envPassword : "");
            log.info("Database properties loaded from environment variables");
            return;
        }
        
        // Second priority: application.properties file
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                dbProperties.load(is);
                
                // Resolve property placeholders with environment variables
                String url = resolveProperty("db.url", "jdbc:postgresql://localhost:5432/supply_chain");
                String username = resolveProperty("db.username", "postgres");
                String password = resolveProperty("db.password", "");
                
                dbProperties.setProperty("db.url", url);
                dbProperties.setProperty("db.username", username);
                dbProperties.setProperty("db.password", password);
                
                log.info("Database properties loaded from application.properties");
                
                // Check if required properties are present
                if (url.isEmpty() || username.isEmpty() || password.isEmpty()) {
                    log.warn("Some database properties are empty. Please set DB_URL, DB_USERNAME, and DB_PASSWORD environment variables.");
                }
            } else {
                log.error("Could not find application.properties file");
                setMinimalFallbackProperties();
            }
        } catch (IOException e) {
            log.error("Error loading database properties", e);
            setMinimalFallbackProperties();
        }
    }
    
    private static String resolveProperty(String key, String defaultValue) {
        String value = dbProperties.getProperty(key);
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        
        // Check for ${ENV_VAR:defaultValue} pattern
        if (value.startsWith("${") && value.endsWith("}")) {
            String content = value.substring(2, value.length() - 1);
            String[] parts = content.split(":", 2);
            String envVar = parts[0];
            String envDefault = parts.length > 1 ? parts[1] : defaultValue;
            
            String envValue = System.getenv(envVar);
            return envValue != null ? envValue : envDefault;
        }
        
        return value;
    }
    
    private static void setMinimalFallbackProperties() {
        dbProperties.setProperty("db.url", "jdbc:postgresql://localhost:5432/supply_chain");
        dbProperties.setProperty("db.username", "postgres");
        dbProperties.setProperty("db.password", "");
        log.warn("Using fallback database properties without password. Set environment variables for secure configuration.");
    }
    
    public static Connection getConnection() throws SQLException {
        String url = dbProperties.getProperty("db.url");
        String username = dbProperties.getProperty("db.username");
        String password = dbProperties.getProperty("db.password");
        
        if (url == null || username == null) {
            throw new SQLException("Database configuration is incomplete");
        }
        
        if (password == null || password.isEmpty()) {
            log.warn("Database password is empty. This is not recommended for production environments.");
        }
        
        return DriverManager.getConnection(url, username, password);
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
