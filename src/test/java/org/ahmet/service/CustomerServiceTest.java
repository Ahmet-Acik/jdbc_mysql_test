package org.ahmet.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import org.ahmet.dao.CustomerDao;
import org.ahmet.model.Customer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("Customer Service Tests")
class CustomerServiceTest {

    @Mock
    private CustomerDao customerDao;

    private CustomerService customerService;
    private Customer validCustomer;

    @BeforeEach
    void setUp() {
        customerService = new CustomerService(customerDao);
        validCustomer = new Customer("John Doe", "john.doe@example.com", "1234567890");
    }

    @Test
    @DisplayName("Should create customer successfully")
    void createCustomer_Success() throws SQLException {
        // Given
        when(customerDao.findByEmail(validCustomer.getEmail())).thenReturn(Optional.empty());
        when(customerDao.createCustomer(validCustomer)).thenReturn(1);

        // When
        Customer result = customerService.createCustomer(validCustomer);

        // Then
        assertNotNull(result);
        assertEquals(validCustomer.getEmail(), result.getEmail());
        verify(customerDao).findByEmail(validCustomer.getEmail());
        verify(customerDao).createCustomer(validCustomer);
    }

    @Test
    @DisplayName("Should throw exception when creating customer with existing email")
    void createCustomer_ExistingEmail_ThrowsException() throws SQLException {
        // Given
        when(customerDao.findByEmail(validCustomer.getEmail())).thenReturn(Optional.of(validCustomer));

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(validCustomer));
        
        assertTrue(exception.getMessage().contains("already exists"));
        verify(customerDao).findByEmail(validCustomer.getEmail());
        verify(customerDao, never()).createCustomer(any(Customer.class));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t", "\n"})
    @DisplayName("Should throw exception for invalid names")
    void createCustomer_InvalidName_ThrowsException(String invalidName) {
        // Given
        validCustomer.setName(invalidName);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(validCustomer));
        
        assertEquals("Customer name is required", exception.getMessage());
    }

    @ParameterizedTest
    @ValueSource(strings = {"invalid-email", "test@", "@example.com", "test"})
    @DisplayName("Should throw exception for invalid email formats")
    void createCustomer_InvalidEmail_ThrowsException(String invalidEmail) {
        // Given
        validCustomer.setEmail(invalidEmail);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.createCustomer(validCustomer));
        
        assertEquals("Invalid email format", exception.getMessage());
    }

    @Test
    @DisplayName("Should retrieve customer by ID successfully")
    void getCustomerById_Success() throws SQLException {
        // Given
        int customerId = 1;
        when(customerDao.findById(customerId)).thenReturn(Optional.of(validCustomer));

        // When
        Optional<Customer> result = customerService.getCustomerById(customerId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(validCustomer, result.get());
        verify(customerDao).findById(customerId);
    }

    @Test
    @DisplayName("Should return empty optional when customer not found")
    void getCustomerById_NotFound_ReturnsEmpty() throws SQLException {
        // Given
        int customerId = 999;
        when(customerDao.findById(customerId)).thenReturn(Optional.empty());

        // When
        Optional<Customer> result = customerService.getCustomerById(customerId);

        // Then
        assertFalse(result.isPresent());
        verify(customerDao).findById(customerId);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, -1, -100})
    @DisplayName("Should throw exception for invalid customer IDs")
    void getCustomerById_InvalidId_ThrowsException(int invalidId) {
        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.getCustomerById(invalidId));
        
        assertEquals("Customer ID must be positive", exception.getMessage());
    }

    @Test
    @DisplayName("Should retrieve all customers successfully")
    void getAllCustomers_Success() throws SQLException {
        // Given
        Customer customer2 = new Customer("Jane Smith", "jane@example.com", "0987654321");
        List<Customer> customers = Arrays.asList(validCustomer, customer2);
        when(customerDao.findAll()).thenReturn(customers);

        // When
        List<Customer> result = customerService.getAllCustomers();

        // Then
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains(validCustomer));
        assertTrue(result.contains(customer2));
        verify(customerDao).findAll();
    }

    @Test
    @DisplayName("Should update customer successfully")
    void updateCustomer_Success() throws SQLException {
        // Given
        validCustomer.setCustomerId(1);
        when(customerDao.updateCustomer(validCustomer)).thenReturn(true);

        // When
        boolean result = customerService.updateCustomer(validCustomer);

        // Then
        assertTrue(result);
        verify(customerDao).updateCustomer(validCustomer);
    }

    @Test
    @DisplayName("Should throw exception when updating customer without ID")
    void updateCustomer_NoId_ThrowsException() {
        // Given
        validCustomer.setCustomerId(null);

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.updateCustomer(validCustomer));
        
        assertTrue(exception.getMessage().contains("Valid customer ID is required"));
    }

    @Test
    @DisplayName("Should delete customer successfully")
    void deleteCustomer_Success() throws SQLException {
        // Given
        int customerId = 1;
        when(customerDao.deleteCustomer(customerId)).thenReturn(true);

        // When
        boolean result = customerService.deleteCustomer(customerId);

        // Then
        assertTrue(result);
        verify(customerDao).deleteCustomer(customerId);
    }

    @Test
    @DisplayName("Should handle SQL exceptions properly")
    void createCustomer_SQLException_ThrowsRuntimeException() throws SQLException {
        // Given
        when(customerDao.findByEmail(validCustomer.getEmail())).thenThrow(new SQLException("Database error"));

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> customerService.createCustomer(validCustomer));
        
        assertEquals("Failed to create customer", exception.getMessage());
        assertTrue(exception.getCause() instanceof SQLException);
    }

    @Test
    @DisplayName("Should create customer with special characters in name")
    void createCustomer_SpecialCharName_Success() throws SQLException {
    Customer specialCharCustomer = new Customer("Jöhn Dœ!@#", "special@example.com", "1234567890");
    when(customerDao.findByEmail(specialCharCustomer.getEmail())).thenReturn(Optional.empty());
    when(customerDao.createCustomer(specialCharCustomer)).thenReturn(2);
    Customer result = customerService.createCustomer(specialCharCustomer);
    assertNotNull(result);
    assertEquals("Jöhn Dœ!@#", result.getName());
    verify(customerDao).findByEmail(specialCharCustomer.getEmail());
    verify(customerDao).createCustomer(specialCharCustomer);
    }

    @Test
    @DisplayName("Should update customer with very long email address")
    void updateCustomer_LongEmail_Success() throws SQLException {
    String longEmail = "a".repeat(100) + "@example.com";
    validCustomer.setCustomerId(2);
    validCustomer.setEmail(longEmail);
    when(customerDao.updateCustomer(validCustomer)).thenReturn(true);
    boolean result = customerService.updateCustomer(validCustomer);
    assertTrue(result);
    verify(customerDao).updateCustomer(validCustomer);
    }

    @Test
    @DisplayName("Should delete customer with very large ID value")
    void deleteCustomer_LargeId_Success() throws SQLException {
    int largeId = Integer.MAX_VALUE;
    when(customerDao.deleteCustomer(largeId)).thenReturn(true);
    boolean result = customerService.deleteCustomer(largeId);
    assertTrue(result);
    verify(customerDao).deleteCustomer(largeId);
    }

    @Test
    @DisplayName("Should return empty list when no customers exist")
    void getAllCustomers_EmptyList_ReturnsEmpty() throws SQLException {
    when(customerDao.findAll()).thenReturn(List.of());
    List<Customer> result = customerService.getAllCustomers();
    assertNotNull(result);
    assertTrue(result.isEmpty());
    }
    
      @Test
    @DisplayName("Should trim leading/trailing spaces in fields")
    void createCustomer_TrimmedFields_Success() throws SQLException {
        Customer trimmedCustomer = new Customer("  John Doe  ", "  john.doe@example.com  ", "1234567890");
        when(customerDao.findByEmail("  john.doe@example.com  ")).thenReturn(Optional.empty());
        when(customerDao.createCustomer(trimmedCustomer)).thenReturn(3);
        Customer result = customerService.createCustomer(trimmedCustomer);
        assertNotNull(result);
        assertEquals("John Doe", result.getName().trim());
        assertEquals("john.doe@example.com", result.getEmail().trim());
        assertEquals("1234567890", result.getPhoneNumber().trim());
    }

    @Test
    @DisplayName("Should throw exception for non-numeric phone number")
    void createCustomer_NonNumericPhone_ThrowsException() {
    validCustomer.setPhoneNumber("123-abc-!@#");
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
        () -> customerService.createCustomer(validCustomer));
    assertEquals("Customer phone number must be numeric", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when updating customer with null phone number")
    void updateCustomer_NullPhone_ThrowsException() {
    validCustomer.setCustomerId(3);
    validCustomer.setPhoneNumber(null);
    IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> customerService.updateCustomer(validCustomer));
    assertEquals("Customer phone number is required", exception.getMessage());
    }

    @Test
    @DisplayName("Should throw exception when deleting customer with zero ID")
    void deleteCustomer_ZeroId_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> customerService.deleteCustomer(0));
        assertEquals("Customer ID must be positive", exception.getMessage());
    }
}