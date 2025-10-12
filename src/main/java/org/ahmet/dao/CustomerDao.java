package org.ahmet.dao;

import org.ahmet.model.Customer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Data Access Object for Customer entity operations.
 */
public class CustomerDao extends BaseDao {
    private static final Logger LOGGER = Logger.getLogger(CustomerDao.class.getName());
    
    private static final String INSERT_CUSTOMER = 
        "INSERT INTO Customer (name, email, phone_number) VALUES (?, ?, ?)";
    
    private static final String SELECT_CUSTOMER_BY_ID = 
        "SELECT customer_id, name, email, phone_number FROM Customer WHERE customer_id = ?";
    
    private static final String SELECT_ALL_CUSTOMERS = 
        "SELECT customer_id, name, email, phone_number FROM Customer ORDER BY name";
    
    private static final String UPDATE_CUSTOMER = 
        "UPDATE Customer SET name = ?, email = ?, phone_number = ? WHERE customer_id = ?";
    
    private static final String DELETE_CUSTOMER = 
        "DELETE FROM Customer WHERE customer_id = ?";
    
    private static final String SELECT_CUSTOMER_BY_EMAIL = 
        "SELECT customer_id, name, email, phone_number FROM Customer WHERE email = ?";

    /**
     * Creates a new customer in the database.
     * @param customer Customer to create
     * @return Generated customer ID
     * @throws SQLException if creation fails
     */
    public int createCustomer(Customer customer) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet generatedKeys = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(INSERT_CUSTOMER, Statement.RETURN_GENERATED_KEYS);
            statement.setString(1, customer.getName());
            statement.setString(2, customer.getEmail());
            statement.setString(3, customer.getPhoneNumber());
            
            int affectedRows = statement.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Creating customer failed, no rows affected.");
            }
            
            generatedKeys = statement.getGeneratedKeys();
            if (generatedKeys.next()) {
                int customerId = generatedKeys.getInt(1);
                customer.setCustomerId(customerId);
                LOGGER.info("Customer created with ID: " + customerId);
                return customerId;
            } else {
                throw new SQLException("Creating customer failed, no ID obtained.");
            }
        } finally {
            closeResources(connection, statement, generatedKeys);
        }
    }

    /**
     * Retrieves a customer by ID.
     * @param customerId Customer ID
     * @return Optional containing the customer if found
     * @throws SQLException if retrieval fails
     */
    public Optional<Customer> findById(int customerId) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(SELECT_CUSTOMER_BY_ID);
            statement.setInt(1, customerId);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(mapResultSetToCustomer(resultSet));
            }
            return Optional.empty();
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Retrieves a customer by email.
     * @param email Customer email
     * @return Optional containing the customer if found
     * @throws SQLException if retrieval fails
     */
    public Optional<Customer> findByEmail(String email) throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(SELECT_CUSTOMER_BY_EMAIL);
            statement.setString(1, email);
            resultSet = statement.executeQuery();
            
            if (resultSet.next()) {
                return Optional.of(mapResultSetToCustomer(resultSet));
            }
            return Optional.empty();
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Retrieves all customers.
     * @return List of all customers
     * @throws SQLException if retrieval fails
     */
    public List<Customer> findAll() throws SQLException {
        Connection connection = null;
        PreparedStatement statement = null;
        ResultSet resultSet = null;
        List<Customer> customers = new ArrayList<>();
        
        try {
            connection = getConnection();
            statement = connection.prepareStatement(SELECT_ALL_CUSTOMERS);
            resultSet = statement.executeQuery();
            
            while (resultSet.next()) {
                customers.add(mapResultSetToCustomer(resultSet));
            }
            return customers;
        } finally {
            closeResources(connection, statement, resultSet);
        }
    }

    /**
     * Updates an existing customer.
     * @param customer Customer with updated information
     * @return true if update was successful
     * @throws SQLException if update fails
     */
    public boolean updateCustomer(Customer customer) throws SQLException {
        int rowsAffected = executeUpdate(UPDATE_CUSTOMER, 
            customer.getName(), 
            customer.getEmail(), 
            customer.getPhoneNumber(), 
            customer.getCustomerId());
        
        boolean success = rowsAffected > 0;
        if (success) {
            LOGGER.info("Customer updated: " + customer.getCustomerId());
        }
        return success;
    }

    /**
     * Deletes a customer by ID.
     * @param customerId Customer ID to delete
     * @return true if deletion was successful
     * @throws SQLException if deletion fails
     */
    public boolean deleteCustomer(int customerId) throws SQLException {
        int rowsAffected = executeUpdate(DELETE_CUSTOMER, customerId);
        boolean success = rowsAffected > 0;
        if (success) {
            LOGGER.info("Customer deleted: " + customerId);
        }
        return success;
    }

    /**
     * Maps a ResultSet row to a Customer object.
     * @param resultSet ResultSet containing customer data
     * @return Customer object
     * @throws SQLException if mapping fails
     */
    private Customer mapResultSetToCustomer(ResultSet resultSet) throws SQLException {
        return new Customer(
            resultSet.getInt("customer_id"),
            resultSet.getString("name"),
            resultSet.getString("email"),
            resultSet.getString("phone_number")
        );
    }
}