package org.ahmet.service;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.ahmet.dao.CustomerDao;
import org.ahmet.model.Customer;

/**
 * Service layer for customer operations.
 * Handles business logic and transaction management.
 */
public class CustomerService {
    private static final Logger LOGGER = Logger.getLogger(CustomerService.class.getName());
    private final CustomerDao customerDao;

    public CustomerService() {
        this.customerDao = new CustomerDao();
    }

    // Constructor injection for testing
    public CustomerService(CustomerDao customerDao) {
        this.customerDao = customerDao;
    }

    /**
     * Creates a new customer after validation.
     * @param customer Customer to create
     * @return Created customer with ID
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if creation fails
     */
    public Customer createCustomer(Customer customer) {
        validateCustomer(customer);
        
        try {
            // Check if email already exists
            Optional<Customer> existingCustomer = customerDao.findByEmail(customer.getEmail());
            if (existingCustomer.isPresent()) {
                throw new IllegalArgumentException("Customer with email " + customer.getEmail() + " already exists");
            }

            customerDao.createCustomer(customer);
            LOGGER.info("Customer created successfully: " + customer.getEmail());
            return customer;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating customer", e);
            throw new RuntimeException("Failed to create customer", e);
        }
    }

    /**
     * Retrieves a customer by ID.
     * @param customerId Customer ID
     * @return Customer if found
     * @throws IllegalArgumentException if ID is invalid
     * @throws RuntimeException if retrieval fails
     */
    public Optional<Customer> getCustomerById(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }

        try {
            return customerDao.findById(customerId);
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving customer by ID: " + customerId, e);
            throw new RuntimeException("Failed to retrieve customer", e);
        }
    }

    /**
     * Retrieves all customers.
     * @return List of all customers
     * @throws RuntimeException if retrieval fails
     */
    public List<Customer> getAllCustomers() {
        try {
            return customerDao.findAll();
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error retrieving all customers", e);
            throw new RuntimeException("Failed to retrieve customers", e);
        }
    }

    /**
     * Updates an existing customer.
     * @param customer Customer with updated information
     * @return true if update successful
     * @throws IllegalArgumentException if validation fails
     * @throws RuntimeException if update fails
     */
    public boolean updateCustomer(Customer customer) {
        validateCustomer(customer);
        
        if (customer.getCustomerId() == null || customer.getCustomerId() <= 0) {
            throw new IllegalArgumentException("Valid customer ID is required for update");
        }

        try {
            boolean success = customerDao.updateCustomer(customer);
            if (success) {
                LOGGER.info("Customer updated successfully: " + customer.getCustomerId());
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error updating customer: " + customer.getCustomerId(), e);
            throw new RuntimeException("Failed to update customer", e);
        }
    }

    /**
     * Deletes a customer by ID.
     * @param customerId Customer ID to delete
     * @return true if deletion successful
     * @throws IllegalArgumentException if ID is invalid
     * @throws RuntimeException if deletion fails
     */
    public boolean deleteCustomer(int customerId) {
        if (customerId <= 0) {
            throw new IllegalArgumentException("Customer ID must be positive");
        }

        try {
            boolean success = customerDao.deleteCustomer(customerId);
            if (success) {
                LOGGER.info("Customer deleted successfully: " + customerId);
            }
            return success;
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error deleting customer: " + customerId, e);
            throw new RuntimeException("Failed to delete customer", e);
        }
    }

    /**
     * Validates customer data.
     * @param customer Customer to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validateCustomer(Customer customer) {
        if (customer == null) {
            throw new IllegalArgumentException("Customer cannot be null");
        }

        if (customer.getName() == null || customer.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer name is required");
        }

        if (customer.getEmail() == null || customer.getEmail().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer email is required");
        }

        if (!isValidEmail(customer.getEmail())) {
            throw new IllegalArgumentException("Invalid email format");
        }

        if (customer.getPhoneNumber() == null || customer.getPhoneNumber().trim().isEmpty()) {
            throw new IllegalArgumentException("Customer phone number is required");
        }
        if (!customer.getPhoneNumber().matches("\\d+")) {
            throw new IllegalArgumentException("Customer phone number must be numeric");
        }
    }

    /**
     * Basic email validation.
     * @param email Email to validate
     * @return true if valid
     */
    private boolean isValidEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            return false;
        }
        
        // Basic email validation rules
        int atIndex = email.indexOf("@");
        int lastDotIndex = email.lastIndexOf(".");
        
        // Must contain exactly one @ symbol
        if (atIndex <= 0 || atIndex != email.lastIndexOf("@")) {
            return false;
        }
        
        // Must have at least one character before @
        if (atIndex == 0) {
            return false;
        }
        
        // Must have a dot after @ and at least one character between @ and dot
        if (lastDotIndex <= atIndex + 1) {
            return false;
        }
        
        // Must have at least one character after the last dot
        if (lastDotIndex >= email.length() - 1) {
            return false;
        }
        
        return true;
    }
}