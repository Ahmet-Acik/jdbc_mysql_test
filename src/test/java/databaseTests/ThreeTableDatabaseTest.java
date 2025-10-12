// src/test/java/databaseTests/ThreeTableDatabaseTest.java
package databaseTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Complex multi-table operations tests using JDBC.
 * Extends BaseIntegrationTest for DRY principles - eliminates duplicate setup/teardown code.
 * 
 * Tests validate:
 * - Multi-table operations and joins
 * - Foreign key relationships and referential integrity  
 * - Complex transaction scenarios
 * - Order-Product relationship management
 */
class ThreeTableDatabaseTest extends BaseIntegrationTest {

    private Statement stmt;

    @Override
    protected void performAdditionalSetup() throws SQLException {
        // Create statement for this test class
        stmt = conn.createStatement();
    }

    @Override
    protected void performAdditionalTeardown() throws SQLException {
        // Clean up statement
        if (stmt != null && !stmt.isClosed()) {
            stmt.close();
        }
    }

    static Stream<Arguments> provideCustomerData() {
        return Stream.of(
                Arguments.of("John Doe", "john.doe@example.com", "555-1234"),
                Arguments.of("Jane Doe", "jane.doe@example.com", "555-5678")
        );
    }

    static Stream<Arguments> provideInvalidOrderData() {
        return Stream.of(
                Arguments.of(Date.valueOf("2023-08-22"), 999)  // Non-existent customer_id
        );
    }

    static Stream<Arguments> provideInvalidOrderProductData() {
        return Stream.of(
                Arguments.of(999, 1, 1),  // Non-existent order_id, assuming product_id = 1 exists
                Arguments.of(1, 999, 1)   // Assuming order_id = 1 exists, non-existent product_id
        );
    }

    /**
     * Clear all data from tables while preserving schema.
     * Used by individual test methods that need clean state.
     */
    private void clearAllTables() throws SQLException {
        stmt.executeUpdate("DELETE FROM Order_Product");
        stmt.executeUpdate("DELETE FROM `Order`");
        stmt.executeUpdate("DELETE FROM Product");
        stmt.executeUpdate("DELETE FROM Customer");
    }

    // Test for Creating and Reading a Customer
    @Test
    @DisplayName("Test for Creating and Reading a Customer")
    void testCreateAndReadCustomer() throws SQLException {
        // Insert customer (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)");
        ps.setString(1, "John Doe");
        ps.setString(2, "john.doe@example.com");
        ps.setString(3, "555-1234");
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the customer (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customer WHERE email = 'john.doe@example.com'");
        assertTrue(rs.next(), "Customer should exist in the database");
        assertEquals("John Doe", rs.getString("name"));
        assertEquals("john.doe@example.com", rs.getString("email"));
        assertEquals("555-1234", rs.getString("phone_number"));

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @CsvSource({
            "John Doe, john.doe@example.com, 555-1234",
            "Jane Doe, jane.doe@example.com, 555-5678"
    })
    @DisplayName("Test for Creating and Reading a Customer")
    void testCreateAndReadCustomer(String name, String email, String phoneNumber) throws SQLException {
        // Insert customer (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, phoneNumber);
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the customer (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customer WHERE email = '" + email + "'");
        assertTrue(rs.next(), "Customer should exist in the database");
        assertEquals(name, rs.getString("name"));
        assertEquals(email, rs.getString("email"));
        assertEquals(phoneNumber, rs.getString("phone_number"));

        rs.close();
        ps.close();
    }

    // Test for Creating and Reading a Product
    @Test
    @DisplayName("Test for Creating and Reading a Product")
    void testCreateAndReadProduct() throws SQLException {
        // Insert product (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Product (product_name, price) VALUES (?, ?)");
        ps.setString(1, "Laptop");
        ps.setBigDecimal(2, new java.math.BigDecimal("799.99"));
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the product (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Product WHERE product_name = 'Laptop'");
        assertTrue(rs.next(), "Product should exist in the database");
        assertEquals("Laptop", rs.getString("product_name"));
        assertEquals(799.99, rs.getBigDecimal("price").doubleValue());

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @CsvSource({
            "Laptop, 799.99",
            "Smartphone, 499.99"
    })
    @DisplayName("Test for Creating and Reading a Product")
    void testCreate_AndReadProduct(String productName, double price) throws SQLException {
        // Insert product (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Product (product_name, price) VALUES (?, ?)");
        ps.setString(1, productName);
        ps.setBigDecimal(2, new java.math.BigDecimal(price));
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the product (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Product WHERE product_name = '" + productName + "'");
        assertTrue(rs.next(), "Product should exist in the database");
        assertEquals(productName, rs.getString("product_name"));
        assertEquals(price, rs.getBigDecimal("price").doubleValue());

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/customer_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Creating and Reading a Customer")
    void testCreate_AndReadCustomer(String name, String email, String phoneNumber) throws SQLException {
        // Insert customer (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, phoneNumber);
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the customer (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customer WHERE email = '" + email + "'");
        assertTrue(rs.next(), "Customer should exist in the database");
        assertEquals(name, rs.getString("name"));
        assertEquals(email, rs.getString("email"));
        assertEquals(phoneNumber, rs.getString("phone_number"));

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/product_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Creating and Reading a Product")
    void testCreateAndReadProduct(String productName, double price) throws SQLException {
        // Insert product (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Product (product_name, price) VALUES (?, ?)");
        ps.setString(1, productName);
        ps.setBigDecimal(2, new java.math.BigDecimal(price));
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the product (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Product WHERE product_name = '" + productName + "'");
        assertTrue(rs.next(), "Product should exist in the database");
        assertEquals(productName, rs.getString("product_name"));
        assertEquals(price, rs.getBigDecimal("price").doubleValue());

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @MethodSource("provideCustomerData")
    @DisplayName("Test for Creating and Reading a Customer using Method Source")
    void testCreateAndReadCustomerMethodSource(String name, String email, String phoneNumber) throws SQLException {
        // Insert customer (Create)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, phoneNumber);
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be inserted");

        // Query for the customer (Read)
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customer WHERE email = '" + email + "'");
        assertTrue(rs.next(), "Customer should exist in the database");
        assertEquals(name, rs.getString("name"));
        assertEquals(email, rs.getString("email"));
        assertEquals(phoneNumber, rs.getString("phone_number"));

        rs.close();
        ps.close();
    }

    // Test for Creating an Order and Linking it to a Customer and Product
    @Test
    @DisplayName("Test for Creating an Order and Linking it to a Customer and Product")
    void testCreateOrderAndLinkProduct() throws SQLException {
        // Insert a Customer
        stmt.executeUpdate("INSERT INTO Customer (name, email, phone_number) VALUES ('Jane Smith', 'jane.smith@example.com', '555-4321')");

        // Insert Products and retrieve their IDs
        stmt.executeUpdate("INSERT INTO Product (product_name, price) VALUES ('Smartphone', 499.99)", Statement.RETURN_GENERATED_KEYS);
        ResultSet rs = stmt.getGeneratedKeys();
        assertTrue(rs.next(), "Product ID should be generated for 'Smartphone'");
        int smartphoneId = rs.getInt(1);
        rs.close();

        stmt.executeUpdate("INSERT INTO Product (product_name, price) VALUES ('Tablet', 299.99)", Statement.RETURN_GENERATED_KEYS);
        rs = stmt.getGeneratedKeys();
        assertTrue(rs.next(), "Product ID should be generated for 'Tablet'");
        int tabletId = rs.getInt(1);
        rs.close();

        // Insert an Order (Link to Customer)
        PreparedStatement ps = conn.prepareStatement("INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        ps.setDate(1, Date.valueOf("2023-08-20"));
        ps.setInt(2, 1);  // Assuming customer_id = 1 for 'Jane Smith'
        ps.executeUpdate();

        // Retrieve the generated order_id
        rs = ps.getGeneratedKeys();
        assertTrue(rs.next(), "Order ID should be generated");
        int orderId = rs.getInt(1);
        rs.close();
        ps.close();

        // Insert Products into the Order_Product table (Link Products to Order)
        ps = conn.prepareStatement("INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
        ps.setInt(1, orderId);  // Use the generated order_id
        ps.setInt(2, smartphoneId);  // Use the generated product_id for 'Smartphone'
        ps.setInt(3, 2);  // Ordered 2 Smartphones
        ps.setBigDecimal(4, new java.math.BigDecimal("499.99"));  // Unit price for Smartphone
        ps.executeUpdate();

        ps.setInt(1, orderId);
        ps.setInt(2, tabletId);  // Use the generated product_id for 'Tablet'
        ps.setInt(3, 1);  // Ordered 1 Tablet
        ps.setBigDecimal(4, new java.math.BigDecimal("299.99"));  // Unit price for Tablet
        ps.executeUpdate();

        // Query for the Order_Product details
        rs = stmt.executeQuery("SELECT * FROM Order_Product WHERE order_id = " + orderId);
        assertTrue(rs.next(), "First Product should exist in the order");
        assertEquals(orderId, rs.getInt("order_id"));
        assertEquals(smartphoneId, rs.getInt("product_id"));
        assertEquals(2, rs.getInt("quantity"));

        assertTrue(rs.next(), "Second Product should exist in the order");
        assertEquals(orderId, rs.getInt("order_id"));
        assertEquals(tabletId, rs.getInt("product_id"));
        assertEquals(1, rs.getInt("quantity"));

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/order_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Creating an Order and Linking it to a Customer and Product")
    void testCreateOrderAndLinkProduct(String orderDate, String customerName, String customerEmail, String customerPhone, String productName, double productPrice, int quantity) throws SQLException {
        // Insert a Customer
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, customerName);
        ps.setString(2, customerEmail);
        ps.setString(3, customerPhone);
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        int customerId = rs.getInt(1);
        rs.close();
        ps.close();

        // Insert Product and retrieve its ID
        ps = conn.prepareStatement("INSERT INTO Product (product_name, price) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, productName);
        ps.setBigDecimal(2, new java.math.BigDecimal(productPrice));
        ps.executeUpdate();
        rs = ps.getGeneratedKeys();
        rs.next();
        int productId = rs.getInt(1);
        rs.close();
        ps.close();

        // Insert an Order (Link to Customer)
        ps = conn.prepareStatement("INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)", Statement.RETURN_GENERATED_KEYS);
        ps.setDate(1, Date.valueOf(orderDate));
        ps.setInt(2, customerId);
        ps.executeUpdate();
        rs = ps.getGeneratedKeys();
        rs.next();
        int orderId = rs.getInt(1);
        rs.close();
        ps.close();

        // Insert Product into the Order_Product table (Link Product to Order)
        ps = conn.prepareStatement("INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
        ps.setInt(1, orderId);
        ps.setInt(2, productId);
        ps.setInt(3, quantity);
        ps.setBigDecimal(4, new java.math.BigDecimal(String.valueOf(productPrice)));  // Use the product price as unit price
        ps.executeUpdate();
        ps.close();

        // Query for the Order_Product details
        rs = stmt.executeQuery("SELECT * FROM Order_Product WHERE order_id = " + orderId);
        assertTrue(rs.next(), "Product should exist in the order");
        assertEquals(orderId, rs.getInt("order_id"));
        assertEquals(productId, rs.getInt("product_id"));
        assertEquals(quantity, rs.getInt("quantity"));

        rs.close();
    }

    // Test for Schema and Foreign Key Constraints (Cascade Delete)
    @Test
    @DisplayName("Test for Schema and Foreign Key Constraints (Cascade Delete)")
    void testForeignKeyCascadeDelete() throws SQLException {
        // Insert a Customer and retrieve the generated customer_id
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)", Statement.RETURN_GENERATED_KEYS);
        ps.setString(1, "Test User");
        ps.setString(2, "test.user@example.com");
        ps.setString(3, "555-9876");
        ps.executeUpdate();
        ResultSet rs = ps.getGeneratedKeys();
        rs.next();
        int customerId = rs.getInt(1);
        rs.close();
        ps.close();

        // Insert an Order linked to the customer
        ps = conn.prepareStatement("INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)");
        ps.setDate(1, Date.valueOf("2023-08-21"));
        ps.setInt(2, customerId);
        ps.executeUpdate();
        ps.close();

        // Delete the customer, which should cascade delete the linked orders
        int rowsAffected = stmt.executeUpdate("DELETE FROM Customer WHERE customer_id = " + customerId);
        assertEquals(1, rowsAffected, "1 customer should be deleted");

        // Verify the order was also deleted due to cascading
        rs = stmt.executeQuery("SELECT * FROM `Order` WHERE customer_id = " + customerId);
        assertFalse(rs.next(), "Order should be deleted because the customer was deleted");

        rs.close();
    }

    // Test for Updating a Customer
    @Test
    @DisplayName("Test for Updating a Customer")
    void testUpdateCustomer() throws SQLException {
        // Insert a Customer
        stmt.executeUpdate("INSERT INTO Customer (name, email, phone_number) VALUES ('Alice', 'alice@example.com', '555-6789')");

        // Update the Customer
        PreparedStatement ps = conn.prepareStatement("UPDATE Customer SET phone_number = ? WHERE email = ?");
        ps.setString(1, "555-0000");
        ps.setString(2, "alice@example.com");
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be updated");

        // Query for the updated customer
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customer WHERE email = 'alice@example.com'");
        assertTrue(rs.next(), "Customer should exist in the database");
        assertEquals("555-0000", rs.getString("phone_number"));

        rs.close();
        ps.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/update_customer_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Updating a Customer")
    void testUpdateCustomer(String name, String email, String oldPhone, String newPhone) throws SQLException {
        // Insert a Customer
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)");
        ps.setString(1, name);
        ps.setString(2, email);
        ps.setString(3, oldPhone);
        ps.executeUpdate();
        ps.close();

        // Update the Customer
        ps = conn.prepareStatement("UPDATE Customer SET phone_number = ? WHERE email = ?");
        ps.setString(1, newPhone);
        ps.setString(2, email);
        int rowsAffected = ps.executeUpdate();
        assertEquals(1, rowsAffected, "1 row should be updated");

        // Query for the updated customer
        ResultSet rs = stmt.executeQuery("SELECT * FROM Customer WHERE email = '" + email + "'");
        assertTrue(rs.next(), "Customer should exist in the database");
        assertEquals(newPhone, rs.getString("phone_number"));

        rs.close();
        ps.close();
    }

    // Test for Deleting a Product
    @Test
    @DisplayName("Test for Deleting a Product")
    void testDeleteProduct() throws SQLException {
        // Insert a Product
        stmt.executeUpdate("INSERT INTO Product (product_name, price) VALUES ('Headphones', 199.99)");

        // Delete the Product
        int rowsAffected = stmt.executeUpdate("DELETE FROM Product WHERE product_name = 'Headphones'");
        assertEquals(1, rowsAffected, "1 row should be deleted");

        // Verify the product was deleted
        ResultSet rs = stmt.executeQuery("SELECT * FROM Product WHERE product_name = 'Headphones'");
        assertFalse(rs.next(), "Product should be deleted");

        rs.close();
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/delete_product_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Deleting a Product")
    void testDeleteProduct(String productName, double price) throws SQLException {
        // Insert a Product
        PreparedStatement ps = conn.prepareStatement("INSERT INTO Product (product_name, price) VALUES (?, ?)");
        ps.setString(1, productName);
        ps.setBigDecimal(2, new java.math.BigDecimal(price));
        ps.executeUpdate();
        ps.close();

        // Delete the Product
        int rowsAffected = stmt.executeUpdate("DELETE FROM Product WHERE product_name = '" + productName + "'");
        assertEquals(1, rowsAffected, "1 row should be deleted");

        // Verify the product was deleted
        ResultSet rs = stmt.executeQuery("SELECT * FROM Product WHERE product_name = '" + productName + "'");
        assertFalse(rs.next(), "Product should be deleted");

        rs.close();
    }

    // Test for Inserting an Order with a Non-Existent Customer (should fail)
    @Test
    @DisplayName("Test for Inserting an Order with a Non-Existent Customer (should fail)")
    void testInsertOrderWithNonExistentCustomer() {
        assertThrows(SQLException.class, () -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)");
            ps.setDate(1, Date.valueOf("2023-08-22"));
            ps.setInt(2, 999);  // Non-existent customer_id
            ps.executeUpdate();
        });
    }

    // Test for Inserting an Order_Product with a Non-Existent Order or Product (should fail)
    @Test
    @DisplayName("Test for Inserting an Order_Product with a Non-Existent Order or Product (should fail)")
    void testInsertOrderProductWithNonExistentOrderOrProduct() {
        assertThrows(SQLException.class, () -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
            ps.setInt(1, 999);  // Non-existent order_id
            ps.setInt(2, 1);    // Assuming product_id = 1 exists
            ps.setInt(3, 1);
            ps.setBigDecimal(4, new java.math.BigDecimal("10.00"));  // Sample unit price
            ps.executeUpdate();
        });

        assertThrows(SQLException.class, () -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
            ps.setInt(1, 1);    // Assuming order_id = 1 exists
            ps.setInt(2, 999);  // Non-existent product_id
            ps.setInt(3, 1);
            ps.setBigDecimal(4, new java.math.BigDecimal("10.00"));  // Sample unit price
            ps.executeUpdate();
        });
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOrderData")
    @DisplayName("Test for Inserting an Order with a Non-Existent Customer (should fail)")
    void testInsertOrderWithNonExistentCustomer(Date orderDate, int customerId) {
        SQLException exception = assertThrows(SQLException.class, () -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)");
            ps.setDate(1, orderDate);
            ps.setInt(2, customerId);
            ps.executeUpdate();
        });
        assertNotNull(exception, "SQLException should be thrown");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOrderProductData")
    @DisplayName("Test for Inserting an Order_Product with a Non-Existent Order or Product (should fail)")
    void testInsertOrderProductWithNonExistentOrderOrProduct(int orderId, int productId, int quantity) {
        SQLException exception1 = assertThrows(SQLException.class, () -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
            ps.setInt(1, orderId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, new java.math.BigDecimal("10.00"));  // Sample unit price
            ps.executeUpdate();
        });
        assertNotNull(exception1, "SQLException should be thrown for non-existent order_id");

        SQLException exception2 = assertThrows(SQLException.class, () -> {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)");
            ps.setInt(1, 1);    // Assuming order_id = 1 exists
            ps.setInt(2, 999);  // Non-existent product_id
            ps.setInt(3, 1);
            ps.setBigDecimal(4, new java.math.BigDecimal("10.00"));  // Sample unit price
            ps.executeUpdate();
        });
        assertNotNull(exception2, "SQLException should be thrown for non-existent product_id");
    }

}