package org.ahmet.database;

import org.ahmet.config.DatabaseConfig;
import org.flywaydb.core.Flyway;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Database setup utility class for creating databases, tables and running migrations.
 * Uses Flyway for database migrations and proper prepared statements for security.
 */
public class DatabaseSetup {
    private static final Logger LOGGER = Logger.getLogger(DatabaseSetup.class.getName());

    /**
     * Drops a database if it exists.
     * @param dbName Database name to drop
     * @throws SQLException if database drop fails
     */
    public static void dropDatabase(String dbName) throws SQLException {
        String dropQuery = "DROP DATABASE IF EXISTS " + dbName;
        try (Connection conn = getSystemConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(dropQuery);
            LOGGER.info("Database " + dbName + " dropped successfully");
        }
    }

    /**
     * Creates a database if it doesn't exist.
     * @param dbName Database name to create
     * @throws SQLException if database creation fails
     */
    public static void createDatabase(String dbName) throws SQLException {
        String createQuery = "CREATE DATABASE IF NOT EXISTS " + dbName;
        try (Connection conn = getSystemConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(createQuery);
            LOGGER.info("Database " + dbName + " created successfully");
        }
    }

    /**
     * Checks if a table exists in the specified database.
     * @param dbName Database name
     * @param tableName Table name
     * @return true if table exists
     * @throws SQLException if check fails
     */
    public static boolean tableExists(String dbName, String tableName) throws SQLException {
        String checkQuery = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = ? AND table_name = ?";
        try (Connection connection = DatabaseConfig.getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement(checkQuery)) {
            statement.setString(1, dbName);
            statement.setString(2, tableName);
            ResultSet rs = statement.executeQuery();
            rs.next();
            return rs.getInt(1) > 0;
        }
    }

    /**
     * Runs Flyway migrations to set up the database schema.
     * @param dataSource DataSource to use for migrations
     */
    public static void runMigrations(DataSource dataSource) {
        try {
            Flyway flyway = Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
            
            flyway.migrate();
            LOGGER.info("Database migrations completed successfully");
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error running database migrations", e);
            throw new RuntimeException("Migration failed", e);
        }
    }

    /**
     * Gets a system connection (without specifying a database) for admin operations.
     * @return Database connection
     * @throws SQLException if connection fails
     */
    private static Connection getSystemConnection() throws SQLException {
        // Create a temporary data source for system operations
        String url = "jdbc:mysql://localhost:3306/?useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";
        return java.sql.DriverManager.getConnection(url, "root", "root7623"); // This should also be configurable
    }

    /**
     * Initializes the entire database: creates database, runs migrations.
     * @param dbName Database name
     */
    public static void initializeDatabase(String dbName) {
        try {
            createDatabase(dbName);
            runMigrations(DatabaseConfig.getDataSource());
            LOGGER.info("Database initialization completed successfully");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error during database initialization", e);
            throw new RuntimeException("Database initialization failed", e);
        }
    }

    public static void main(String[] args) {
        String dbName = DatabaseConfig.getDatabaseName();
        initializeDatabase(dbName);
    }
}