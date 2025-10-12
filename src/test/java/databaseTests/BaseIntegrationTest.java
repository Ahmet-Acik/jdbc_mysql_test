package databaseTests;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.util.DatabaseUtil;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

/**
 * Base class for all database integration tests.
 * Follows DRY principles by centralizing common test setup and teardown logic.
 * 
 * Key features:
 * - Shared connection pool management
 * - Automatic database setup and cleanup
 * - Proper resource management to prevent connection leaks
 * - Flyway integration for consistent schema management
 */
public abstract class BaseIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(BaseIntegrationTest.class.getName());
    protected static final String DB_NAME = "testdb_integration";
    
    // Shared DataSource to prevent connection pool exhaustion
    private static DataSource sharedDataSource;
    
    // Per-test connection instance
    protected Connection conn;

    /**
     * One-time setup for the entire test class.
     * Creates database and initializes shared connection pool.
     */
    @BeforeAll
    static void setUpClass() throws SQLException, IOException {
        LOGGER.info("Setting up test database and connection pool");
        
        // Drop and recreate database for clean state
        DatabaseSetup.dropDatabase(DB_NAME);
        DatabaseSetup.createDatabase(DB_NAME);
        
        // Initialize shared DataSource once
        sharedDataSource = DatabaseUtil.getDataSource(DB_NAME);
        
        // Run initial migration
        Flyway flyway = Flyway.configure()
                .dataSource(sharedDataSource)
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
        
        LOGGER.info("Test database setup completed");
    }

    /**
     * Setup before each test method.
     * Resets database to clean state and provides fresh connection.
     */
    @BeforeEach
    void setUpTest() throws SQLException {
        LOGGER.fine("Setting up individual test");
        
        // Clean and re-migrate database for test isolation
        Flyway flyway = Flyway.configure()
                .dataSource(sharedDataSource)
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        
        // Get fresh connection for this test
        conn = DatabaseUtil.getConnection(DB_NAME);
        
        // Allow subclasses to perform additional setup
        performAdditionalSetup();
    }

    /**
     * Cleanup after each test method.
     * Ensures proper connection cleanup to prevent resource leaks.
     */
    @AfterEach
    void tearDownTest() throws SQLException {
        LOGGER.fine("Cleaning up individual test");
        
        try {
            // Perform any subclass-specific cleanup
            performAdditionalTeardown();
        } finally {
            // Always ensure connection is closed
            if (conn != null && !conn.isClosed()) {
                conn.close();
            }
        }
    }

    /**
     * Final cleanup for the entire test class.
     * Closes shared connection pool.
     */
    @AfterAll
    static void tearDownClass() throws SQLException {
        LOGGER.info("Tearing down test database and connection pool");
        
        try {
            // Clean up database
            DatabaseSetup.dropDatabase(DB_NAME);
        } finally {
            // Close DataSource using utility method to ensure proper cleanup
            DatabaseUtil.closeDataSource(DB_NAME);
        }
        
        LOGGER.info("Test database teardown completed");
    }

    /**
     * Hook for subclasses to perform additional setup after database reset.
     * Called after Flyway migration but before test execution.
     */
    protected void performAdditionalSetup() throws SQLException {
        // Default implementation does nothing
    }

    /**
     * Hook for subclasses to perform additional teardown.
     * Called before connection is closed.
     */
    protected void performAdditionalTeardown() throws SQLException {
        // Default implementation does nothing
    }

    /**
     * Utility method to get shared DataSource instance.
     * Useful for tests that need direct DataSource access.
     */
    protected static DataSource getSharedDataSource() {
        return sharedDataSource;
    }

    /**
     * Utility method to execute SQL statements safely.
     * Handles common SQL execution patterns with proper error handling.
     */
    protected void executeSql(String sql) throws SQLException {
        try (var stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
    }

    /**
     * Utility method to count rows in a table.
     * Common operation across many tests.
     */
    protected int countRowsInTable(String tableName) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + tableName;
        try (var stmt = conn.createStatement();
             var rs = stmt.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Utility method to check if a table exists.
     * Useful for schema validation tests.
     */
    protected boolean tableExists(String tableName) throws SQLException {
        String sql = "SHOW TABLES LIKE ?";
        try (var pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tableName);
            try (var rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Utility method to reset auto-commit to default state.
     * Ensures tests don't interfere with each other's transaction state.
     */
    protected void resetConnection() throws SQLException {
        if (conn != null && !conn.isClosed()) {
            conn.setAutoCommit(true);
            // Rollback any uncommitted changes
            if (!conn.getAutoCommit()) {
                conn.rollback();
            }
        }
    }
}