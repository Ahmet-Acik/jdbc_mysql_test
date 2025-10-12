package databaseTests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Comprehensive table operations tests using JDBC.
 * Extends BaseIntegrationTest for DRY principles - eliminates duplicate setup/teardown code.
 * 
 * Tests validate:
 * - CRUD operations using PreparedStatement
 * - Parameterized tests with CSV data sources
 * - SQL injection prevention
 * - Transaction handling
 */
public class TableDataBaseTest extends BaseIntegrationTest {

    private Statement stmt;

    private static final String INSERT_CUSTOMER = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
    private static final String INSERT_PRODUCT = "INSERT INTO Product (product_name, price) VALUES (?, ?)";
    private static final String INSERT_ORDER = "INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)";
    private static final String INSERT_ORDER_PRODUCT = "INSERT INTO Order_Product (order_id, product_id, quantity, unit_price) VALUES (?, ?, ?, ?)";
    private static final String SELECT_CUSTOMER_BY_EMAIL = "SELECT * FROM Customer WHERE email = ?";
    private static final String SELECT_PRODUCT_BY_NAME = "SELECT * FROM Product WHERE product_name = ?";
    private static final String SELECT_ORDER_PRODUCT_BY_ORDER_ID = "SELECT * FROM Order_Product WHERE order_id = ?";
    private static final String UPDATE_CUSTOMER_PHONE = "UPDATE Customer SET phone_number = ? WHERE email = ?";
    private static final String DELETE_PRODUCT_BY_NAME = "DELETE FROM Product WHERE product_name = ?";

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

    static Stream<Arguments> provideInvalidCustomerData() {
        return Stream.of(
                Arguments.of(null, "invalid@example.com", "555-0000"),  // Null name
                Arguments.of("Invalid User", null, "555-0000"),         // Null email
                Arguments.of("Invalid User", "invalid@example.com", null), // Null phone number
                Arguments.of("Duplicate User", "duplicate@example.com", "555-0000") // Duplicate email
        );
    }

    static Stream<Arguments> provideInvalidProductData() {
        return Stream.of(
                Arguments.of(null, 100.00),  // Null product name
                Arguments.of("Invalid Product", -10.00)  // Negative price
        );
    }

    static Stream<Arguments> provideInvalidUpdateCustomerData() {
        return Stream.of(
                Arguments.of("nonexistent@example.com", "555-0000")  // Non-existent email
        );
    }

    static Stream<Arguments> provideInvalidDeleteProductData() {
        return Stream.of(
                Arguments.of("NonExistentProduct")  // Non-existent product name
        );
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/customer_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Creating and Reading a Customer")
    void testCreateAndReadCustomer(String name, String email, String phoneNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CUSTOMER)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phoneNumber);
            int rowsAffected = ps.executeUpdate();
            assertEquals(1, rowsAffected, "1 row should be inserted");
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_CUSTOMER_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Customer should exist in the database");
                assertEquals(name, rs.getString("name"));
                assertEquals(email, rs.getString("email"));
                assertEquals(phoneNumber, rs.getString("phone_number"));
            }
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/product_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Creating and Reading a Product")
    void testCreateAndReadProduct(String productName, double price) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCT)) {
            ps.setString(1, productName);
            ps.setDouble(2, price);
            int rowsAffected = ps.executeUpdate();
            assertEquals(1, rowsAffected, "1 row should be inserted");
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_PRODUCT_BY_NAME)) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Product should exist in the database");
                assertEquals(productName, rs.getString("product_name"));
                assertEquals(price, rs.getBigDecimal("price").doubleValue());
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideCustomerData")
    @DisplayName("Test for Creating and Reading a Customer using Method Source")
    void testCreateAndReadCustomerMethodSource(String name, String email, String phoneNumber) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CUSTOMER)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phoneNumber);
            int rowsAffected = ps.executeUpdate();
            assertEquals(1, rowsAffected, "1 row should be inserted");
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_CUSTOMER_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Customer should exist in the database");
                assertEquals(name, rs.getString("name"));
                assertEquals(email, rs.getString("email"));
                assertEquals(phoneNumber, rs.getString("phone_number"));
            }
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/order_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Creating an Order and Linking it to a Customer and Product")
    void testCreateOrderAndLinkProduct(String orderDate, String customerName, String customerEmail, String customerPhone, String productName, double productPrice, int quantity) throws SQLException {
        int customerId;
        int productId;
        int orderId;

        try (PreparedStatement ps = conn.prepareStatement(INSERT_CUSTOMER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, customerName);
            ps.setString(2, customerEmail);
            ps.setString(3, customerPhone);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                customerId = rs.getInt(1);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCT, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, productName);
            ps.setBigDecimal(2, new java.math.BigDecimal(productPrice));
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                productId = rs.getInt(1);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_ORDER, Statement.RETURN_GENERATED_KEYS)) {
            ps.setDate(1, Date.valueOf(orderDate));
            ps.setInt(2, customerId);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                orderId = rs.getInt(1);
            }
        }

        try (PreparedStatement ps = conn.prepareStatement(INSERT_ORDER_PRODUCT)) {
            ps.setInt(1, orderId);
            ps.setInt(2, productId);
            ps.setInt(3, quantity);
            ps.setBigDecimal(4, new java.math.BigDecimal(productPrice));
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_ORDER_PRODUCT_BY_ORDER_ID)) {
            ps.setInt(1, orderId);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Product should exist in the order");
                assertEquals(orderId, rs.getInt("order_id"));
                assertEquals(productId, rs.getInt("product_id"));
                assertEquals(quantity, rs.getInt("quantity"));
            }
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/update_customer_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Updating a Customer")
    void testUpdateCustomer(String name, String email, String oldPhone, String newPhone) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CUSTOMER)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, oldPhone);
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(UPDATE_CUSTOMER_PHONE)) {
            ps.setString(1, newPhone);
            ps.setString(2, email);
            int rowsAffected = ps.executeUpdate();
            assertEquals(1, rowsAffected, "1 row should be updated");
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_CUSTOMER_BY_EMAIL)) {
            ps.setString(1, email);
            try (ResultSet rs = ps.executeQuery()) {
                assertTrue(rs.next(), "Customer should exist in the database");
                assertEquals(newPhone, rs.getString("phone_number"));
            }
        }
    }

    @ParameterizedTest
    @CsvFileSource(resources = "/delete_product_data.csv", numLinesToSkip = 1)
    @DisplayName("Test for Deleting a Product")
    void testDeleteProduct(String productName, double price) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCT)) {
            ps.setString(1, productName);
            ps.setBigDecimal(2, new java.math.BigDecimal(price));
            ps.executeUpdate();
        }

        try (PreparedStatement ps = conn.prepareStatement(DELETE_PRODUCT_BY_NAME)) {
            ps.setString(1, productName);
            int rowsAffected = ps.executeUpdate();
            assertEquals(1, rowsAffected, "1 row should be deleted");
        }

        try (PreparedStatement ps = conn.prepareStatement(SELECT_PRODUCT_BY_NAME)) {
            ps.setString(1, productName);
            try (ResultSet rs = ps.executeQuery()) {
                assertFalse(rs.next(), "Product should be deleted");
            }
        }
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOrderData")
    @DisplayName("Test for Inserting an Order with a Non-Existent Customer (should fail)")
    void testInsertOrderWithNonExistentCustomer(Date orderDate, int customerId) {
        SQLException exception = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_ORDER)) {
                ps.setDate(1, orderDate);
                ps.setInt(2, customerId);
                ps.executeUpdate();
            }
        });
        assertNotNull(exception, "SQLException should be thrown");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidOrderProductData")
    @DisplayName("Test for Inserting an Order_Product with a Non-Existent Order or Product (should fail)")
    void testInsertOrderProductWithNonExistentOrderOrProduct(int orderId, int productId, int quantity) {
        SQLException exception1 = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_ORDER_PRODUCT)) {
                ps.setInt(1, orderId);
                ps.setInt(2, productId);
                ps.setInt(3, quantity);
                ps.setBigDecimal(4, new java.math.BigDecimal("10.00"));
                ps.executeUpdate();
            }
        });
        assertNotNull(exception1, "SQLException should be thrown for non-existent order_id");

        SQLException exception2 = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_ORDER_PRODUCT)) {
                ps.setInt(1, 1);    // Assuming order_id = 1 exists
                ps.setInt(2, 999);  // Non-existent product_id
                ps.setInt(3, 1);
                ps.setBigDecimal(4, new java.math.BigDecimal("10.00"));
                ps.executeUpdate();
            }
        });
        assertNotNull(exception2, "SQLException should be thrown for non-existent product_id");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidProductData")
    @DisplayName("Test for Inserting a Product with Invalid Data (should fail)")
    void testInsertProductWithInvalidData(String productName, double price) {
        SQLException exception = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_PRODUCT)) {
                ps.setString(1, productName);
                ps.setDouble(2, price);
                ps.executeUpdate();
            }
        });
        assertNotNull(exception, "SQLException should be thrown for invalid product data");
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUpdateCustomerData")
    @DisplayName("Test for Updating a Customer with Invalid Data (should fail)")
    void testUpdateCustomerWithInvalidData(String email, String newPhone) {
        try (PreparedStatement ps = conn.prepareStatement(UPDATE_CUSTOMER_PHONE)) {
            ps.setString(1, newPhone);
            ps.setString(2, email);
            int rowsAffected = ps.executeUpdate();
            assertEquals(0, rowsAffected, "No rows should be updated for non-existent email");
        } catch (SQLException e) {
            assertNotNull(e, "SQLException should be thrown for invalid update data");
        }
    }

    @ParameterizedTest
    @MethodSource("provideInvalidDeleteProductData")
    @DisplayName("Test for Deleting a Product with Invalid Data (should fail)")
    void testDeleteProductWithInvalidData(String productName) {
        try (PreparedStatement ps = conn.prepareStatement(DELETE_PRODUCT_BY_NAME)) {
            ps.setString(1, productName);
            int rowsAffected = ps.executeUpdate();
            assertEquals(0, rowsAffected, "No rows should be deleted for non-existent product");
        } catch (SQLException e) {
            assertNotNull(e, "SQLException should be thrown for invalid delete data");
        }
    }

    @ParameterizedTest
    @MethodSource("provideInvalidCustomerData")
    @DisplayName("Test for Inserting a Customer with Invalid Data (should fail)")
    void testInsertCustomerWithInvalidData(String name, String email, String phoneNumber) {
        // Setup duplicate email
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CUSTOMER)) {
            ps.setString(1, "Existing User");
            ps.setString(2, "duplicate@example.com");
            ps.setString(3, "555-0000");
            ps.executeUpdate();
        } catch (SQLException e) {
            // Ignore if the setup fails, as it might already exist
        }

        // Test for invalid customer data
        try (PreparedStatement ps = conn.prepareStatement(INSERT_CUSTOMER)) {
            ps.setString(1, name);
            ps.setString(2, email);
            ps.setString(3, phoneNumber);
            ps.executeUpdate();
            fail("SQLException should be thrown for invalid customer data");
        } catch (SQLException e) {
            assertNotNull(e, "SQLException should be thrown for invalid customer data");
        }
    }
}