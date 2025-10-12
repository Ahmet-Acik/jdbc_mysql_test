// src/main/java/org/ahmet/util/DatabaseUtil.java
package org.ahmet.util;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.sql.DataSource;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

public class DatabaseUtil {
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final Properties properties = new Properties();
    
    // Cache for DataSource instances to prevent creating multiple pools for the same database
    private static final Map<String, HikariDataSource> dataSourceCache = new ConcurrentHashMap<>();

    static {
        try (InputStream input = DatabaseUtil.class.getClassLoader().getResourceAsStream("database.properties")) {
            if (input == null) {
                LOGGER.severe("Sorry, unable to find database.properties");
            } else {
                properties.load(input);
            }
        } catch (IOException ex) {
            LOGGER.log(Level.SEVERE, "Error loading database properties", ex);
        }
    }

    public static Connection getConnection(String dbName) throws SQLException {
        String url = "jdbc:mysql://localhost:3306/" + dbName;
        String user = properties.getProperty("database.user");
        String password = properties.getProperty("database.password");
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Gets or creates a DataSource for the specified database.
     * Uses singleton pattern to prevent multiple connection pools for the same database.
     */
    public static DataSource getDataSource(String dbName) {
        return dataSourceCache.computeIfAbsent(dbName, DatabaseUtil::createDataSource);
    }
    
    /**
     * Creates a new HikariDataSource for the specified database.
     * Private method used by getDataSource singleton pattern.
     */
    private static HikariDataSource createDataSource(String dbName) {
        LOGGER.info("Creating new DataSource for database: " + dbName);
        
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/" + dbName);
        config.setUsername(properties.getProperty("database.user"));
        config.setPassword(properties.getProperty("database.password"));
        
        // Optimized connection pool settings to prevent exhaustion
        config.setMaximumPoolSize(3); // Reduce maximum pool size for testing
        config.setMinimumIdle(1); // Keep at least one connection idle
        config.setConnectionTimeout(10000); // 10 seconds timeout
        config.setIdleTimeout(30000); // 30 seconds idle timeout
        config.setMaxLifetime(600000); // 10 minutes max lifetime
        config.setLeakDetectionThreshold(5000); // 5 seconds leak detection
        
        // Additional settings for better resource management
        config.setPoolName("HikariPool-" + dbName);
        config.setAutoCommit(true);
        config.setIsolateInternalQueries(false);
        
        return new HikariDataSource(config);
    }

    public static String getDatabaseName() {
        return properties.getProperty("database.name");
    }

    public static void executeUpdate(String query) throws SQLException {
        try (Connection connection = getConnection("");
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(query);
        }
    }

    public static void closeResources(Connection connection, Statement statement, ResultSet resultSet) {
        try {
            if (resultSet != null && !resultSet.isClosed()) {
                resultSet.close();
            }
            if (statement != null && !statement.isClosed()) {
                statement.close();
            }
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error closing resources", e);
        }
    }
    
    /**
     * Closes all cached DataSources to prevent resource leaks.
     * Should be called during application shutdown or test cleanup.
     */
    public static void closeAllDataSources() {
        LOGGER.info("Closing all cached DataSources");
        dataSourceCache.forEach((dbName, dataSource) -> {
            try {
                LOGGER.info("Closing DataSource for database: " + dbName);
                dataSource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing DataSource for " + dbName, e);
            }
        });
        dataSourceCache.clear();
    }
    
    /**
     * Closes a specific DataSource and removes it from cache.
     * Useful for cleaning up after individual test databases.
     */
    public static void closeDataSource(String dbName) {
        HikariDataSource dataSource = dataSourceCache.remove(dbName);
        if (dataSource != null) {
            try {
                LOGGER.info("Closing DataSource for database: " + dbName);
                dataSource.close();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Error closing DataSource for " + dbName, e);
            }
        }
    }
}