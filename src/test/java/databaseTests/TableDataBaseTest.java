package databaseTests;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.util.DatabaseUtil;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvFileSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.*;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

public class TableDataBaseTest {


    private static Connection conn;
    private static Statement stmt;

    private static final String INSERT_CUSTOMER = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
    private static final String INSERT_PRODUCT = "INSERT INTO Product (product_name, price) VALUES (?, ?)";
    private static final String INSERT_ORDER = "INSERT INTO `Order` (order_date, customer_id) VALUES (?, ?)";
    private static final String INSERT_ORDER_PRODUCT = "INSERT INTO Order_Product (order_id, product_id, quantity) VALUES (?, ?, ?)";
    private static final String SELECT_CUSTOMER_BY_EMAIL = "SELECT * FROM Customer WHERE email = ?";
    private static final String SELECT_PRODUCT_BY_NAME = "SELECT * FROM Product WHERE product_name = ?";
    private static final String SELECT_ORDER_PRODUCT_BY_ORDER_ID = "SELECT * FROM Order_Product WHERE order_id = ?";
    private static final String UPDATE_CUSTOMER_PHONE = "UPDATE Customer SET phone_number = ? WHERE email = ?";
    private static final String DELETE_PRODUCT_BY_NAME = "DELETE FROM Product WHERE product_name = ?";

    @BeforeAll
    static void setup() throws SQLException {
        String dbName = DatabaseUtil.getDatabaseName();

        // Create the database
        DatabaseSetup.createDatabase(dbName);

        // Establish the JDBC connection
        conn = DatabaseUtil.getConnection(dbName);
        stmt = conn.createStatement();

        // Drop and recreate tables for testing
        stmt.executeUpdate("DROP TABLE IF EXISTS Order_Product");
        stmt.executeUpdate("DROP TABLE IF EXISTS `Order`");
        stmt.executeUpdate("DROP TABLE IF EXISTS Product");
        stmt.executeUpdate("DROP TABLE IF EXISTS Customer");

        // Create Customer Table
        stmt.executeUpdate("CREATE TABLE Customer (" +
                "customer_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "phone_number VARCHAR(15) NOT NULL" +
                ")");

        // Create Product Table
        stmt.executeUpdate("CREATE TABLE Product (" +
                "product_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "price DECIMAL(10, 2) NOT NULL" +
                ")");

        // Create Order Table
        stmt.executeUpdate("CREATE TABLE `Order` (" +
                "order_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "order_date DATE NOT NULL, " +
                "customer_id INT NOT NULL, " +
                "FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE CASCADE" +
                ")");

        // Create Order_Product Junction Table
        stmt.executeUpdate("CREATE TABLE Order_Product (" +
                "order_id INT NOT NULL, " +
                "product_id INT NOT NULL, " +
                "quantity INT NOT NULL, " +
                "PRIMARY KEY (order_id, product_id), " +
                "FOREIGN KEY (order_id) REFERENCES `Order`(order_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (product_id) REFERENCES Product(product_id) ON DELETE CASCADE" +
                ")");
    }

    @AfterAll
    static void teardown() throws SQLException {
        DatabaseUtil.closeResources(conn, stmt, null);
    }

    @BeforeEach
    void clearDatabase() throws SQLException {
        stmt.executeUpdate("DELETE FROM Order_Product");
        stmt.executeUpdate("DELETE FROM `Order`");
        stmt.executeUpdate("DELETE FROM Product");
        stmt.executeUpdate("DELETE FROM Customer");
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
            ps.setBigDecimal(2, new java.math.BigDecimal(price));
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
                ps.executeUpdate();
            }
        });
        assertNotNull(exception1, "SQLException should be thrown for non-existent order_id");

        SQLException exception2 = assertThrows(SQLException.class, () -> {
            try (PreparedStatement ps = conn.prepareStatement(INSERT_ORDER_PRODUCT)) {
                ps.setInt(1, 1);    // Assuming order_id = 1 exists
                ps.setInt(2, 999);  // Non-existent product_id
                ps.setInt(3, 1);
                ps.executeUpdate();
            }
        });
        assertNotNull(exception2, "SQLException should be thrown for non-existent product_id");
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
}