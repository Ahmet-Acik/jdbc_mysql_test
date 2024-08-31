package databaseTests;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.util.DatabaseUtil;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataTest {

    private static Connection conn;
    private static Statement stmt;
    private static ResultSet rs;
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final Properties properties = new Properties();

    @BeforeAll
    static void setUp() throws SQLException, IOException {

        // Get the database name from the properties file
        String dbName = DatabaseUtil.getDatabaseName();

        // Drop the existing database
        DatabaseSetup.dropDatabase(dbName);


        // Create the database
        DatabaseSetup.createDatabase(dbName);

        // Establish the JDBC connection
        conn = DatabaseUtil.getConnection(dbName);
        stmt = conn.createStatement();

//        // Read and execute the schema SQL file
//        String schemaSql = new String(Files.readAllBytes(Paths.get("src/test/resources/schema.sql")));
//        for (String sql : schemaSql.split(";")) {
//            if (!sql.trim().isEmpty()) {
//                stmt.execute(sql);
//            }
//        }
//
//        // Read and execute the data SQL file
//        String dataSql = new String(Files.readAllBytes(Paths.get("src/test/resources/data.sql")));
//        for (String sql : dataSql.split(";")) {
//            if (!sql.trim().isEmpty()) {
//                stmt.execute(sql);
//            }
//        }
        // Configure and run Flyway migrations
        Flyway flyway = Flyway.configure()
                .dataSource(DatabaseUtil.getDataSource(dbName))
                .baselineOnMigrate(true)  // Set baselineOnMigrate to true
                .load();
        flyway.migrate();
    }


    @Test
    void test_Customer_Table_AssertThat_TheNumberOfRows_Is_20() {

        // Query the customer table and assert the number of rows is 20
        try {
            rs = stmt.executeQuery("SELECT COUNT(*) FROM Customer");
            rs.next();
            assert (rs.getInt(1) == 20);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database");
        }
    }

    // Query the product table and assert the number of rows is 10
    @Test
    void test_Product_Table_AssertThat_TheNumberOfRows_Is_10() {
        try {
            rs = stmt.executeQuery("SELECT COUNT(*) FROM Product");
            rs.next();
            assert (rs.getInt(1) == 10);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database");
        }
    }

//    //Display all orders with their order date and customer details:
//    @Test
//    void test_Order_Table_Display_AllOrders_With_OrderDate_And_CustomerDetails() {
//        try {
//            rs = stmt.executeQuery("SELECT o.order_id, o.order_date, c.name, c.email, c.phone_number " +
//                    "FROM `Order` o " +
//                    "JOIN Customer c ON o.customer_id = c.customer_id");
//            while (rs.next()) {
//                System.out.println("Order ID: " + rs.getInt("order_id") +
//                        ", Order Date: " + rs.getDate("order_date") +
//                        ", Customer Name: " + rs.getString("name") +
//                        ", Customer Email: " + rs.getString("email") +
//                        ", Customer Phone Number: " + rs.getString("phone_number"));
//            }
//        } catch (SQLException e) {
//            LOGGER.severe("Error querying the database");
//        }
//    }

    @Test
    void test_Order_Table_Display_AllOrders_With_OrderDate_And_CustomerDetails() {
        List<Map<String, Object>> orderDetailsList = new ArrayList<>();
        try {
            rs = stmt.executeQuery("SELECT o.order_id, o.order_date, c.name, c.email, c.phone_number " +
                    "FROM `Order` o " +
                    "JOIN Customer c ON o.customer_id = c.customer_id");
            while (rs.next()) {
                Map<String, Object> orderDetails = new HashMap<>();
                orderDetails.put("order_id", rs.getInt("order_id"));
                orderDetails.put("order_date", rs.getDate("order_date"));
                orderDetails.put("name", rs.getString("name"));
                orderDetails.put("email", rs.getString("email"));
                orderDetails.put("phone_number", rs.getString("phone_number"));
                orderDetailsList.add(orderDetails);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database");
        } finally {
            try {
                if (rs != null) rs.close();
                if (stmt != null) stmt.close();
            } catch (SQLException e) {
                LOGGER.severe("Error closing resources");
            }
        }

        // Assert the expected values
        assertEquals(20, orderDetailsList.size());

        // Add more assertions as needed
        assertEquals(1, orderDetailsList.get(0).get("order_id"));
        assertEquals(java.sql.Date.valueOf("2024-08-01"), orderDetailsList.get(0).get("order_date"));
        assertEquals("Alice Dupont", orderDetailsList.get(0).get("name"));
        assertEquals("alice.dupont@gmail.com", orderDetailsList.get(0).get("email"));
        assertEquals("+33612345678", orderDetailsList.get(0).get("phone_number"));

        assertEquals(2, orderDetailsList.get(1).get("order_id"));
        assertEquals(java.sql.Date.valueOf("2024-08-02"), orderDetailsList.get(1).get("order_date"));
        assertEquals("Bernard Martin", orderDetailsList.get(1).get("name"));
        assertEquals("bernard.martin@yahoo.com", orderDetailsList.get(1).get("email"));
        assertEquals("+33623456789", orderDetailsList.get(1).get("phone_number"));

    }
}
