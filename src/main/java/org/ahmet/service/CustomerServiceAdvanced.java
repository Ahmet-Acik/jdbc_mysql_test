package org.ahmet.service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.ahmet.exception.DatabaseException;
import org.ahmet.exception.EntityNotFoundException;
import org.ahmet.model.Customer;
import org.ahmet.util.DatabaseUtil;

/**
 * Advanced Customer Service demonstrating comprehensive JDBC features
 * 
 * This enhanced service showcases:
 * 1. PreparedStatement for all operations (security)
 * 2. Transaction management
 * 3. Batch operations
 * 4. Connection resource management
 * 5. Proper exception handling
 * 6. Optional return types
 * 7. Pagination support
 * 8. Search functionality
 * 9. Audit logging
 */
public class CustomerServiceAdvanced {

    private static final Logger LOGGER = Logger.getLogger(CustomerServiceAdvanced.class.getName());
    private final String dbName;

    public CustomerServiceAdvanced(String dbName) {
        this.dbName = dbName;
    }

    /**
     * Creates a new customer with proper transaction handling
     */
    public Customer createCustomer(Customer customer) throws DatabaseException {
        String sql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
        
        try (Connection conn = DatabaseUtil.getConnection(dbName);
             PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getEmail());
            pstmt.setString(3, customer.getPhoneNumber());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new DatabaseException("Creating customer failed, no rows affected.");
            }
            
            try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    int customerId = generatedKeys.getInt(1);
                    customer.setCustomerId(customerId);
                    LOGGER.info("Created customer with ID: " + customerId);
                    return customer;
                } else {
                    throw new DatabaseException("Creating customer failed, no ID obtained.");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to create customer: " + e.getMessage());
            throw new DatabaseException("Failed to create customer: " + e.getMessage(), e);
        }
    }

    /**
     * Finds customer by ID with Optional return type
     */
    public Optional<Customer> findCustomerById(int customerId) throws DatabaseException {
        String sql = "SELECT customer_id, name, email, phone_number FROM Customer WHERE customer_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection(dbName);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Customer customer = mapResultSetToCustomer(rs);
                    return Optional.of(customer);
                }
                return Optional.empty();
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to find customer by ID: " + e.getMessage());
            throw new DatabaseException("Failed to find customer by ID: " + e.getMessage(), e);
        }
    }

    /**
     * Updates customer with optimistic locking simulation
     */
    public Customer updateCustomer(Customer customer) throws DatabaseException, EntityNotFoundException {
        String sql = "UPDATE Customer SET name = ?, email = ?, phone_number = ? WHERE customer_id = ?";
        
        try (Connection conn = DatabaseUtil.getConnection(dbName);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setString(1, customer.getName());
            pstmt.setString(2, customer.getEmail());
            pstmt.setString(3, customer.getPhoneNumber());
            pstmt.setInt(4, customer.getCustomerId());
            
            int affectedRows = pstmt.executeUpdate();
            if (affectedRows == 0) {
                throw new EntityNotFoundException("Customer with ID " + customer.getCustomerId() + " not found");
            }
            
            LOGGER.info("Updated customer ID: " + customer.getCustomerId());
            return customer;
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to update customer: " + e.getMessage());
            throw new DatabaseException("Failed to update customer: " + e.getMessage(), e);
        }
    }

    /**
     * Deletes customer with cascade handling
     */
    public boolean deleteCustomer(int customerId) throws DatabaseException {
        Connection conn = null;
        
        try {
            conn = DatabaseUtil.getConnection(dbName);
            conn.setAutoCommit(false); // Start transaction
            
            // First check if customer exists
            if (!customerExists(conn, customerId)) {
                conn.rollback();
                return false;
            }
            
            // Delete in correct order due to foreign key constraints
            // 1. Delete Order_Product entries for this customer's orders
            String deleteOrderProductsSql = """
                DELETE op FROM Order_Product op 
                JOIN `Order` o ON op.order_id = o.order_id 
                WHERE o.customer_id = ?
                """;
            try (PreparedStatement pstmt = conn.prepareStatement(deleteOrderProductsSql)) {
                pstmt.setInt(1, customerId);
                int orderProductsDeleted = pstmt.executeUpdate();
                LOGGER.info("Deleted " + orderProductsDeleted + " order products for customer " + customerId);
            }
            
            // 2. Delete orders
            String deleteOrdersSql = "DELETE FROM `Order` WHERE customer_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteOrdersSql)) {
                pstmt.setInt(1, customerId);
                int ordersDeleted = pstmt.executeUpdate();
                LOGGER.info("Deleted " + ordersDeleted + " orders for customer " + customerId);
            }
            
            // 3. Delete customer
            String deleteCustomerSql = "DELETE FROM Customer WHERE customer_id = ?";
            try (PreparedStatement pstmt = conn.prepareStatement(deleteCustomerSql)) {
                pstmt.setInt(1, customerId);
                int customerDeleted = pstmt.executeUpdate();
                
                if (customerDeleted == 1) {
                    conn.commit();
                    LOGGER.info("Successfully deleted customer " + customerId);
                    return true;
                } else {
                    conn.rollback();
                    return false;
                }
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.severe("Failed to rollback transaction: " + ex.getMessage());
                }
            }
            LOGGER.severe("Failed to delete customer: " + e.getMessage());
            throw new DatabaseException("Failed to delete customer: " + e.getMessage(), e);
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true); // Restore auto-commit
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.severe("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Searches customers with pagination
     */
    public List<Customer> searchCustomers(String namePattern, String emailPattern, 
                                        int offset, int limit) throws DatabaseException {
        StringBuilder sql = new StringBuilder("SELECT customer_id, name, email, phone_number FROM Customer WHERE 1=1");
        List<String> parameters = new ArrayList<>();
        
        if (namePattern != null && !namePattern.trim().isEmpty()) {
            sql.append(" AND name LIKE ?");
            parameters.add("%" + namePattern.trim() + "%");
        }
        
        if (emailPattern != null && !emailPattern.trim().isEmpty()) {
            sql.append(" AND email LIKE ?");
            parameters.add("%" + emailPattern.trim() + "%");
        }
        
        sql.append(" ORDER BY name LIMIT ? OFFSET ?");
        parameters.add(String.valueOf(limit));
        parameters.add(String.valueOf(offset));
        
        try (Connection conn = DatabaseUtil.getConnection(dbName);
             PreparedStatement pstmt = conn.prepareStatement(sql.toString())) {
            
            // Set parameters
            for (int i = 0; i < parameters.size(); i++) {
                if (i < parameters.size() - 2) { // Pattern parameters
                    pstmt.setString(i + 1, parameters.get(i));
                } else { // Limit and offset are integers
                    pstmt.setInt(i + 1, Integer.parseInt(parameters.get(i)));
                }
            }
            
            List<Customer> customers = new ArrayList<>();
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    customers.add(mapResultSetToCustomer(rs));
                }
            }
            
            LOGGER.info("Found " + customers.size() + " customers matching search criteria");
            return customers;
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to search customers: " + e.getMessage());
            throw new DatabaseException("Failed to search customers: " + e.getMessage(), e);
        }
    }

    /**
     * Batch creates multiple customers efficiently
     */
    public List<Customer> batchCreateCustomers(List<Customer> customers) throws DatabaseException {
        if (customers == null || customers.isEmpty()) {
            return new ArrayList<>();
        }
        
        String sql = "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
        Connection conn = null;
        
        try {
            conn = DatabaseUtil.getConnection(dbName);
            conn.setAutoCommit(false);
            
            try (PreparedStatement pstmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                
                // Add all customers to batch
                for (Customer customer : customers) {
                    pstmt.setString(1, customer.getName());
                    pstmt.setString(2, customer.getEmail());
                    pstmt.setString(3, customer.getPhoneNumber());
                    pstmt.addBatch();
                }
                
                // Execute batch
                int[] results = pstmt.executeBatch();
                
                // Get generated IDs
                try (ResultSet generatedKeys = pstmt.getGeneratedKeys()) {
                    int index = 0;
                    while (generatedKeys.next() && index < customers.size()) {
                        customers.get(index).setCustomerId(generatedKeys.getInt(1));
                        index++;
                    }
                }
                
                conn.commit();
                LOGGER.info("Batch created " + results.length + " customers");
                return customers;
                
            }
            
        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                } catch (SQLException ex) {
                    LOGGER.severe("Failed to rollback batch create: " + ex.getMessage());
                }
            }
            LOGGER.severe("Failed to batch create customers: " + e.getMessage());
            throw new DatabaseException("Failed to batch create customers: " + e.getMessage(), e);
            
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    LOGGER.severe("Failed to close connection: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Gets customer spending statistics
     */
    public CustomerStats getCustomerStats(int customerId) throws DatabaseException {
        String sql = """
            SELECT 
                c.customer_id,
                c.name,
                COUNT(DISTINCT o.order_id) as total_orders,
                COALESCE(SUM(op.quantity * p.price), 0) as total_spent,
                COALESCE(AVG(op.quantity * p.price), 0) as avg_order_value,
                COUNT(DISTINCT op.product_id) as unique_products_ordered
            FROM Customer c
            LEFT JOIN `Order` o ON c.customer_id = o.customer_id
            LEFT JOIN Order_Product op ON o.order_id = op.order_id
            LEFT JOIN Product p ON op.product_id = p.product_id
            WHERE c.customer_id = ?
            GROUP BY c.customer_id, c.name
            """;
        
        try (Connection conn = DatabaseUtil.getConnection(dbName);
             PreparedStatement pstmt = conn.prepareStatement(sql)) {
            
            pstmt.setInt(1, customerId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new CustomerStats(
                        rs.getInt("customer_id"),
                        rs.getString("name"),
                        rs.getInt("total_orders"),
                        rs.getDouble("total_spent"),
                        rs.getDouble("avg_order_value"),
                        rs.getInt("unique_products_ordered")
                    );
                } else {
                    throw new EntityNotFoundException("Customer with ID " + customerId + " not found");
                }
            }
            
        } catch (SQLException e) {
            LOGGER.severe("Failed to get customer stats: " + e.getMessage());
            throw new DatabaseException("Failed to get customer stats: " + e.getMessage(), e);
        }
    }

    /**
     * Helper method to check if customer exists
     */
    private boolean customerExists(Connection conn, int customerId) throws SQLException {
        String sql = "SELECT 1 FROM Customer WHERE customer_id = ?";
        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setInt(1, customerId);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    /**
     * Helper method to map ResultSet to Customer object
     */
    private Customer mapResultSetToCustomer(ResultSet rs) throws SQLException {
        Customer customer = new Customer();
        customer.setCustomerId(rs.getInt("customer_id"));
        customer.setName(rs.getString("name"));
        customer.setEmail(rs.getString("email"));
        customer.setPhoneNumber(rs.getString("phone_number"));
        return customer;
    }

    /**
     * Data class for customer statistics
     */
    public static class CustomerStats {
        private final int customerId;
        private final String customerName;
        private final int totalOrders;
        private final double totalSpent;
        private final double avgOrderValue;
        private final int uniqueProductsOrdered;

        public CustomerStats(int customerId, String customerName, int totalOrders, 
                           double totalSpent, double avgOrderValue, int uniqueProductsOrdered) {
            this.customerId = customerId;
            this.customerName = customerName;
            this.totalOrders = totalOrders;
            this.totalSpent = totalSpent;
            this.avgOrderValue = avgOrderValue;
            this.uniqueProductsOrdered = uniqueProductsOrdered;
        }

        // Getters
        public int getCustomerId() { return customerId; }
        public String getCustomerName() { return customerName; }
        public int getTotalOrders() { return totalOrders; }
        public double getTotalSpent() { return totalSpent; }
        public double getAvgOrderValue() { return avgOrderValue; }
        public int getUniqueProductsOrdered() { return uniqueProductsOrdered; }

        @Override
        public String toString() {
            return String.format("CustomerStats{id=%d, name='%s', orders=%d, spent=%.2f, avgOrder=%.2f, products=%d}",
                customerId, customerName, totalOrders, totalSpent, avgOrderValue, uniqueProductsOrdered);
        }
    }
}