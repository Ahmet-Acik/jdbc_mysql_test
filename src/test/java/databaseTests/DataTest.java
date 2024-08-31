package databaseTests;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.util.DatabaseUtil;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class DataTest {

    private static Connection conn;
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final String dbName = DatabaseUtil.getDatabaseName();

    @BeforeAll
    static void setUp() throws SQLException, IOException {

        // Drop the existing database
        DatabaseSetup.dropDatabase(dbName);

        // Create the database
        DatabaseSetup.createDatabase(dbName);

        // Establish the JDBC connection
        conn = DatabaseUtil.getConnection(dbName);

//        // Read and execute the schema SQL file
//        stmt = conn.createStatement();
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

    @BeforeEach
    void resetDatabase() throws SQLException {
        // Clean and re-apply Flyway migrations before each test
        Flyway flyway = Flyway.configure()
                .dataSource(DatabaseUtil.getDataSource(dbName))
                .load();
        flyway.clean();
        flyway.migrate();
    }


    /*Display all customers:
    SELECT * FROM Customer;

    -> [{customer_id=1, name=Alice Dupont,
     */
    @Test
    void test_Customer_Table_AssertThat_TheNumberOfRows_Is_20() {
        // Query the customer table and assert the number of rows is 20
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Customer")) {
            rs.next();
            assertEquals(20, rs.getInt(1));
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
    }

    /* Display all products:
    SELECT * FROM Product;

    -> [{product_id=1, product_name=Apple, price=1.0}, {product_id=2, product_name=Banana, price=1.5}, {product_id=3, product_name=Cherry, price=2.0}, {product_id=4, product_name=Date, price=2.5}, ...]

     */

    @Test
    void test_Product_Table_AssertThat_TheNumberOfRows_Is_10() {
        // Query the product table and assert the number of rows is 10
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM Product")) {
            rs.next();
            assertEquals(10, rs.getInt(1));
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
    }

    /* Display all orders with the order date and customer details:
    SELECT o.order_id, o.order_date, c.name, c.email, c.phone_number
    FROM `Order` o
    JOIN Customer c ON o.customer_id = c.customer_id;

    -> [{order_id=1, order_date=2024-08-01, name=Alice Dupont,
     */
    @Test
    void test_Order_Table_Display_AllOrders_With_OrderDate_And_CustomerDetails() {
        List<Map<String, Object>> orderDetailsList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT o.order_id, o.order_date, c.name, c.email, c.phone_number " +
                     "FROM `Order` o " +
                     "JOIN Customer c ON o.customer_id = c.customer_id")) {
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
            LOGGER.severe("Error querying the database: " + e.getMessage());
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

    /*Display all products ordered in each order (with quantities):

    SELECT o.order_id, p.product_name, op.quantity
    FROM Order_Product op
    JOIN Product p ON op.product_id = p.product_id
    JOIN `Order` o ON op.order_id = o.order_id;

    -> [{order_id=19, product_name=Orange, quantity=1}, {order_id=19, product_name=Apple, quantity=1}, {order_id=19, product_name=Banana, quantity=1}, {order_id=19, product_name=Cherry, quantity=1}, ...]
     */

    @Test
    void test_Order_Table_Display_AllProductsOrdered_InEachOrder_WithQuantities() {
        List<Map<String, Object>> orderDetailsList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT o.order_id, p.product_name, op.quantity " +
                     "FROM Order_Product op " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "JOIN `Order` o ON op.order_id = o.order_id")) {
            while (rs.next()) {
                Map<String, Object> orderDetails = new HashMap<>();
                orderDetails.put("order_id", rs.getInt("order_id"));
                orderDetails.put("product_name", rs.getString("product_name"));
                orderDetails.put("quantity", rs.getInt("quantity"));
                orderDetailsList.add(orderDetails);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }

        // Assert the expected values
        assertEquals(40, orderDetailsList.size());

        assertEquals(19, orderDetailsList.get(0).get("order_id"));
        assertEquals("Orange", orderDetailsList.get(0).get("product_name"));
        assertEquals(1, orderDetailsList.get(0).get("quantity"));

        assertEquals(15, orderDetailsList.get(5).get("order_id"));
        assertEquals("Lemon", orderDetailsList.get(5).get("product_name"));
        assertEquals(1, orderDetailsList.get(5).get("quantity"));

        assertEquals(20, orderDetailsList.get(8).get("order_id"));
        assertEquals("Apple", orderDetailsList.get(8).get("product_name"));
        assertEquals(1, orderDetailsList.get(8).get("quantity"));

        assertEquals(16, orderDetailsList.get(13).get("order_id"));
        assertEquals("Banana", orderDetailsList.get(13).get("product_name"));
        assertEquals(2, orderDetailsList.get(13).get("quantity"));
    }

    /* Display the total amount spent by each customer on their orders:

    SELECT c.name, SUM(op.quantity * p.price) AS total_spent
    FROM Customer c
    JOIN `Order` o ON c.customer_id = o.customer_id
    JOIN Order_Product op ON o.order_id = op.order_id
    JOIN Product p ON op.product_id = p.product_id
    GROUP BY c.name;

    -> [{name=Thomas Perrault, total_spent=17.0}, {name=Pauline Robert, total_spent=10.0}, {name=Marie Leroy, total_spent=11.0}, {name=Francois Dubois, total_spent=3.75}, ...]
     */
    @Test
    void test_Customer_Table_Display_TotalAmountSpent_ByEachCustomer_OnTheirOrders() {
        List<Map<String, Object>> customerSpendingList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, SUM(op.quantity * p.price) AS total_spent " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "GROUP BY c.name")) {
            while (rs.next()) {
                Map<String, Object> customerSpending = new HashMap<>();
                customerSpending.put("name", rs.getString("name"));
                customerSpending.put("total_spent", rs.getDouble("total_spent"));
                customerSpendingList.add(customerSpending);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }

        assertEquals(20, customerSpendingList.size());
        assertEquals("Thomas Perrault", customerSpendingList.get(0).get("name"));
        assertEquals(17.0, customerSpendingList.get(0).get("total_spent"));

        assertEquals("Pauline Robert", customerSpendingList.get(1).get("name"));
        assertEquals(10.0, customerSpendingList.get(1).get("total_spent"));

        assertEquals("Marie Leroy", customerSpendingList.get(2).get("name"));
        assertEquals(11.0, customerSpendingList.get(2).get("total_spent"));

        assertEquals("Francois Dubois", customerSpendingList.get(3).get("name"));
        assertEquals(3.75, customerSpendingList.get(3).get("total_spent"));
    }

    /*
    List customers who have purchased a specific product (e.g., 'Orange'):

    SELECT DISTINCT c.name
    FROM Customer c
    JOIN `Order` o ON c.customer_id = o.customer_id
    JOIN Order_Product op ON o.order_id = op.order_id
    JOIN Product p ON op.product_id = p.product_id
    WHERE p.product_name = 'Orange';

    -> [{name=Thomas Perrault}, {name=Pauline Robert}, {name=Marie Leroy}, {name=Francois Dubois}, {name=Alice Dupont}]
         */

    @Test
    void test_Customer_Table_Display_CustomersWhoOrdered_Orange() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT c.name " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "WHERE p.product_name = 'Orange'")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        assertEquals(5, customerList.size());
        assertEquals("Thomas Perrault", customerList.get(0).get("name"));
        assertEquals("Pauline Robert", customerList.get(1).get("name"));
        assertEquals("Marie Leroy", customerList.get(2).get("name"));
        assertEquals("Francois Dubois", customerList.get(3).get("name"));
        assertEquals("Alice Dupont", customerList.get(4).get("name"));
    }

    /*
    Display all orders placed on a specific date (e.g., '2024-08-05'):

    SELECT o.order_id, c.name, o.order_date
    FROM `Order` o
    JOIN Customer c ON o.customer_id = c.customer_id
    WHERE o.order_date = '2024-08-05';

    -> 5,Elise Renault,2024-08-05

     */

    @Test
    void test_Order_Table_Display_AllOrders_PlacedOn_2024_08_05() {
        List<Map<String, Object>> orderList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT o.order_id, c.name, o.order_date " +
                     "FROM `Order` o " +
                     "JOIN Customer c ON o.customer_id = c.customer_id " +
                     "WHERE o.order_date = '2024-08-05'")) {
            while (rs.next()) {
                Map<String, Object> order = new HashMap<>();
                order.put("order_id", rs.getInt("order_id"));
                order.put("name", rs.getString("name"));
                order.put("order_date", rs.getDate("order_date"));
                orderList.add(order);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        assertEquals(1, orderList.size());
        assertEquals(5, orderList.get(0).get("order_id"));
        assertEquals("Elise Renault", orderList.get(0).get("name"));
        assertEquals(java.sql.Date.valueOf("2024-08-05"), orderList.get(0).get("order_date"));

    }
    /*Display the number of products purchased by each customer:
    SELECT c.name, SUM(op.quantity) AS total_products
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        GROUP BY c.name;

     */

    /*List customers who have placed more than one order:
    SELECT c.name, COUNT(o.order_id) AS order_count
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        GROUP BY c.name
        HAVING order_count > 1;

     */

    /*Display products that have been ordered more than once:
    SELECT p.product_name, COUNT(op.order_id) AS order_count
        FROM Product p
        JOIN Order_Product op ON p.product_id = op.product_id
        GROUP BY p.product_name
        HAVING order_count > 1;

     */

    /*Find the most expensive product ordered by a specific customer (e.g., 'Alice Dupont'):
    SELECT p.product_name, p.price
        FROM Product p
        JOIN Order_Product op ON p.product_id = op.product_id
        JOIN `Order` o ON op.order_id = o.order_id
        JOIN Customer c ON o.customer_id = c.customer_id
        WHERE c.name = 'Alice Dupont'
        ORDER BY p.price DESC
        LIMIT 1;

     */

    /*Display customers and the total number of orders they've placed:
    SELECT c.name, COUNT(o.order_id) AS total_orders
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        GROUP BY c.name;

     */

    /*Display the total quantity of each product ordered across all orders:
    SELECT p.product_name, SUM(op.quantity) AS total_quantity_ordered
        FROM Product p
        JOIN Order_Product op ON p.product_id = op.product_id
        GROUP BY p.product_name;

     */

    /*List orders where more than one type of product was ordered:
    SELECT o.order_id, COUNT(op.product_id) AS product_count
        FROM `Order` o
        JOIN Order_Product op ON o.order_id = op.order_id
        GROUP BY o.order_id
        HAVING product_count > 1;

     */

    /*Find the customer who has spent the most overall:
    SELECT c.name, SUM(op.quantity * p.price) AS total_spent
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        JOIN Product p ON op.product_id = p.product_id
        GROUP BY c.name
        ORDER BY total_spent DESC
        LIMIT 1;

     */

    /*Display all orders that include a product costing more than a certain price (e.g., 5.00):
    SELECT DISTINCT o.order_id, c.name, p.product_name, p.price
        FROM `Order` o
        JOIN Customer c ON o.customer_id = c.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        JOIN Product p ON op.product_id = p.product_id
        WHERE p.price > 5.00;

     */

    /*List all customers who haven't placed an order:
    SELECT c.name
        FROM Customer c
        LEFT JOIN `Order` o ON c.customer_id = o.customer_id
        WHERE o.order_id IS NULL;

     */

    /*Find customers who ordered more than one of the same product:
    SELECT c.name, p.product_name, op.quantity
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        JOIN Product p ON op.product_id = p.product_id
        WHERE op.quantity > 1;

     */


    /*Display the total number of orders placed in a specific month (e.g., August 2024):
    SELECT COUNT(order_id) AS total_orders
        FROM `Order`
        WHERE MONTH(order_date) = 8 AND YEAR(order_date) = 2024;

     */

    /*Display the number of distinct products ordered by each customer:
    SELECT c.name, COUNT(DISTINCT op.product_id) AS distinct_products
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        GROUP BY c.name;
     */








}
