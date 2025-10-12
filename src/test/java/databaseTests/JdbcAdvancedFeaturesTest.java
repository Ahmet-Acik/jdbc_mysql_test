package databaseTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Savepoint;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

/**
 * Advanced JDBC Features Test Class
 * Extends BaseIntegrationTest for DRY principles - eliminates duplicate setup/teardown code.
 * 
 * This class demonstrates advanced JDBC features:
 * 1. PreparedStatement usage with parameters
 * 2. Transaction management (commit/rollback)
 * 3. Savepoints for nested transactions
 * 4. Database metadata inspection
 * 5. ResultSet metadata analysis
 */
public class JdbcAdvancedFeaturesTest extends BaseIntegrationTest {

    private static final Logger LOGGER = Logger.getLogger(JdbcAdvancedFeaturesTest.class.getName());

    @Override
    protected void performAdditionalTeardown() throws SQLException {
        // Reset connection to auto-commit mode for advanced features tests
        resetConnection();
    }

    /**
     * Demonstrates PreparedStatement usage for secure parameterized queries
     * This prevents SQL injection attacks and improves performance
     */
    @Test
    void testPreparedStatement_CustomerSearchWithParameters() throws SQLException {
        // Search for customers with names containing "Alice" and specific email
        String sql = "SELECT customer_id, name, email FROM Customer WHERE name LIKE ? AND email = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Set parameters safely - no SQL injection risk
            pstmt.setString(1, "%Alice%");
            pstmt.setString(2, "alice.dupont@gmail.com");
            
            try (ResultSet rs = pstmt.executeQuery()) {
                List<String> customerNames = new ArrayList<>();
                while (rs.next()) {
                    customerNames.add(rs.getString("name"));
                    LOGGER.info(String.format("Found customer: %s (ID: %d, Email: %s)",
                        rs.getString("name"), rs.getInt("customer_id"), rs.getString("email")));
                }
                
                assertEquals(1, customerNames.size());
                assertTrue(customerNames.get(0).contains("Alice"));
            }
        }
    }

    /**
     * Demonstrates PreparedStatement for INSERT operations with auto-generated keys
     */
    @Test
    void testPreparedStatement_InsertWithGeneratedKeys() throws SQLException {
        String sql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql, PreparedStatement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, "John Doe");
            pstmt.setString(2, "john.doe@example.com");
            pstmt.setString(3, "+1234567890");
            
            int affectedRows = pstmt.executeUpdate();
            assertEquals(1, affectedRows);
            
            // Retrieve the auto-generated customer_id
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                assertTrue(generatedKeys.next());
                int newCustomerId = generatedKeys.getInt(1);
                assertTrue(newCustomerId > 0);
                LOGGER.info("New customer created with ID: " + newCustomerId);
            }
        }
    }

    /**
     * Demonstrates manual transaction management with commit/rollback
     */
    @Test
    void testTransactionManagement_CommitAndRollback() throws SQLException {
        // Save original auto-commit state
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            // Start manual transaction
            conn.setAutoCommit(false);
            
            // First transaction: Insert a new customer and commit
            String insertCustomerSql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertCustomerSql)) {
                pstmt.setString(1, "Transaction Customer");
                pstmt.setString(2, "transaction@example.com");
                pstmt.setString(3, "+9876543210");
                pstmt.executeUpdate();
                
                // Commit the transaction
                conn.commit();
                LOGGER.info("Transaction committed successfully");
            }
            
            // Verify customer was inserted
            String countSql = "SELECT COUNT(*) FROM Customer WHERE email = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
                pstmt.setString(1, "transaction@example.com");
                try (ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    assertEquals(1, rs.getInt(1), "Customer should be committed");
                }
            }
            
            // Second transaction: Insert another customer but rollback
            try (PreparedStatement pstmt = conn.prepareStatement(insertCustomerSql)) {
                pstmt.setString(1, "Rollback Customer");
                pstmt.setString(2, "rollback@example.com");
                pstmt.setString(3, "+1111111111");
                pstmt.executeUpdate();
                
                // Rollback the transaction
                conn.rollback();
                LOGGER.info("Transaction rolled back successfully");
            }
            
            // Verify customer was NOT inserted due to rollback
            try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
                pstmt.setString(1, "rollback@example.com");
                try (ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    assertEquals(0, rs.getInt(1), "Customer should be rolled back");
                }
            }
            
        } finally {
            // Restore original auto-commit state
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Demonstrates advanced transaction management with Savepoints
     */
    @Test
    void testSavepoints_PartialRollback() throws SQLException {
        boolean originalAutoCommit = conn.getAutoCommit();
        
        try {
            conn.setAutoCommit(false);
            
            // Insert first customer
            String insertSql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "Customer 1");
                pstmt.setString(2, "customer1@example.com");
                pstmt.setString(3, "+1111111111");
                pstmt.executeUpdate();
            }
            
            // Create savepoint after first insert
            Savepoint savepoint1 = conn.setSavepoint("AfterFirstCustomer");
            LOGGER.info("Savepoint created after first customer");
            
            // Insert second customer
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "Customer 2");
                pstmt.setString(2, "customer2@example.com");
                pstmt.setString(3, "+2222222222");
                pstmt.executeUpdate();
            }
            
            // Create another savepoint
            Savepoint savepoint2 = conn.setSavepoint("AfterSecondCustomer");
            LOGGER.info("Savepoint created after second customer");
            
            // Insert third customer
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, "Customer 3");
                pstmt.setString(2, "customer3@example.com");
                pstmt.setString(3, "+3333333333");
                pstmt.executeUpdate();
            }
            
            // Rollback to savepoint2 (removes only Customer 3)
            conn.rollback(savepoint2);
            LOGGER.info("Rolled back to savepoint2 - Customer 3 removed");
            
            // Commit the transaction (saves Customer 1 and 2)
            conn.commit();
            
            // Verify the results
            String countSql = "SELECT COUNT(*) FROM Customer WHERE email IN (?, ?, ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(countSql)) {
                pstmt.setString(1, "customer1@example.com");
                pstmt.setString(2, "customer2@example.com");
                pstmt.setString(3, "customer3@example.com");
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    rs.next();
                    assertEquals(2, rs.getInt(1), "Should have 2 customers (3rd was rolled back)");
                }
            }
            
        } finally {
            conn.setAutoCommit(originalAutoCommit);
        }
    }

    /**
     * Demonstrates database metadata inspection
     */
    @Test
    void testDatabaseMetadata_InspectDatabaseInfo() throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        
        // Get basic database information
        String databaseName = metaData.getDatabaseProductName();
        String databaseVersion = metaData.getDatabaseProductVersion();
        String driverName = metaData.getDriverName();
        String driverVersion = metaData.getDriverVersion();
        
        LOGGER.info(String.format("Database: %s %s", databaseName, databaseVersion));
        LOGGER.info(String.format("Driver: %s %s", driverName, driverVersion));
        
        assertTrue(databaseName.toLowerCase().contains("mysql"));
        assertTrue(driverName.toLowerCase().contains("mysql"));
        
        // Get supported features
        boolean supportsTransactions = metaData.supportsTransactions();
        boolean supportsSavepoints = metaData.supportsSavepoints();
        boolean supportsBatchUpdates = metaData.supportsBatchUpdates();
        
        LOGGER.info("Supports transactions: " + supportsTransactions);
        LOGGER.info("Supports savepoints: " + supportsSavepoints);
        LOGGER.info("Supports batch updates: " + supportsBatchUpdates);
        
        assertTrue(supportsTransactions);
        assertTrue(supportsBatchUpdates);
    }

    /**
     * Demonstrates table and column metadata inspection
     */
    @Test
    void testDatabaseMetadata_InspectTableStructure() throws SQLException {
        DatabaseMetaData metaData = conn.getMetaData();
        
        // Get all tables in the database
        try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
            List<String> tableNames = new ArrayList<>();
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                tableNames.add(tableName);
                LOGGER.info("Found table: " + tableName);
            }
            
            assertTrue(tableNames.contains("Customer"));
            assertTrue(tableNames.contains("Product"));
            assertTrue(tableNames.contains("Order"));
        }
        
        // Get columns for Customer table
        try (ResultSet columns = metaData.getColumns(null, null, "Customer", "%")) {
            List<String> columnNames = new ArrayList<>();
            while (columns.next()) {
                String columnName = columns.getString("COLUMN_NAME");
                String dataType = columns.getString("TYPE_NAME");
                int columnSize = columns.getInt("COLUMN_SIZE");
                boolean nullable = columns.getBoolean("NULLABLE");
                
                columnNames.add(columnName);
                LOGGER.info(String.format("Column: %s, Type: %s, Size: %d, Nullable: %b",
                    columnName, dataType, columnSize, nullable));
            }
            
            assertTrue(columnNames.contains("customer_id"));
            assertTrue(columnNames.contains("name"));
            assertTrue(columnNames.contains("email"));
        }
    }

    /**
     * Demonstrates ResultSet metadata analysis
     */
    @Test
    void testResultSetMetadata_AnalyzeQueryResults() throws SQLException {
        String sql = "SELECT c.customer_id, c.name, c.email, COUNT(o.order_id) as order_count " +
                     "FROM Customer c LEFT JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "GROUP BY c.customer_id, c.name, c.email";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql);
             ResultSet rs = pstmt.executeQuery()) {
            
            // Get ResultSet metadata
            var rsMetaData = rs.getMetaData();
            int columnCount = rsMetaData.getColumnCount();
            
            LOGGER.info("Query returned " + columnCount + " columns:");
            
            // Analyze each column
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rsMetaData.getColumnName(i);
                String columnLabel = rsMetaData.getColumnLabel(i);
                String typeName = rsMetaData.getColumnTypeName(i);
                int sqlType = rsMetaData.getColumnType(i);
                boolean nullable = rsMetaData.isNullable(i) == 1;
                
                LOGGER.info(String.format("Column %d: %s (%s) - Type: %s (%d), Nullable: %b",
                    i, columnName, columnLabel, typeName, sqlType, nullable));
            }
            
            assertEquals(4, columnCount);
            assertEquals("customer_id", rsMetaData.getColumnName(1));
            assertEquals("name", rsMetaData.getColumnName(2));
            assertEquals("email", rsMetaData.getColumnName(3));
            assertEquals("order_count", rsMetaData.getColumnLabel(4));
            
            // Verify we have data
            assertTrue(rs.next());
        }
    }

    /**
     * Demonstrates prepared statement reuse for better performance
     */
    @Test
    void testPreparedStatement_ReuseForPerformance() throws SQLException {
        String sql = "SELECT name FROM Customer WHERE customer_id = ?";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Execute the same prepared statement multiple times with different parameters
            int[] customerIds = {1, 2, 3, 4, 5};
            List<String> customerNames = new ArrayList<>();
            
            for (int customerId : customerIds) {
                pstmt.setInt(1, customerId);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String customerName = rs.getString("name");
                        customerNames.add(customerName);
                        LOGGER.info("Customer ID " + customerId + ": " + customerName);
                    }
                }
            }
            
            assertEquals(5, customerNames.size());
            LOGGER.info("PreparedStatement reused " + customerIds.length + " times efficiently");
        }
    }
}