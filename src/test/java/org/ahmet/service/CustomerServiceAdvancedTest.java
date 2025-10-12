package org.ahmet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.logging.Logger;

import org.ahmet.database.DatabaseSetup;
import org.ahmet.exception.DatabaseException;
import org.ahmet.exception.EntityNotFoundException;
import org.ahmet.model.Customer;
import org.ahmet.service.CustomerServiceAdvanced.CustomerStats;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test class for CustomerServiceAdvanced
 * 
 * This demonstrates advanced JDBC patterns through service layer testing:
 * 1. CRUD operations with proper exception handling
 * 2. Transaction management
 * 3. Batch operations
 * 4. Search with pagination
 * 5. Complex aggregation queries
 */
public class CustomerServiceAdvancedTest {

    private static final Logger LOGGER = Logger.getLogger(CustomerServiceAdvancedTest.class.getName());
    private static final String dbName = "testdb_integration";
    private CustomerServiceAdvanced customerService;

    @BeforeAll
    static void setUp() throws SQLException, IOException {
        DatabaseSetup.dropDatabase(dbName);
        DatabaseSetup.createDatabase(dbName);

        Flyway flyway = Flyway.configure()
                .dataSource(org.ahmet.util.DatabaseUtil.getDataSource(dbName))
                .baselineOnMigrate(true)
                .load();
        flyway.migrate();
    }

    @BeforeEach
    void resetDatabase() throws SQLException {
        Flyway flyway = Flyway.configure()
                .dataSource(org.ahmet.util.DatabaseUtil.getDataSource(dbName))
                .cleanDisabled(false)
                .load();
        flyway.clean();
        flyway.migrate();
        
        customerService = new CustomerServiceAdvanced(dbName);
    }

    @Test
    void testCreateCustomer_Success() throws DatabaseException {
        // Arrange
        Customer newCustomer = new Customer();
        newCustomer.setName("John Doe");
        newCustomer.setEmail("john.doe@example.com");
        newCustomer.setPhoneNumber("+1234567890");

        // Act
        Customer createdCustomer = customerService.createCustomer(newCustomer);

        // Assert
        assertNotNull(createdCustomer);
        assertTrue(createdCustomer.getCustomerId() > 0);
        assertEquals("John Doe", createdCustomer.getName());
        assertEquals("john.doe@example.com", createdCustomer.getEmail());
        assertEquals("+1234567890", createdCustomer.getPhoneNumber());
        
        LOGGER.info("Created customer: " + createdCustomer);
    }

    @Test
    void testCreateCustomer_DuplicateEmail_ThrowsException() throws DatabaseException {
        // Arrange - create first customer
        Customer firstCustomer = new Customer();
        firstCustomer.setName("First Customer");
        firstCustomer.setEmail("duplicate@example.com");
        firstCustomer.setPhoneNumber("+1111111111");
        customerService.createCustomer(firstCustomer);

        // Arrange - try to create second customer with same email
        Customer secondCustomer = new Customer();
        secondCustomer.setName("Second Customer");
        secondCustomer.setEmail("duplicate@example.com"); // Duplicate email
        secondCustomer.setPhoneNumber("+2222222222");

        // Act & Assert
        assertThrows(DatabaseException.class, () -> {
            customerService.createCustomer(secondCustomer);
        });
    }

    @Test
    void testFindCustomerById_Exists() throws DatabaseException {
        // Arrange - use existing customer from migration (Alice Dupont, ID=1)
        int existingCustomerId = 1;

        // Act
        Optional<Customer> foundCustomer = customerService.findCustomerById(existingCustomerId);

        // Assert
        assertTrue(foundCustomer.isPresent());
        assertEquals(existingCustomerId, foundCustomer.get().getCustomerId());
        assertEquals("Alice Dupont", foundCustomer.get().getName());
        assertEquals("alice.dupont@gmail.com", foundCustomer.get().getEmail());
        
        LOGGER.info("Found customer: " + foundCustomer.get());
    }

    @Test
    void testFindCustomerById_NotExists() throws DatabaseException {
        // Act
        Optional<Customer> foundCustomer = customerService.findCustomerById(99999);

        // Assert
        assertFalse(foundCustomer.isPresent());
    }

    @Test
    void testUpdateCustomer_Success() throws DatabaseException {
        // Arrange - get existing customer
        Optional<Customer> existingCustomer = customerService.findCustomerById(1);
        assertTrue(existingCustomer.isPresent());
        
        Customer customerToUpdate = existingCustomer.get();
        customerToUpdate.setName("Alice Dupont Updated");
        customerToUpdate.setEmail("alice.updated@gmail.com");
        customerToUpdate.setPhoneNumber("+33987654321");

        // Act
        Customer updatedCustomer = customerService.updateCustomer(customerToUpdate);

        // Assert
        assertNotNull(updatedCustomer);
        assertEquals("Alice Dupont Updated", updatedCustomer.getName());
        assertEquals("alice.updated@gmail.com", updatedCustomer.getEmail());
        assertEquals("+33987654321", updatedCustomer.getPhoneNumber());

        // Verify in database
        Optional<Customer> verifyCustomer = customerService.findCustomerById(1);
        assertTrue(verifyCustomer.isPresent());
        assertEquals("Alice Dupont Updated", verifyCustomer.get().getName());
        
        LOGGER.info("Updated customer: " + updatedCustomer);
    }

    @Test
    void testUpdateCustomer_NotExists_ThrowsException() throws DatabaseException {
        // Arrange
        Customer nonExistentCustomer = new Customer();
        nonExistentCustomer.setCustomerId(99999);
        nonExistentCustomer.setName("Non Existent");
        nonExistentCustomer.setEmail("nonexistent@example.com");
        nonExistentCustomer.setPhoneNumber("+0000000000");

        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            customerService.updateCustomer(nonExistentCustomer);
        });
    }

    @Test
    void testDeleteCustomer_Success() throws DatabaseException {
        // Arrange - create a customer without orders for easier deletion
        Customer newCustomer = new Customer();
        newCustomer.setName("To Be Deleted");
        newCustomer.setEmail("delete@example.com");
        newCustomer.setPhoneNumber("+9999999999");
        Customer createdCustomer = customerService.createCustomer(newCustomer);

        // Act
        boolean deleted = customerService.deleteCustomer(createdCustomer.getCustomerId());

        // Assert
        assertTrue(deleted);

        // Verify customer no longer exists
        Optional<Customer> verifyCustomer = customerService.findCustomerById(createdCustomer.getCustomerId());
        assertFalse(verifyCustomer.isPresent());
        
        LOGGER.info("Successfully deleted customer ID: " + createdCustomer.getCustomerId());
    }

    @Test
    void testDeleteCustomer_WithOrders_Success() throws DatabaseException {
        // Arrange - customer 1 (Alice Dupont) has orders in the migration data
        int customerIdWithOrders = 1;

        // Verify customer exists and has orders
        Optional<Customer> existingCustomer = customerService.findCustomerById(customerIdWithOrders);
        assertTrue(existingCustomer.isPresent());

        // Act
        boolean deleted = customerService.deleteCustomer(customerIdWithOrders);

        // Assert
        assertTrue(deleted);

        // Verify customer no longer exists
        Optional<Customer> verifyCustomer = customerService.findCustomerById(customerIdWithOrders);
        assertFalse(verifyCustomer.isPresent());
        
        LOGGER.info("Successfully deleted customer with orders, ID: " + customerIdWithOrders);
    }

    @Test
    void testDeleteCustomer_NotExists() throws DatabaseException {
        // Act
        boolean deleted = customerService.deleteCustomer(99999);

        // Assert
        assertFalse(deleted);
    }

    @Test
    void testSearchCustomers_ByName() throws DatabaseException {
        // Act - search for customers with "Alice" in name
        List<Customer> results = customerService.searchCustomers("Alice", null, 0, 10);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().anyMatch(c -> c.getName().contains("Alice")));
        
        LOGGER.info("Found " + results.size() + " customers with 'Alice' in name");
    }

    @Test
    void testSearchCustomers_ByEmail() throws DatabaseException {
        // Act - search for customers with "@gmail.com" in email
        List<Customer> results = customerService.searchCustomers(null, "@gmail.com", 0, 10);

        // Assert
        assertNotNull(results);
        assertFalse(results.isEmpty());
        assertTrue(results.stream().allMatch(c -> c.getEmail().contains("@gmail.com")));
        
        LOGGER.info("Found " + results.size() + " customers with '@gmail.com' in email");
    }

    @Test
    void testSearchCustomers_WithPagination() throws DatabaseException {
        // Act - get first page
        List<Customer> firstPage = customerService.searchCustomers(null, null, 0, 5);
        
        // Act - get second page
        List<Customer> secondPage = customerService.searchCustomers(null, null, 5, 5);

        // Assert
        assertNotNull(firstPage);
        assertNotNull(secondPage);
        assertTrue(firstPage.size() <= 5);
        assertTrue(secondPage.size() <= 5);
        
        // Ensure no overlap between pages (assuming enough customers)
        if (!firstPage.isEmpty() && !secondPage.isEmpty()) {
            assertFalse(firstPage.get(0).getCustomerId() == secondPage.get(0).getCustomerId());
        }
        
        LOGGER.info("First page: " + firstPage.size() + " customers, Second page: " + secondPage.size() + " customers");
    }

    @Test
    void testBatchCreateCustomers_Success() throws DatabaseException {
        // Arrange
        List<Customer> customersToCreate = Arrays.asList(
            createCustomer("Batch Customer 1", "batch1@example.com", "+1111111111"),
            createCustomer("Batch Customer 2", "batch2@example.com", "+2222222222"),
            createCustomer("Batch Customer 3", "batch3@example.com", "+3333333333"),
            createCustomer("Batch Customer 4", "batch4@example.com", "+4444444444"),
            createCustomer("Batch Customer 5", "batch5@example.com", "+5555555555")
        );

        // Act
        List<Customer> createdCustomers = customerService.batchCreateCustomers(customersToCreate);

        // Assert
        assertNotNull(createdCustomers);
        assertEquals(5, createdCustomers.size());
        
        // Verify all customers have IDs assigned
        for (Customer customer : createdCustomers) {
            assertTrue(customer.getCustomerId() > 0);
            LOGGER.info("Batch created customer: " + customer.getName() + " (ID: " + customer.getCustomerId() + ")");
        }

        // Verify customers exist in database
        for (Customer customer : createdCustomers) {
            Optional<Customer> verifyCustomer = customerService.findCustomerById(customer.getCustomerId());
            assertTrue(verifyCustomer.isPresent());
            assertEquals(customer.getName(), verifyCustomer.get().getName());
        }
        
        LOGGER.info("Successfully batch created " + createdCustomers.size() + " customers");
    }

    @Test
    void testBatchCreateCustomers_EmptyList() throws DatabaseException {
        // Act
        List<Customer> result = customerService.batchCreateCustomers(Arrays.asList());

        // Assert
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void testGetCustomerStats_WithOrders() throws DatabaseException {
        // Act - get stats for customer 1 (Alice Dupont who has orders)
        CustomerStats stats = customerService.getCustomerStats(1);

        // Assert
        assertNotNull(stats);
        assertEquals(1, stats.getCustomerId());
        assertEquals("Alice Dupont", stats.getCustomerName());
        assertTrue(stats.getTotalOrders() > 0, "Alice should have orders");
        assertTrue(stats.getTotalSpent() > 0, "Alice should have spent money");
        assertTrue(stats.getUniqueProductsOrdered() > 0, "Alice should have ordered products");
        
        if (stats.getTotalOrders() > 0) {
            assertTrue(stats.getAvgOrderValue() > 0, "Average order value should be positive");
        }
        
        LOGGER.info("Customer stats: " + stats);
    }

    @Test
    void testGetCustomerStats_NotExists() {
        // Act & Assert
        assertThrows(EntityNotFoundException.class, () -> {
            customerService.getCustomerStats(99999);
        });
    }

    // Helper method to create Customer objects
    private Customer createCustomer(String name, String email, String phone) {
        Customer customer = new Customer();
        customer.setName(name);
        customer.setEmail(email);
        customer.setPhoneNumber(phone);
        return customer;
    }
}