package org.ahmet.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

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
            String jdbcUrl = String.format("jdbc:mysql://%s:%s/%s?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true", 
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
        return properties.getProperty(key, System.getProperty(key, defaultValue));
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