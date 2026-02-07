package com.smagesci.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseManager {
    
    private static final Logger log = LoggerFactory.getLogger(DatabaseManager.class);
    private static HikariDataSource dataSource;
    
    static {
        initializeDataSource();
    }
    
    private static void initializeDataSource() {
        try {
            Properties dbProperties = loadDatabaseProperties();
            
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(dbProperties.getProperty("db.url"));
            config.setUsername(dbProperties.getProperty("db.username"));
            config.setPassword(dbProperties.getProperty("db.password"));
            
            // Connection pool settings
            config.setMaximumPoolSize(10);
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setConnectionTestQuery("SELECT 1");
            
            dataSource = new HikariDataSource(config);
            log.info("HikariCP connection pool initialized successfully");
        } catch (Exception e) {
            log.error("Failed to initialize HikariCP connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }
    
    private static Properties loadDatabaseProperties() {
        Properties dbProperties = new Properties();
        
        // First priority: Environment variables
        String envUrl = System.getenv("DB_URL");
        String envUsername = System.getenv("DB_USERNAME");
        String envPassword = System.getenv("DB_PASSWORD");
        
        if (envUrl != null && !envUrl.trim().isEmpty()) {
            dbProperties.setProperty("db.url", envUrl);
            dbProperties.setProperty("db.username", envUsername != null ? envUsername : "postgres");
            dbProperties.setProperty("db.password", envPassword != null ? envPassword : "");
            log.info("Database properties loaded from environment variables");
            return dbProperties;
        }
        
        // Second priority: application.properties file
        try (InputStream is = DatabaseManager.class.getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                dbProperties.load(is);
                
                // Resolve property placeholders with environment variables
                String url = resolveProperty(dbProperties, "db.url", "jdbc:postgresql://localhost:5432/supply_chain");
                String username = resolveProperty(dbProperties, "db.username", "postgres");
                String password = resolveProperty(dbProperties, "db.password", "");
                
                dbProperties.setProperty("db.url", url);
                dbProperties.setProperty("db.username", username);
                dbProperties.setProperty("db.password", password);
                
                log.info("Database properties loaded from application.properties");
                
                // Check if required properties are present
                if (password.isEmpty()) {
                    log.warn("Database password is empty. Set DB_PASSWORD environment variable for secure configuration.");
                }
            } else {
                log.error("Could not find application.properties file");
                setMinimalFallbackProperties(dbProperties);
            }
        } catch (IOException e) {
            log.error("Error loading database properties", e);
            setMinimalFallbackProperties(dbProperties);
        }
        
        return dbProperties;
    }
    
    private static String resolveProperty(Properties props, String key, String defaultValue) {
        String value = props.getProperty(key);
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
    
    private static void setMinimalFallbackProperties(Properties props) {
        // In production, we should not use fallback values. This is only for development.
        String env = System.getenv("ENVIRONMENT");
        if ("production".equalsIgnoreCase(env)) {
            log.error("Database configuration is missing in production environment. Cannot proceed without proper configuration.");
            throw new RuntimeException("Database configuration required for production environment");
        }
        
        props.setProperty("db.url", "jdbc:postgresql://localhost:5432/supply_chain");
        props.setProperty("db.username", "postgres");
        props.setProperty("db.password", "");
        log.warn("Using fallback database properties for development. DO NOT use in production. Set DB_URL, DB_USERNAME, and DB_PASSWORD environment variables.");
    }
    
    public static Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Database connection pool is not available");
        }
        return dataSource.getConnection();
    }
    
    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            log.info("HikariCP connection pool shutdown successfully");
        }
    }
    
    public static void closeConnection(Connection connection) {
        if (connection != null) {
            try {
                connection.close();
                log.debug("Database connection returned to pool");
            } catch (SQLException e) {
                log.error("Error closing database connection", e);
            }
        }
    }
}
