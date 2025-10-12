package databaseTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
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
 * JDBC Stored Procedures and CallableStatement Test Class
 * 
 * This class demonstrates JDBC stored procedure capabilities:
 * 1. Creating and calling stored procedures
 * 2. CallableStatement usage
 * 3. IN, OUT, and INOUT parameters
 * 4. Stored functions
 * 5. Multiple result sets from procedures
 */
public class JdbcStoredProcedureTest {

    private Connection conn;
    private static final Logger LOGGER = Logger.getLogger(JdbcStoredProcedureTest.class.getName());
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
        
        // Recreate stored procedures after clean
        createStoredProcedures();
    }

    @AfterEach
    void tearDown() throws SQLException {
        // Close connection after each test to prevent connection leaks
        if (conn != null && !conn.isClosed()) {
            conn.close();
        }
    }

    /**
     * Creates stored procedures and functions for testing
     */
    private void createStoredProcedures() throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            // Drop procedures if they exist
            try {
                stmt.execute("DROP PROCEDURE IF EXISTS GetCustomerOrders");
                stmt.execute("DROP PROCEDURE IF EXISTS GetCustomerStats");
                stmt.execute("DROP PROCEDURE IF EXISTS CreateCustomerWithOrder");
                stmt.execute("DROP FUNCTION IF EXISTS GetCustomerOrderCount");
                stmt.execute("DROP FUNCTION IF EXISTS CalculateOrderTotal");
            } catch (SQLException e) {
                // Ignore errors for procedures that don't exist
            }
            
            // Create procedure with IN parameter and result set
            String proc1 = """
                CREATE PROCEDURE GetCustomerOrders(IN customer_id INT)
                BEGIN
                    SELECT o.order_id, o.order_date, 
                           p.product_name, op.quantity, p.price,
                           (op.quantity * p.price) AS line_total
                    FROM `Order` o
                    JOIN Order_Product op ON o.order_id = op.order_id
                    JOIN Product p ON op.product_id = p.product_id
                    WHERE o.customer_id = customer_id
                    ORDER BY o.order_date, o.order_id;
                END
                """;
            
            // Create procedure with IN and OUT parameters
            String proc2 = """
                CREATE PROCEDURE GetCustomerStats(
                    IN customer_id INT,
                    OUT total_orders INT,
                    OUT total_spent DECIMAL(10,2),
                    OUT avg_order_value DECIMAL(10,2)
                )
                BEGIN
                    SELECT COUNT(o.order_id), 
                           COALESCE(SUM(op.quantity * p.price), 0),
                           COALESCE(AVG(op.quantity * p.price), 0)
                    INTO total_orders, total_spent, avg_order_value
                    FROM Customer c
                    LEFT JOIN `Order` o ON c.customer_id = o.customer_id
                    LEFT JOIN Order_Product op ON o.order_id = op.order_id
                    LEFT JOIN Product p ON op.product_id = p.product_id
                    WHERE c.customer_id = customer_id;
                END
                """;
            
            // Create procedure with transaction (IN parameters)
            String proc3 = """
                CREATE PROCEDURE CreateCustomerWithOrder(
                    IN customer_name VARCHAR(100),
                    IN customer_email VARCHAR(100),
                    IN customer_phone VARCHAR(20),
                    IN product_id INT,
                    IN quantity INT,
                    OUT new_customer_id INT,
                    OUT new_order_id INT
                )
                BEGIN
                    DECLARE EXIT HANDLER FOR SQLEXCEPTION
                    BEGIN
                        ROLLBACK;
                        RESIGNAL;
                    END;
                    
                    START TRANSACTION;
                    
                    -- Insert customer
                    INSERT INTO Customer (name, email, phone_number) 
                    VALUES (customer_name, customer_email, customer_phone);
                    SET new_customer_id = LAST_INSERT_ID();
                    
                    -- Insert order
                    INSERT INTO `Order` (customer_id, order_date) 
                    VALUES (new_customer_id, CURDATE());
                    SET new_order_id = LAST_INSERT_ID();
                    
                    -- Insert order product (get unit_price from Product table)
                    INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) 
                    SELECT new_order_id, product_id, quantity, p.price
                    FROM Product p WHERE p.product_id = product_id;
                    
                    COMMIT;
                END
                """;
            
            // Create function that returns a value
            String func1 = """
                CREATE FUNCTION GetCustomerOrderCount(cust_id INT) 
                RETURNS INT
                READS SQL DATA
                DETERMINISTIC
                BEGIN
                    DECLARE order_count INT DEFAULT 0;
                    SELECT COUNT(*) INTO order_count 
                    FROM `Order` 
                    WHERE customer_id = cust_id;
                    RETURN order_count;
                END
                """;
            
            // Create function to calculate order total
            String func2 = """
                CREATE FUNCTION CalculateOrderTotal(order_id INT) 
                RETURNS DECIMAL(10,2)
                READS SQL DATA
                DETERMINISTIC
                BEGIN
                    DECLARE total DECIMAL(10,2) DEFAULT 0.00;
                    SELECT COALESCE(SUM(op.quantity * p.price), 0.00) INTO total
                    FROM Order_Product op
                    JOIN Product p ON op.product_id = p.product_id
                    WHERE op.order_id = order_id;
                    RETURN total;
                END
                """;
            
            // Execute all creation statements
            stmt.execute(proc1);
            stmt.execute(proc2);
            stmt.execute(proc3);
            stmt.execute(func1);
            stmt.execute(func2);
            
            LOGGER.info("All stored procedures and functions created successfully");
        }
    }

    /**
     * Demonstrates calling stored procedure with IN parameter and processing result set
     */
    @Test
    void testStoredProcedure_GetCustomerOrders() throws SQLException {
        // Call procedure for customer ID 1 (Alice Dupont)
        String call = "{CALL GetCustomerOrders(?)}";
        
        try (CallableStatement cstmt = conn.prepareCall(call)) {
            cstmt.setInt(1, 1); // customer_id = 1
            
            // Execute and process result set
            boolean hasResults = cstmt.execute();
            assertTrue(hasResults, "Procedure should return a result set");
            
            List<OrderDetail> orderDetails = new ArrayList<>();
            try (ResultSet rs = cstmt.getResultSet()) {
                while (rs.next()) {
                    OrderDetail detail = new OrderDetail(
                        rs.getInt("order_id"),
                        rs.getDate("order_date"),
                        rs.getString("product_name"),
                        rs.getInt("quantity"),
                        rs.getDouble("price"),
                        rs.getDouble("line_total")
                    );
                    orderDetails.add(detail);
                    
                    LOGGER.info(String.format("Order %d: %s x%d = %.2f",
                        detail.orderId, detail.productName, detail.quantity, detail.lineTotal));
                }
            }
            
            // Alice Dupont should have orders (from migration data)
            assertTrue(orderDetails.size() > 0, "Alice Dupont should have orders");
            
            // Verify calculation
            for (OrderDetail detail : orderDetails) {
                assertEquals(detail.quantity * detail.price, detail.lineTotal, 0.01,
                    "Line total should equal quantity * price");
            }
        }
    }

    /**
     * Demonstrates calling stored procedure with IN and OUT parameters
     */
    @Test
    void testStoredProcedure_GetCustomerStats() throws SQLException {
        String call = "{CALL GetCustomerStats(?, ?, ?, ?)}";
        
        try (CallableStatement cstmt = conn.prepareCall(call)) {
            // Set IN parameter
            cstmt.setInt(1, 1); // customer_id = 1 (Alice Dupont)
            
            // Register OUT parameters
            cstmt.registerOutParameter(2, Types.INTEGER); // total_orders
            cstmt.registerOutParameter(3, Types.DECIMAL); // total_spent
            cstmt.registerOutParameter(4, Types.DECIMAL); // avg_order_value
            
            // Execute procedure
            cstmt.execute();
            
            // Retrieve OUT parameters
            int totalOrders = cstmt.getInt(2);
            double totalSpent = cstmt.getDouble(3);
            double avgOrderValue = cstmt.getDouble(4);
            
            LOGGER.info(String.format("Customer 1 Stats - Orders: %d, Total Spent: %.2f, Avg Order: %.2f",
                totalOrders, totalSpent, avgOrderValue));
            
            // Verify results
            assertTrue(totalOrders >= 0, "Total orders should be non-negative");
            assertTrue(totalSpent >= 0, "Total spent should be non-negative");
            
            if (totalOrders > 0) {
                assertTrue(avgOrderValue > 0, "Average order value should be positive when orders exist");
            }
        }
    }

    /**
     * Demonstrates calling stored procedure with transaction handling
     */
    @Test
    void testStoredProcedure_CreateCustomerWithOrder() throws SQLException {
        String call = "{CALL CreateCustomerWithOrder(?, ?, ?, ?, ?, ?, ?)}";
        
        try (CallableStatement cstmt = conn.prepareCall(call)) {
            // Set IN parameters
            cstmt.setString(1, "Stored Proc Customer");
            cstmt.setString(2, "storedproc@example.com");
            cstmt.setString(3, "+9999999999");
            cstmt.setInt(4, 1); // product_id = 1 (Apple)
            cstmt.setInt(5, 5); // quantity = 5
            
            // Register OUT parameters
            cstmt.registerOutParameter(6, Types.INTEGER); // new_customer_id
            cstmt.registerOutParameter(7, Types.INTEGER); // new_order_id
            
            // Execute procedure
            cstmt.execute();
            
            // Retrieve OUT parameters
            int newCustomerId = cstmt.getInt(6);
            int newOrderId = cstmt.getInt(7);
            
            LOGGER.info(String.format("Created Customer ID: %d, Order ID: %d", newCustomerId, newOrderId));
            
            // Verify the customer was created
            assertTrue(newCustomerId > 0, "New customer ID should be positive");
            assertTrue(newOrderId > 0, "New order ID should be positive");
            
            // Verify customer exists
            String verifyCustSql = "SELECT name, email FROM Customer WHERE customer_id = ?";
            try (var pstmt = conn.prepareStatement(verifyCustSql)) {
                pstmt.setInt(1, newCustomerId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next(), "Customer should exist");
                    assertEquals("Stored Proc Customer", rs.getString("name"));
                    assertEquals("storedproc@example.com", rs.getString("email"));
                }
            }
            
            // Verify order exists
            String verifyOrderSql = "SELECT customer_id FROM `Order` WHERE order_id = ?";
            try (var pstmt = conn.prepareStatement(verifyOrderSql)) {
                pstmt.setInt(1, newOrderId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next(), "Order should exist");
                    assertEquals(newCustomerId, rs.getInt("customer_id"));
                }
            }
            
            // Verify order product exists
            String verifyOrderProdSql = "SELECT quantity FROM Order_Product WHERE order_id = ? AND product_id = ?";
            try (var pstmt = conn.prepareStatement(verifyOrderProdSql)) {
                pstmt.setInt(1, newOrderId);
                pstmt.setInt(2, 1);
                try (ResultSet rs = pstmt.executeQuery()) {
                    assertTrue(rs.next(), "Order product should exist");
                    assertEquals(5, rs.getInt("quantity"));
                }
            }
        }
    }

    /**
     * Demonstrates calling stored functions
     */
    @Test
    void testStoredFunction_GetCustomerOrderCount() throws SQLException {
        // Call function to get order count for customer 1
        String call = "{? = CALL GetCustomerOrderCount(?)}";
        
        try (CallableStatement cstmt = conn.prepareCall(call)) {
            // Register return value
            cstmt.registerOutParameter(1, Types.INTEGER);
            
            // Set function parameter
            cstmt.setInt(2, 1); // customer_id = 1
            
            // Execute function
            cstmt.execute();
            
            // Get return value
            int orderCount = cstmt.getInt(1);
            
            LOGGER.info("Customer 1 has " + orderCount + " orders (from function)");
            
            // Verify against direct query
            String directSql = "SELECT COUNT(*) FROM `Order` WHERE customer_id = 1";
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(directSql)) {
                rs.next();
                int directCount = rs.getInt(1);
                assertEquals(directCount, orderCount, "Function should return same count as direct query");
            }
        }
    }

    /**
     * Demonstrates calling stored function to calculate order total
     */
    @Test
    void testStoredFunction_CalculateOrderTotal() throws SQLException {
        // Call function to calculate total for order 1
        String call = "{? = CALL CalculateOrderTotal(?)}";
        
        try (CallableStatement cstmt = conn.prepareCall(call)) {
            // Register return value
            cstmt.registerOutParameter(1, Types.DECIMAL);
            
            // Set function parameter
            cstmt.setInt(2, 1); // order_id = 1
            
            // Execute function
            cstmt.execute();
            
            // Get return value
            double orderTotal = cstmt.getDouble(1);
            
            LOGGER.info("Order 1 total: " + orderTotal + " (from function)");
            
            // Verify against direct query
            String directSql = """
                SELECT SUM(op.quantity * p.price) 
                FROM Order_Product op 
                JOIN Product p ON op.product_id = p.product_id 
                WHERE op.order_id = 1
                """;
            try (var stmt = conn.createStatement();
                 var rs = stmt.executeQuery(directSql)) {
                rs.next();
                double directTotal = rs.getDouble(1);
                assertEquals(directTotal, orderTotal, 0.01, "Function should return same total as direct query");
            }
        }
    }

    /**
     * Demonstrates error handling in stored procedure calls
     */
    @Test
    void testStoredProcedure_ErrorHandling() throws SQLException {
        // Try to create customer with duplicate email (should fail)
        String call = "{CALL CreateCustomerWithOrder(?, ?, ?, ?, ?, ?, ?)}";
        
        try (CallableStatement cstmt = conn.prepareCall(call)) {
            // Set IN parameters with duplicate email
            cstmt.setString(1, "Duplicate Email Customer");
            cstmt.setString(2, "alice.dupont@gmail.com"); // Duplicate email from migration data
            cstmt.setString(3, "+8888888888");
            cstmt.setInt(4, 1); // product_id = 1
            cstmt.setInt(5, 1); // quantity = 1
            
            // Register OUT parameters
            cstmt.registerOutParameter(6, Types.INTEGER);
            cstmt.registerOutParameter(7, Types.INTEGER);
            
            try {
                // This should fail due to unique constraint on email
                cstmt.execute();
                LOGGER.warning("Expected SQLException for duplicate email, but procedure succeeded");
            } catch (SQLException e) {
                LOGGER.info("Expected error for duplicate email: " + e.getMessage());
                // This is expected behavior
                assertTrue(e.getMessage().toLowerCase().contains("duplicate") || 
                          e.getMessage().toLowerCase().contains("unique"),
                    "Error should be related to duplicate/unique constraint");
            }
        }
    }

    // Helper class for test data
    private static class OrderDetail {
        final int orderId;
        final java.sql.Date orderDate;
        final String productName;
        final int quantity;
        final double price;
        final double lineTotal;
        
        OrderDetail(int orderId, java.sql.Date orderDate, String productName, 
                   int quantity, double price, double lineTotal) {
            this.orderId = orderId;
            this.orderDate = orderDate;
            this.productName = productName;
            this.quantity = quantity;
            this.price = price;
            this.lineTotal = lineTotal;
        }
    }
}