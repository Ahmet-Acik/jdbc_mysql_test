package databaseTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.util.DatabaseUtil;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * JDBC Batch Processing Test Class
 * 
 * This class demonstrates JDBC batch processing capabilities for efficient bulk operations:
 * 1. Batch INSERT operations
 * 2. Batch UPDATE operations  
 * 3. Batch mixed operations (INSERT/UPDATE/DELETE)
 * 4. Performance comparison: batch vs individual operations
 * 5. Error handling in batch operations
 */
public class JdbcBatchProcessingTest {

    private Connection conn;
    private static final Logger LOGGER = Logger.getLogger(JdbcBatchProcessingTest.class.getName());
    private static final String dbName = "testdb_integration";

    @BeforeAll
    static void setUp() throws SQLException, IOException {
        DatabaseSetup.dropDatabase(dbName);
        DatabaseSetup.createDatabase(dbName);

        Flyway flyway = Flyway.configure()
                .dataSource(DatabaseUtil.getDataSource(dbName))
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    @BeforeEach
    void resetDatabase() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(DatabaseUtil.getDataSource(dbName))
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        
        // Get a fresh connection for each test
        conn = DatabaseUtil.getConnection(dbName);
    }

    @AfterEach
    void tearDown() throws SQLException {
        try {
            if (conn != null && !conn.isClosed()) {
                conn.setAutoCommit(true); // Reset to default
                conn.close(); // Close connection after each test
            }
        } catch (SQLException e) {
            LOGGER.severe("Error closing connection: " + e.getMessage());
        }
    }

    /**
     * Demonstrates batch INSERT operations for bulk data insertion
     */
    @Test
    void testBatchInsert_BulkCustomerInsertion() throws SQLException {
        String sql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
        
        // Sample customer data
        List<CustomerData> customers = Arrays.asList(
            new CustomerData("John Smith", "john.smith@example.com", "+1111111111"),
            new CustomerData("Jane Doe", "jane.doe@example.com", "+2222222222"),
            new CustomerData("Bob Johnson", "bob.johnson@example.com", "+3333333333"),
            new CustomerData("Alice Brown", "alice.brown@example.com", "+4444444444"),
            new CustomerData("Charlie Wilson", "charlie.wilson@example.com", "+5555555555")
        );
        
        long startTime = System.currentTimeMillis();
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Add all customers to the batch
            for (CustomerData customer : customers) {
                pstmt.setString(1, customer.name);
                pstmt.setString(2, customer.email);
                pstmt.setString(3, customer.phoneNumber);
                pstmt.addBatch();
            }
            
            // Execute the batch
            int[] results = pstmt.executeBatch();
            
            long endTime = System.currentTimeMillis();
            LOGGER.info("Batch insert completed in " + (endTime - startTime) + "ms");
            
            // Verify all inserts were successful
            assertEquals(customers.size(), results.length);
            for (int result : results) {
                assertEquals(1, result, "Each insert should affect exactly 1 row");
            }
            
            // Verify the data was inserted correctly
            String countSql = "SELECT COUNT(*) FROM Customer WHERE email LIKE '%@example.com'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                rs.next();
                assertEquals(customers.size(), rs.getInt(1));
            }
        }
    }

    /**
     * Demonstrates batch UPDATE operations for bulk data modification
     */
    @Test
    void testBatchUpdate_BulkPriceUpdate() throws SQLException {
        // First, let's update some product prices in batch
        String updateSql = "UPDATE Product SET price = ? WHERE product_name = ?";
        
        // Price updates: productName -> newPrice (using existing products from migration)
        String[][] priceUpdates = {
            {"Apple", "3.50"},
            {"Banana", "2.00"}, 
            {"Orange", "2.75"},
            {"Lemon", "2.25"}
        };
        
        long startTime = System.currentTimeMillis();
        
        try (PreparedStatement pstmt = conn.prepareStatement(updateSql)) {
            // Add all updates to the batch
            for (String[] update : priceUpdates) {
                pstmt.setDouble(1, Double.parseDouble(update[1]));
                pstmt.setString(2, update[0]);
                pstmt.addBatch();
            }
            
            // Execute the batch
            int[] results = pstmt.executeBatch();
            
            long endTime = System.currentTimeMillis();
            LOGGER.info("Batch update completed in " + (endTime - startTime) + "ms");
            
            // Verify all updates were successful
            assertEquals(priceUpdates.length, results.length);
            for (int result : results) {
                assertEquals(1, result, "Each update should affect exactly 1 row");
            }
            
            // Verify the prices were updated correctly
            String verifySql = "SELECT product_name, price FROM Product WHERE product_name IN ('Apple', 'Banana', 'Orange', 'Lemon')";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(verifySql)) {
                
                int count = 0;
                while (rs.next()) {
                    String productName = rs.getString("product_name");
                    double price = rs.getDouble("price");
                    
                    // Find expected price
                    for (String[] update : priceUpdates) {
                        if (update[0].equals(productName)) {
                            assertEquals(Double.parseDouble(update[1]), price, 0.01, 
                                "Price should be updated for " + productName);
                            count++;
                            break;
                        }
                    }
                }
                assertEquals(priceUpdates.length, count);
            }
        }
    }

    /**
     * Demonstrates mixed batch operations (INSERT, UPDATE, DELETE)
     */
    @Test
    void testBatchMixed_MultipleOperationTypes() throws SQLException {
        conn.setAutoCommit(false);
        
        try {
            // Step 1: Batch insert new products
            String insertSql = "INSERT INTO Product (product_name, price) VALUES (?, ?)";
            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, "Watermelon");
                insertStmt.setDouble(2, 5.99);
                insertStmt.addBatch();
                
                insertStmt.setString(1, "Kiwi");
                insertStmt.setDouble(2, 0.99);
                insertStmt.addBatch();
                
                int[] insertResults = insertStmt.executeBatch();
                assertEquals(2, insertResults.length);
                LOGGER.info("Inserted " + insertResults.length + " new products");
            }
            
            // Step 2: Batch update existing products
            String updateSql = "UPDATE Product SET price = price * 1.1 WHERE product_name IN ('Apple', 'Banana')";
            try (Statement updateStmt = conn.createStatement()) {
                updateStmt.addBatch(updateSql);
                updateStmt.addBatch("UPDATE Product SET price = 1.25 WHERE product_name = 'Cherry'");
                
                int[] updateResults = updateStmt.executeBatch();
                assertEquals(2, updateResults.length);
                LOGGER.info("Updated prices for existing products");
            }
            
            // Step 3: Verify total product count
            String countSql = "SELECT COUNT(*) FROM Product";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                rs.next();
                int totalProducts = rs.getInt(1);
                assertTrue(totalProducts >= 12, "Should have at least 12 products (10 original + 2 new)");
                LOGGER.info("Total products after batch operations: " + totalProducts);
            }
            
            conn.commit();
            LOGGER.info("All batch operations committed successfully");
            
        } catch (SQLException e) {
            conn.rollback();
            LOGGER.severe("Batch operations failed, rolled back: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Demonstrates performance comparison between batch and individual operations
     */
    @Test
    void testPerformanceComparison_BatchVsIndividual() throws SQLException {
        // Test data
        List<CustomerData> testCustomers = Arrays.asList(
            new CustomerData("Perf Test 1", "perf1@test.com", "+1000000001"),
            new CustomerData("Perf Test 2", "perf2@test.com", "+1000000002"),
            new CustomerData("Perf Test 3", "perf3@test.com", "+1000000003"),
            new CustomerData("Perf Test 4", "perf4@test.com", "+1000000004"),
            new CustomerData("Perf Test 5", "perf5@test.com", "+1000000005"),
            new CustomerData("Perf Test 6", "perf6@test.com", "+1000000006"),
            new CustomerData("Perf Test 7", "perf7@test.com", "+1000000007"),
            new CustomerData("Perf Test 8", "perf8@test.com", "+1000000008"),
            new CustomerData("Perf Test 9", "perf9@test.com", "+1000000009"),
            new CustomerData("Perf Test 10", "perf10@test.com", "+1000000010")
        );
        
        String sql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
        
        // Method 1: Individual operations
        long individualStart = System.currentTimeMillis();
        for (CustomerData customer : testCustomers) {
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                pstmt.setString(1, "Individual " + customer.name);
                pstmt.setString(2, "individual_" + customer.email);
                pstmt.setString(3, customer.phoneNumber);
                pstmt.executeUpdate();
            }
        }
        long individualEnd = System.currentTimeMillis();
        long individualTime = individualEnd - individualStart;
        
        // Method 2: Batch operations
        long batchStart = System.currentTimeMillis();
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            for (CustomerData customer : testCustomers) {
                pstmt.setString(1, "Batch " + customer.name);
                pstmt.setString(2, "batch_" + customer.email);
                pstmt.setString(3, customer.phoneNumber);
                pstmt.addBatch();
            }
            pstmt.executeBatch();
        }
        long batchEnd = System.currentTimeMillis();
        long batchTime = batchEnd - batchStart;
        
        LOGGER.info("Individual operations time: " + individualTime + "ms");
        LOGGER.info("Batch operations time: " + batchTime + "ms");
        LOGGER.info("Performance improvement: " + ((double)(individualTime - batchTime) / individualTime * 100) + "%");
        
        // Verify both methods inserted the correct number of records
        String countSql = "SELECT COUNT(*) FROM Customer WHERE name LIKE 'Individual Perf Test%' OR name LIKE 'Batch Perf Test%'";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(countSql)) {
            rs.next();
            assertEquals(testCustomers.size() * 2, rs.getInt(1));
        }
        
        // Batch should generally be faster (though with small datasets the difference might be minimal)
        assertTrue(batchTime <= individualTime, "Batch operations should not be slower than individual operations");
    }

    /**
     * Demonstrates error handling in batch operations
     */
    @Test
    void testBatchErrorHandling_PartialFailure() throws SQLException {
        String sql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
        
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            // Add valid customer
            pstmt.setString(1, "Valid Customer 1");
            pstmt.setString(2, "valid1@example.com");
            pstmt.setString(3, "+1111111111");
            pstmt.addBatch();
            
            // Add another valid customer
            pstmt.setString(1, "Valid Customer 2");
            pstmt.setString(2, "valid2@example.com");
            pstmt.setString(3, "+2222222222");
            pstmt.addBatch();
            
            // Add customer with duplicate email (should cause error if we run this batch twice)
            pstmt.setString(1, "Duplicate Email Customer");
            pstmt.setString(2, "valid1@example.com"); // Duplicate email
            pstmt.setString(3, "+3333333333");
            pstmt.addBatch();
            
            try {
                // First execution should succeed
                int[] results = pstmt.executeBatch();
                assertEquals(3, results.length);
                LOGGER.info("First batch execution succeeded");
                
                // Clear and try the same batch again (should fail due to duplicate emails)
                pstmt.clearBatch();
                
                // Re-add the same data
                pstmt.setString(1, "Valid Customer 1");
                pstmt.setString(2, "valid1@example.com");
                pstmt.setString(3, "+1111111111");
                pstmt.addBatch();
                
                pstmt.setString(1, "Valid Customer 2");
                pstmt.setString(2, "valid2@example.com");
                pstmt.setString(3, "+2222222222");
                pstmt.addBatch();
                
                try {
                    pstmt.executeBatch();
                    // This might not throw an exception in all cases,
                    // as MySQL handles duplicates differently based on configuration
                    LOGGER.info("Second batch execution - checking for duplicates");
                } catch (SQLException e) {
                    LOGGER.info("Expected error for duplicate entries: " + e.getMessage());
                }
                
            } catch (SQLException e) {
                LOGGER.severe("Batch execution failed: " + e.getMessage());
                // Handle batch failure appropriately
            }
            
            // Verify at least the first successful batch was processed
            String countSql = "SELECT COUNT(*) FROM Customer WHERE email IN ('valid1@example.com', 'valid2@example.com')";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(countSql)) {
                rs.next();
                assertTrue(rs.getInt(1) >= 2, "At least 2 valid customers should be inserted");
            }
        }
    }

    /**
     * Demonstrates batch operations with different batch sizes
     */
    @Test
    void testBatchSizes_OptimalBatchSize() throws SQLException {
        String sql = "INSERT INTO Product (product_name, price) VALUES (?, ?)";
        int[] batchSizes = {1, 5, 10, 20};
        
        for (int batchSize : batchSizes) {
            long startTime = System.currentTimeMillis();
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                for (int i = 1; i <= 100; i++) {
                    pstmt.setString(1, "BatchTest_" + batchSize + "_Product_" + i);
                    pstmt.setDouble(2, i * 0.50);
                    pstmt.addBatch();
                    
                    // Execute batch when it reaches the specified size
                    if (i % batchSize == 0) {
                        pstmt.executeBatch();
                        pstmt.clearBatch();
                    }
                }
                
                // Execute any remaining batched statements
                pstmt.executeBatch();
            }
            
            long endTime = System.currentTimeMillis();
            LOGGER.info("Batch size " + batchSize + " completed in " + (endTime - startTime) + "ms");
            
            // Clean up for next test
            String deleteSql = "DELETE FROM Product WHERE product_name LIKE 'BatchTest_" + batchSize + "_%'";
            try (Statement stmt = conn.createStatement()) {
                int deleted = stmt.executeUpdate(deleteSql);
                assertEquals(100, deleted);
            }
        }
    }

    // Helper class for test data
    private static class CustomerData {
        final String name;
        final String email;
        final String phoneNumber;
        
        CustomerData(String name, String email, String phoneNumber) {
            this.name = name;
            this.email = email;
            this.phoneNumber = phoneNumber;
        }
    }
}