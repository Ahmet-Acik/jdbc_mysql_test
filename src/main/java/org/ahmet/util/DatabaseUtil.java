// src/main/java/org/ahmet/util/DatabaseUtil.java
package org.ahmet.util;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseUtil {
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final Properties properties = new Properties();

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

    public static DataSource getDataSource(String dbName) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/" + properties.getProperty("database.name"));
        config.setUsername(properties.getProperty("database.user"));
        config.setPassword(properties.getProperty("database.password"));
        config.setMaximumPoolSize(5); // Reduce the maximum pool size
        config.setConnectionTimeout(30000); // 30 seconds
        config.setIdleTimeout(60000); // 60 seconds
        config.setMaxLifetime(1800000); // 30 minutes
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
}