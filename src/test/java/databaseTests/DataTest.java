package databaseTests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.util.DatabaseUtil;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zaxxer.hikari.HikariDataSource;

public class DataTest {

    private static Connection conn;
    private static HikariDataSource dataSource;
    private static final Logger LOGGER = Logger.getLogger(DatabaseUtil.class.getName());
    private static final String dbName = "testdb_integration"; // Use separate test database
    private static final Properties properties = new Properties();

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
                .cleanDisabled(false)  // Enable clean for tests
                .load();
        flyway.clean();
        flyway.migrate();
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null) {
            dataSource.close();
        }
        // Introduce a delay to avoid exceeding the connection limit
        try {
            Thread.sleep(1000); // 1 second delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /*Display all customers:
    SELECT * FROM Customer;

    -> [{customer_id=1, name=Alice Dupont,
     */
    @Test
    void test_Customer_Table_AssertThat_TheNumberOfRows_Is_20() {
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
                     "JOIN Customer c ON o.customer_id = c.customer_id " +
                     "ORDER BY o.order_id")) {
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

        assertEquals(20, orderDetailsList.size());
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

    -> [{quantity=1, order_id=1, product_name=Apple}, {quantity=2, order_id=1, product_name=Orange}, {quantity=1, order_id=2, product_name=Grapes}, {quantity=3, order_id=2, product_name=Lemon}, {quantity=2, order_id=3, product_name=Banana}, {quantity=1, order_id=3, product_name=Pineapple}, {quantity=1, order_id=4, product_name=Mango}, {quantity=1, order_id=4, product_name=Peach}, {quantity=2, order_id=5, product_name=Blueberry}, {quantity=1, order_id=5, product_name=Strawberry}, {quantity=1, order_id=6, product_name=Banana}, {quantity=1, order_id=6, product_name=Orange}, {quantity=2, order_id=7, product_name=Apple}, {quantity=1, order_id=7, product_name=Grapes}, {quantity=2, order_id=8, product_name=Pineapple}, {quantity=1, order_id=8, product_name=Strawberry}, {quantity=1, order_id=9, product_name=Lemon}, {quantity=1, order_id=9, product_name=Peach}, {quantity=3, order_id=10, product_name=Apple}, {quantity=2, order_id=10, product_name=Banana}, {quantity=1, order_id=11, product_name=Blueberry}, {quantity=1, order_id=11, product_name=Grapes}, {quantity=1, order_id=12, product_name=Mango}, {quantity=1, order_id=12, product_name=Peach}, {quantity=2, order_id=13, product_name=Apple}, {quantity=2, order_id=13, product_name=Orange}, {quantity=1, order_id=14, product_name=Blueberry}, {quantity=2, order_id=14, product_name=Pineapple}, {quantity=1, order_id=15, product_name=Lemon}, {quantity=1, order_id=15, product_name=Strawberry}, {quantity=2, order_id=16, product_name=Banana}, {quantity=3, order_id=16, product_name=Orange}, {quantity=1, order_id=17, product_name=Grapes}, {quantity=2, order_id=17, product_name=Mango}, {quantity=1, order_id=18, product_name=Peach}, {quantity=1, order_id=18, product_name=Strawberry}, {quantity=2, order_id=19, product_name=Blueberry}, {quantity=1, order_id=19, product_name=Orange}, {quantity=1, order_id=20, product_name=Apple}, {quantity=1, order_id=20, product_name=Pineapple}]
      */
    @Test
    void test_Order_Table_Display_AllProductsOrdered_InEachOrder_WithQuantities() {
        List<Map<String, Object>> orderDetailsList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT o.order_id, p.product_name, op.quantity " +
                     "FROM Order_Product op " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "JOIN `Order` o ON op.order_id = o.order_id " +
                     "ORDER BY o.order_id, p.product_name")) {
            while (rs.next()) {
                Map<String, Object> orderDetails = new HashMap<>();
                orderDetails.put("order_id", rs.getInt("order_id"));
                orderDetails.put("product_name", rs.getString("product_name"));
                orderDetails.put("quantity", rs.getInt("quantity"));
                orderDetailsList.add(orderDetails);
            }
            System.out.println(orderDetailsList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        assertEquals(40, orderDetailsList.size());

        assertEquals(1, orderDetailsList.get(0).get("order_id"));
        assertEquals("Apple", orderDetailsList.get(0).get("product_name"));
        assertEquals(1, orderDetailsList.get(0).get("quantity"));

        assertEquals(1, orderDetailsList.get(1).get("order_id"));
        assertEquals("Orange", orderDetailsList.get(1).get("product_name"));
        assertEquals(2, orderDetailsList.get(1).get("quantity"));

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
                     "GROUP BY c.name " +
                     "ORDER BY total_spent DESC, c.name ASC")) {
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
        assertEquals("Elise Renault", customerSpendingList.get(0).get("name"));
        assertEquals(20.5, customerSpendingList.get(0).get("total_spent"));

        assertEquals("Nicolas Gauthier", customerSpendingList.get(1).get("name"));
        assertEquals(17.25, customerSpendingList.get(1).get("total_spent"));

        assertEquals("Thomas Perrault", customerSpendingList.get(2).get("name"));
        assertEquals(17.0, customerSpendingList.get(2).get("total_spent"));

        assertEquals("Hugo Blanchard", customerSpendingList.get(3).get("name"));
        assertEquals(16.0, customerSpendingList.get(3).get("total_spent"));
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
                     "WHERE p.product_name = 'Orange' " +
                     "ORDER BY c.name")) {
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
        assertEquals("Alice Dupont", customerList.get(0).get("name"));
        assertEquals("Francois Dubois", customerList.get(1).get("name"));
        assertEquals("Marie Leroy", customerList.get(2).get("name"));
        assertEquals("Pauline Robert", customerList.get(3).get("name"));
        assertEquals("Thomas Perrault", customerList.get(4).get("name"));
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
                     "WHERE o.order_date = '2024-08-05' " +
                     "ORDER BY o.order_id")) {
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
    -> [{name=Jacques Durand, total_products=5}, {name=Pauline Robert, total_products=5}, {name=Bernard Martin, total_products=4}, {name=Marie Leroy, total_products=4}, {name=Alice Dupont, total_products=3}, {name=Claire Lefevre, total_products=3}, {name=Elise Renault, total_products=3}, {name=Gabrielle Petit, total_products=3}, {name=Hugo Blanchard, total_products=3}, {name=Nicolas Gauthier, total_products=3}, {name=Quentin Lambert, total_products=3}, {name=Thomas Perrault, total_products=3}, {name=David Laurent, total_products=2}, {name=Francois Dubois, total_products=2}, {name=Isabelle Fontaine, total_products=2}, {name=Karine Girard, total_products=2}, {name=Luc Moreau, total_products=2}, {name=Olivier Bernard, total_products=2}, {name=Sophie Michel, total_products=2}, {name=Valerie Simon, total_products=2}]
    */
    @Test
    void test_Customer_Table_Display_NumberOfProductsPurchased_ByEachCustomer() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, SUM(op.quantity) AS total_products " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "GROUP BY c.name " +
                     "ORDER BY total_products DESC")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("total_products", rs.getInt("total_products"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }

        assertEquals(20, customerList.size());
        assertEquals("Jacques Durand", customerList.get(0).get("name"));
        assertEquals(5, customerList.get(0).get("total_products"));
        assertEquals("Pauline Robert", customerList.get(1).get("name"));
        assertEquals(5, customerList.get(1).get("total_products"));


    }

    /*List customers who have placed more than one order:
    SELECT c.name, COUNT(o.order_id) AS order_count
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        GROUP BY c.name
        HAVING order_count > 1;
      */
    @Test
    void test_Customer_Table_Display_CustomersWhoPlaced_MoreThanOneOrder() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, COUNT(o.order_id) AS order_count " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "GROUP BY c.name " +
                     "HAVING COUNT(o.order_id) > 1 " +
                     "ORDER BY order_count DESC")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("order_count", rs.getInt("order_count"));
                customerList.add(customer);
            }
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Customers who placed more than one order: " + customerList);
        assertEquals(0, customerList.size());
    }

    /*Display the total number of products ordered by each customer:
    SELECT c.name, SUM(op.quantity) AS total_products
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        GROUP BY c.name;

        -> [{name=Jacques Durand, total_products=5}, {name=Pauline Robert, total_products=5}, {name=Bernard Martin, total_products=4}, {name=Marie Leroy, total_products=4}, {name=Alice Dupont, total_products=3}, {name=Claire Lefevre, total_products=3}, {name=Elise Renault, total_products=3}, {name=Gabrielle Petit, total_products=3}, {name=Hugo Blanchard, total_products=3}, {name=Nicolas Gauthier, total_products=3}, {name=Quentin Lambert, total_products=3}, {name=Thomas Perrault, total_products=3}, {name=David Laurent, total_products=2}, {name=Francois Dubois, total_products=2}, {name=Isabelle Fontaine, total_products=2}, {name=Karine Girard, total_products=2}, {name=Luc Moreau, total_products=2}, {name=Olivier Bernard, total_products=2}, {name=Sophie Michel, total_products=2}, {name=Valerie Simon, total_products=2}]
     */
    @Test
    void test_Customer_Table_Display_TotalNumberOfProductsOrdered_ByEachCustomer() throws SQLException {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, SUM(op.quantity) AS total_products " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "GROUP BY c.name " +
                     "ORDER BY total_products DESC")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("total_products", rs.getInt("total_products"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        assertEquals(20, customerList.size());
        assertEquals("Jacques Durand", customerList.get(0).get("name"));
        assertEquals(5, customerList.get(0).get("total_products"));
        assertEquals("Pauline Robert", customerList.get(1).get("name"));
        assertEquals(5, customerList.get(1).get("total_products"));
    }

    /*Display products that have been ordered more than once:
    SELECT p.product_name, COUNT(op.order_id) AS order_count
        FROM Product p
        JOIN Order_Product op ON p.product_id = op.product_id
        GROUP BY p.product_name
        HAVING order_count > 1;
    -> [{order_count=5, product_name=Orange}, {order_count=5, product_name=Apple}, {order_count=4, product_name=Banana}, {order_count=4, product_name=Grapes}, {order_count=4, product_name=Pineapple}, {order_count=4, product_name=Peach}, {order_count=4, product_name=Strawberry}, {order_count=4, product_name=Blueberry}, {order_count=3, product_name=Lemon}, {order_count=3, product_name=Mango}]
     */
    @Test
    void test_Product_Table_Display_ProductsThatHaveBeenOrdered_MoreThanOnce() {
        List<Map<String, Object>> productList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT p.product_name, COUNT(op.order_id) AS order_count " +
                     "FROM Product p " +
                     "JOIN Order_Product op ON p.product_id = op.product_id " +
                     "GROUP BY p.product_name " +
                     "HAVING COUNT(op.order_id) > 1 " +
                     "ORDER BY order_count DESC")) {
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("product_name", rs.getString("product_name"));
                product.put("order_count", rs.getInt("order_count"));
                productList.add(product);
            }
            System.out.println(productList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Products that have been ordered more than once: " + productList);
        assertEquals(10, productList.size());
        assertEquals("Orange", productList.get(0).get("product_name"));
        assertEquals(5, productList.get(0).get("order_count"));
        assertEquals("Apple", productList.get(1).get("product_name"));
        assertEquals(5, productList.get(1).get("order_count"));
    }

    /*Find the most expensive product ordered by a specific customer (e.g., 'Alice Dupont'):
    SELECT p.product_name, p.price
        FROM Product p
        JOIN Order_Product op ON p.product_id = op.product_id
        JOIN `Order` o ON op.order_id = o.order_id
        JOIN Customer c ON o.customer_id = c.customer_id
        WHERE c.name = 'Alice Dupont'
        ORDER BY p.price DESC
        LIMIT 1;

       -> [{price=3.0, product_name=Apple}]
     */
    @Test
    void test_Customer_Table_FindTheMostExpensiveProductOrdered_ByAliceDupont() {
        List<Map<String, Object>> productList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT p.product_name, p.price " +
                     "FROM Product p " +
                     "JOIN Order_Product op ON p.product_id = op.product_id " +
                     "JOIN `Order` o ON op.order_id = o.order_id " +
                     "JOIN Customer c ON o.customer_id = c.customer_id " +
                     "WHERE c.name = 'Alice Dupont' " +
                     "ORDER BY p.price DESC " +
                     "LIMIT 1")) {
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("product_name", rs.getString("product_name"));
                product.put("price", rs.getDouble("price"));
                productList.add(product);
            }
            System.out.println(productList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Most expensive product ordered by Alice Dupont: " + productList);
        assertEquals(1, productList.size());
        assertEquals("Apple", productList.get(0).get("product_name"));
        assertEquals(3.0, productList.get(0).get("price"));
    }

    /*Display customers and the total number of orders they've placed:
    SELECT c.name, COUNT(o.order_id) AS total_orders
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        GROUP BY c.name;
        ORDER BY c.name DESC
[{name=Valerie Simon, total_orders=1}, {name=Thomas Perrault, total_orders=1}, {name=Sophie Michel, total_orders=1}, {name=Quentin Lambert, total_orders=1}, {name=Pauline Robert, total_orders=1}, {name=Olivier Bernard, total_orders=1}, {name=Nicolas Gauthier, total_orders=1}, {name=Marie Leroy, total_orders=1}, {name=Luc Moreau, total_orders=1}, {name=Karine Girard, total_orders=1}, {name=Jacques Durand, total_orders=1}, {name=Isabelle Fontaine, total_orders=1}, {name=Hugo Blanchard, total_orders=1}, {name=Gabrielle Petit, total_orders=1}, {name=Francois Dubois, total_orders=1}, {name=Elise Renault, total_orders=1}, {name=David Laurent, total_orders=1}, {name=Claire Lefevre, total_orders=1}, {name=Bernard Martin, total_orders=1}, {name=Alice Dupont, total_orders=1}]
     */
    @Test
    void test_Customer_Table_Display_Customers_And_TotalNumberOfOrdersTheyPlaced() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, COUNT(o.order_id) AS total_orders " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "GROUP BY c.name " +
                     "ORDER BY c.name DESC")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("total_orders", rs.getInt("total_orders"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Customers and the total number of orders they've placed: " + customerList);
        assertEquals(20, customerList.size());
        assertEquals("Valerie Simon", customerList.get(0).get("name"));
        assertEquals(1, customerList.get(0).get("total_orders"));
        assertEquals("Thomas Perrault", customerList.get(1).get("name"));
        assertEquals(1, customerList.get(1).get("total_orders"));
    }

    /*Display the total quantity of each product ordered across all orders:
    SELECT p.product_name, SUM(op.quantity) AS total_quantity_ordered
        FROM Product p
        JOIN Order_Product op ON p.product_id = op.product_id
        GROUP BY p.product_name;
    -> [{total_quantity_ordered=9, product_name=Orange}, {total_quantity_ordered=9, product_name=Apple}, {total_quantity_ordered=7, product_name=Banana}, {total_quantity_ordered=6, product_name=Pineapple}, {total_quantity_ordered=6, product_name=Blueberry}, {total_quantity_ordered=5, product_name=Lemon}, {total_quantity_ordered=4, product_name=Grapes}, {total_quantity_ordered=4, product_name=Mango}, {total_quantity_ordered=4, product_name=Peach}, {total_quantity_ordered=4, product_name=Strawberry}]
     */
    @Test
    void test_Product_Table_Display_TotalQuantityOfEachProductOrdered_AcrossAllOrders() {
        List<Map<String, Object>> productList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT p.product_name, SUM(op.quantity) AS total_quantity_ordered " +
                     "FROM Product p " +
                     "JOIN Order_Product op ON p.product_id = op.product_id " +
                     "GROUP BY p.product_name " +
                     "ORDER BY total_quantity_ordered DESC")) {
            while (rs.next()) {
                Map<String, Object> product = new HashMap<>();
                product.put("product_name", rs.getString("product_name"));
                product.put("total_quantity_ordered", rs.getInt("total_quantity_ordered"));
                productList.add(product);
            }
            System.out.println(productList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Total quantity of each product ordered across all orders: " + productList);
        assertEquals(10, productList.size());
        assertEquals("Orange", productList.get(0).get("product_name"));
        assertEquals(9, productList.get(0).get("total_quantity_ordered"));
        assertEquals("Apple", productList.get(1).get("product_name"));
        assertEquals(9, productList.get(1).get("total_quantity_ordered"));
    }

    /*List orders where more than one type of product was ordered:
    SELECT o.order_id, COUNT(op.product_id) AS product_count
        FROM `Order` o
        JOIN Order_Product op ON o.order_id = op.order_id
        GROUP BY o.order_id
        HAVING product_count > 1;
    ->[{product_count=2, order_id=1}, {product_count=2, order_id=2}, {product_count=2, order_id=3}, {product_count=2, order_id=4}, {product_count=2, order_id=5}, {product_count=2, order_id=6}, {product_count=2, order_id=7}, {product_count=2, order_id=8}, {product_count=2, order_id=9}, {product_count=2, order_id=10}, {product_count=2, order_id=11}, {product_count=2, order_id=12}, {product_count=2, order_id=13}, {product_count=2, order_id=14}, {product_count=2, order_id=15}, {product_count=2, order_id=16}, {product_count=2, order_id=17}, {product_count=2, order_id=18}, {product_count=2, order_id=19}, {product_count=2, order_id=20}]
     */
    @Test
    void test_Order_Table_ListOrders_WhereMoreThanOneTypeOfProductWasOrdered() {
        List<Map<String, Object>> orderList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT o.order_id, COUNT(op.product_id) AS product_count " +
                     "FROM `Order` o " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "GROUP BY o.order_id " +
                     "HAVING COUNT(op.product_id) > 1 " +
                     "ORDER BY o.order_id")) {
            while (rs.next()) {
                Map<String, Object> order = new HashMap<>();
                order.put("order_id", rs.getInt("order_id"));
                order.put("product_count", rs.getInt("product_count"));
                orderList.add(order);
            }
            System.out.println(orderList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Orders where more than one type of product was ordered: " + orderList);
        assertEquals(20, orderList.size());
        assertEquals(1, orderList.get(0).get("order_id"));
        assertEquals(2, orderList.get(0).get("product_count"));
        assertEquals(2, orderList.get(1).get("order_id"));
        assertEquals(2, orderList.get(1).get("product_count"));
    }

    /*Find the customer who has spent the most overall:
    SELECT c.name, SUM(op.quantity * p.price) AS total_spent
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        JOIN Product p ON op.product_id = p.product_id
        GROUP BY c.name
        ORDER BY total_spent DESC
        LIMIT 1;
    ->[{total_spent=20.5, name=Elise Renault}]
     */
    @Test
    void test_Customer_Table_FindTheCustomer_WhoHasSpentTheMostOverall() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, SUM(op.quantity * p.price) AS total_spent " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "GROUP BY c.name " +
                     "ORDER BY total_spent DESC " +
                     "LIMIT 1")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("total_spent", rs.getDouble("total_spent"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Customer who has spent the most overall: " + customerList);
        assertEquals(1, customerList.size());
        assertEquals("Elise Renault", customerList.get(0).get("name"));
        assertEquals(20.5, customerList.get(0).get("total_spent"));
    }

    /*Display all orders that include a product costing more than a certain price (e.g., 5.00):
    SELECT DISTINCT o.order_id, c.name, p.product_name, p.price
        FROM `Order` o
        JOIN Customer c ON o.customer_id = c.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        JOIN Product p ON op.product_id = p.product_id
        WHERE p.price > 5.00;
    -> [{price=6.0, name=Elise Renault, order_id=5, product_name=Strawberry}, {price=7.25, name=Elise Renault, order_id=5, product_name=Blueberry}, {price=6.0, name=Hugo Blanchard, order_id=8, product_name=Strawberry}, {price=7.25, name=Karine Girard, order_id=11, product_name=Blueberry}, {price=7.25, name=Nicolas Gauthier, order_id=14, product_name=Blueberry}, {price=6.0, name=Olivier Bernard, order_id=15, product_name=Strawberry}, {price=6.0, name=Sophie Michel, order_id=18, product_name=Strawberry}, {price=7.25, name=Thomas Perrault, order_id=19, product_name=Blueberry}]
     */
    @Test
    void test_Order_Table_DisplayAllOrders_ThatIncludeAProductCostingMoreThan_5_00() {
        List<Map<String, Object>> orderList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT DISTINCT o.order_id, c.name, p.product_name, p.price " +
                     "FROM `Order` o " +
                     "JOIN Customer c ON o.customer_id = c.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "WHERE p.price > 5.00 " +
                     "ORDER BY o.order_id")) {
            while (rs.next()) {
                Map<String, Object> order = new HashMap<>();
                order.put("order_id", rs.getInt("order_id"));
                order.put("name", rs.getString("name"));
                order.put("product_name", rs.getString("product_name"));
                order.put("price", rs.getDouble("price"));
                orderList.add(order);
            }
            System.out.println(orderList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("All orders that include a product costing more than 5.00: " + orderList);
        assertEquals(8, orderList.size());
        assertEquals(5, orderList.get(0).get("order_id"));
        assertEquals("Elise Renault", orderList.get(0).get("name"));
        assertEquals("Strawberry", orderList.get(0).get("product_name"));
        assertEquals(6.0, orderList.get(0).get("price"));
    }

    /*List all customers who haven't placed an order:
    SELECT c.name
        FROM Customer c
        LEFT JOIN `Order` o ON c.customer_id = o.customer_id
        WHERE o.order_id IS NULL;

     */
    @Test
    void test_Customer_Table_ListAllCustomers_WhoHaventPlacedAnOrder() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name " +
                     "FROM Customer c " +
                     "LEFT JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "WHERE o.order_id IS NULL")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("All customers who haven't placed an order: " + customerList);
        assertEquals(0, customerList.size());
    }

    /*Find customers who ordered more than one of the same product:
    SELECT c.name, p.product_name, op.quantity
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        JOIN Product p ON op.product_id = p.product_id
        WHERE op.quantity > 1;
    -> [{quantity=2, name=Alice Dupont, product_name=Orange}, {quantity=3, name=Bernard Martin, product_name=Lemon}, {quantity=2, name=Claire Lefevre, product_name=Banana}, {quantity=2, name=Elise Renault, product_name=Blueberry}, {quantity=2, name=Gabrielle Petit, product_name=Apple}, {quantity=2, name=Hugo Blanchard, product_name=Pineapple}, {quantity=3, name=Jacques Durand, product_name=Apple}, {quantity=2, name=Jacques Durand, product_name=Banana}, {quantity=2, name=Marie Leroy, product_name=Orange}, {quantity=2, name=Marie Leroy, product_name=Apple}, {quantity=2, name=Nicolas Gauthier, product_name=Pineapple}, {quantity=3, name=Pauline Robert, product_name=Orange}, {quantity=2, name=Pauline Robert, product_name=Banana}, {quantity=2, name=Quentin Lambert, product_name=Mango}, {quantity=2, name=Thomas Perrault, product_name=Blueberry}]
     */
    @Test
    void test_Customer_Table_FindCustomers_WhoOrderedMoreThanOneOfTheSameProduct() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, p.product_name, op.quantity " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "JOIN Product p ON op.product_id = p.product_id " +
                     "WHERE op.quantity > 1 " +
                     "ORDER BY c.name")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("product_name", rs.getString("product_name"));
                customer.put("quantity", rs.getInt("quantity"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Customers who ordered more than one of the same product: " + customerList);
        assertEquals(15, customerList.size());
        assertEquals("Alice Dupont", customerList.get(0).get("name"));
        assertEquals("Orange", customerList.get(0).get("product_name"));
        assertEquals(2, customerList.get(0).get("quantity"));

    }

    /*Display the total number of orders placed in a specific month (e.g., August 2024):
    SELECT COUNT(order_id) AS total_orders
        FROM `Order`
        WHERE MONTH(order_date) = 8 AND YEAR(order_date) = 2024;
    ->[{total_orders=20}]
     */
    @Test
    void test_Order_Table_DisplayTotalNumberOfOrdersPlaced_InAugust2024() {
        List<Map<String, Object>> orderList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(order_id) AS total_orders " +
                     "FROM `Order` " +
                     "WHERE MONTH(order_date) = 8 AND YEAR(order_date) = 2024")) {
            while (rs.next()) {
                Map<String, Object> order = new HashMap<>();
                order.put("total_orders", rs.getInt("total_orders"));
                orderList.add(order);
            }
            System.out.println(orderList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Total number of orders placed in August 2024: " + orderList);
        assertEquals(1, orderList.size());
        assertEquals(20, orderList.get(0).get("total_orders"));
    }

    /*Display the number of distinct products ordered by each customer:
    SELECT c.name, COUNT(DISTINCT op.product_id) AS distinct_products
        FROM Customer c
        JOIN `Order` o ON c.customer_id = o.customer_id
        JOIN Order_Product op ON o.order_id = op.order_id
        GROUP BY c.name;
        -> [{name=Valerie Simon, distinct_products=2}, {name=Thomas Perrault, distinct_products=2}, {name=Sophie Michel, distinct_products=2}, {name=Quentin Lambert, distinct_products=2}, {name=Pauline Robert, distinct_products=2}, {name=Olivier Bernard, distinct_products=2}, {name=Nicolas Gauthier, distinct_products=2}, {name=Marie Leroy, distinct_products=2}, {name=Luc Moreau, distinct_products=2}, {name=Karine Girard, distinct_products=2}, {name=Jacques Durand, distinct_products=2}, {name=Isabelle Fontaine, distinct_products=2}, {name=Hugo Blanchard, distinct_products=2}, {name=Gabrielle Petit, distinct_products=2}, {name=Francois Dubois, distinct_products=2}, {name=Elise Renault, distinct_products=2}, {name=David Laurent, distinct_products=2}, {name=Claire Lefevre, distinct_products=2}, {name=Bernard Martin, distinct_products=2}, {name=Alice Dupont, distinct_products=2}]
     */
    @Test
    void test_Customer_Table_DisplayNumberOfDistinctProductsOrdered_ByEachCustomer() {
        List<Map<String, Object>> customerList = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT c.name, COUNT(DISTINCT op.product_id) AS distinct_products " +
                     "FROM Customer c " +
                     "JOIN `Order` o ON c.customer_id = o.customer_id " +
                     "JOIN Order_Product op ON o.order_id = op.order_id " +
                     "GROUP BY c.name " +
                     "ORDER BY c.name DESC")) {
            while (rs.next()) {
                Map<String, Object> customer = new HashMap<>();
                customer.put("name", rs.getString("name"));
                customer.put("distinct_products", rs.getInt("distinct_products"));
                customerList.add(customer);
            }
            System.out.println(customerList);
        } catch (SQLException e) {
            LOGGER.severe("Error querying the database: " + e.getMessage());
        }
        LOGGER.info("Number of distinct products ordered by each customer: " + customerList);
        assertEquals(20, customerList.size());
        assertEquals("Valerie Simon", customerList.get(0).get("name"));
        assertEquals(2, customerList.get(0).get("distinct_products"));
        assertEquals("Thomas Perrault", customerList.get(1).get("name"));
        assertEquals(2, customerList.get(1).get("distinct_products"));
    }


}
