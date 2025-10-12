package org.ahmet.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Database configuration class responsible for managing database connections
 * and connection pooling using HikariCP.
 */
public class DatabaseConfig {
    private static final Logger LOGGER = Logger.getLogger(DatabaseConfig.class.getName());
    private static final String PROPERTIES_FILE = "database.properties";
    private static volatile HikariDataSource dataSource;
    private static final Properties properties = new Properties();

    static {
        loadProperties();
        initializeDataSource();
    }

    private static void loadProperties() {
        try (InputStream input = DatabaseConfig.class.getClassLoader()
                .getResourceAsStream(PROPERTIES_FILE)) {
            if (input == null) {
                throw new RuntimeException("Unable to find " + PROPERTIES_FILE);
            }
            properties.load(input);
            LOGGER.info("Database properties loaded successfully");
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading database properties", ex);
            throw new RuntimeException("Failed to load database configuration", ex);
        }
    }

    private static void initializeDataSource() {
        try {
            HikariConfig config = new HikariConfig();
            
            String host = getProperty("database.host", "localhost");
            String port = getProperty("database.port", "3306");
            String dbName = getProperty("database.name", "testdb");
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true", 
                                         host, port, dbName);
            
            config.setJdbcUrl(jdbcUrl);
            config.setUsername(getProperty("database.user", "root"));
            config.setPassword(getProperty("database.password", ""));
            
            // Connection pool settings
            config.setMaximumPoolSize(Integer.parseInt(getProperty("database.pool.maximum-size", "10")));
            config.setConnectionTimeout(Long.parseLong(getProperty("database.pool.connection-timeout", "30000")));
            config.setIdleTimeout(Long.parseLong(getProperty("database.pool.idle-timeout", "600000")));
            config.setMaxLifetime(Long.parseLong(getProperty("database.pool.max-lifetime", "1800000")));
            
            // Additional security and performance settings
            config.setLeakDetectionThreshold(60000);
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            
            dataSource = new HikariDataSource(config);
            LOGGER.info("Database connection pool initialized successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to initialize database connection pool", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    private static String getProperty(String key, String defaultValue) {
        // First check environment variables based on property key
        String envValue = getEnvironmentValue(key);
        if (envValue != null) {
            return envValue;
        }
        // Then check system properties and properties file
        return properties.getProperty(key, System.getProperty(key, defaultValue));
    }
    
    private static String getEnvironmentValue(String propertyKey) {
        // Map property keys to environment variable names
        switch (propertyKey) {
            case "database.name": return System.getenv("DB_NAME");
            case "database.user": return System.getenv("DB_USER");
            case "database.password": return System.getenv("DB_PASSWORD");
            case "database.host": return System.getenv("DB_HOST");
            case "database.port": return System.getenv("DB_PORT");
            case "database.url": return System.getenv("DB_URL");
            case "database.pool.maximum-size": return System.getenv("DB_POOL_SIZE");
            case "database.pool.connection-timeout": return System.getenv("DB_CONNECTION_TIMEOUT");
            case "database.pool.idle-timeout": return System.getenv("DB_IDLE_TIMEOUT");
            case "database.pool.max-lifetime": return System.getenv("DB_MAX_LIFETIME");
            default: return null;
        }
    }

    public static DataSource getDataSource() {
        return dataSource;
    }

    public static String getDatabaseName() {
        return getProperty("database.name", "testdb");
    }

    public static void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            LOGGER.info("Database connection pool closed");
        }
    }
}