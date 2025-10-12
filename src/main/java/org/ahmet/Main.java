package org.ahmet;

import org.ahmet.config.DatabaseConfig;
import org.ahmet.database.DatabaseSetup;
import org.ahmet.model.Customer;
import org.ahmet.service.CustomerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Main application class demonstrating JDBC best practices.
 */
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        LOGGER.info("Starting JDBC MySQL Test Application");

        try {
            // Initialize database
            String dbName = DatabaseConfig.getDatabaseName();
            DatabaseSetup.initializeDatabase(dbName);

            // Demonstrate customer operations
            demonstrateCustomerOperations();

        } catch (Exception e) {
            LOGGER.error("Application failed to start", e);
            System.exit(1);
        } finally {
            // Clean shutdown
            DatabaseConfig.shutdown();
            LOGGER.info("Application shutdown complete");
        }
    }

    private static void demonstrateCustomerOperations() {
        LOGGER.info("Demonstrating customer operations");
        CustomerService customerService = new CustomerService();

        try {
            // Create customers
            Customer customer1 = new Customer("John Doe", "john.doe@email.com", "1234567890");
            Customer customer2 = new Customer("Jane Smith", "jane.smith@email.com", "0987654321");

            customerService.createCustomer(customer1);
            customerService.createCustomer(customer2);

            // Retrieve and display all customers
            List<Customer> customers = customerService.getAllCustomers();
            LOGGER.info("All customers:");
            customers.forEach(customer -> LOGGER.info("  {}", customer));

            // Update customer
            if (!customers.isEmpty()) {
                Customer firstCustomer = customers.get(0);
                firstCustomer.setPhoneNumber("555-0123");
                customerService.updateCustomer(firstCustomer);
                LOGGER.info("Updated customer: {}", firstCustomer);
            }

        } catch (Exception e) {
            LOGGER.error("Error during customer operations demonstration", e);
        }
    }
}