# JDBC MySQL Test Application

A Java application demonstrating JDBC best practices with MySQL database integration.

## 🏗️ Architecture

This project follows enterprise Java best practices with a layered architecture:

```
src/main/java/org/ahmet/
├── Main.java                    # Application entry point
├── config/
│   └── DatabaseConfig.java     # Database configuration and connection pooling
├── dao/
│   ├── BaseDao.java            # Base DAO with common database operations
│   └── CustomerDao.java        # Customer-specific data access operations
├── database/
│   └── DatabaseSetup.java      # Database initialization and migrations
├── exception/
│   ├── DatabaseException.java  # Custom database exceptions
│   └── EntityNotFoundException.java
├── model/
│   ├── Customer.java           # Customer entity
│   └── Product.java            # Product entity
└── service/
    └── CustomerService.java    # Business logic layer
```

## ✨ Features

- **Security First**: Prepared statements prevent SQL injection
- **Connection Pooling**: HikariCP for efficient database connections
- **Database Migrations**: Flyway for version-controlled schema changes
- **Proper Logging**: SLF4J with Logback for structured logging
- **Exception Handling**: Custom exceptions with proper error propagation
- **Unit Testing**: JUnit 5 with Mockito for comprehensive testing
- **Environment Configuration**: Externalized configuration with environment variables
- **Docker Support**: Containerized MySQL for development

## 🚀 Quick Start

### Prerequisites

- Java 17+
- Maven 3.6+
- Docker and Docker Compose (for development database)

### Development Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd JdbcMysqlTest
   ```

2. **Set up development environment**
   ```bash
   ./setup-dev-env.sh
   ```

3. **Configure environment variables** (optional)
   ```bash
   cp .env.template .env
   # Edit .env with preferred settings
   ```

4. **Run the application**
   ```bash
   mvn clean compile exec:java -Dexec.mainClass=org.ahmet.Main
   ```

### Manual Database Setup

If you prefer not to use Docker:

1. Install MySQL 8.0+
2. Create database: `CREATE DATABASE testdb;`
3. Update `src/main/resources/database.properties` with credentials
4. Run migrations: `mvn flyway:migrate`

## 🧪 Testing

### Unit Tests
```bash
mvn test
```

### Integration Tests
```bash
mvn failsafe:integration-test
```

### Code Coverage
```bash
mvn jacoco:report
# Open target/site/jacoco/index.html
```

## 🔧 Configuration

### Database Properties

The application uses environment variables for configuration:

```properties
# Database connection
DB_HOST=localhost
DB_PORT=3306
DB_NAME=testdb
DB_USER=root
DB_PASSWORD=your_password

# Connection pool settings
DB_POOL_SIZE=10
DB_CONNECTION_TIMEOUT=30000
DB_IDLE_TIMEOUT=600000
DB_MAX_LIFETIME=1800000
```

### Logging Configuration

Logging is configured in `src/main/resources/logback.xml`:
- Console output for development
- File rotation for production
- Configurable log levels per package

## 📊 Database Schema

The application manages these entities:

- **Customer**: Customer information and contact details
- **Product**: Product catalog with pricing
- **Order**: Customer orders with status tracking
- **Order_Product**: Order line items with quantities and pricing

Schema changes are managed through Flyway migrations in `src/main/resources/db/migration/`.

## 🛡️ Security Best Practices

- **SQL Injection Prevention**: All queries use prepared statements
- **Connection Security**: SSL-enabled database connections
- **Credential Management**: Environment-based configuration
- **Resource Management**: Automatic connection cleanup with try-with-resources

## 📈 Performance Optimizations

- **Connection Pooling**: HikariCP with optimized settings
- **Database Indexing**: Strategic indexes for common queries
- **Prepared Statement Caching**: Reduced SQL parsing overhead
- **Resource Lifecycle**: Proper cleanup to prevent memory leaks

## 🏗️ Best Practices Implemented

### Code Organization
- **Layered Architecture**: Separation of concerns with DAO, Service, and Model layers
- **Dependency Injection**: Constructor injection for testability
- **Exception Handling**: Custom exceptions with meaningful error messages

### Database Access
- **DAO Pattern**: Centralized data access logic
- **Transaction Management**: Proper transaction boundaries
- **Resource Management**: Automatic resource cleanup
- **Connection Pooling**: Production-ready connection management

### Testing Strategy
- **Unit Tests**: Isolated testing with mocks
- **Integration Tests**: End-to-end database testing
- **Test Coverage**: Comprehensive coverage reporting
- **Test Data Management**: Controlled test data with cleanup

### Development Workflow
- **Environment Setup**: Automated development environment
- **Database Migrations**: Version-controlled schema changes
- **Configuration Management**: Environment-specific settings
- **Logging Strategy**: Structured logging with different levels

## 🔍 Common Operations

### Customer Management
```java
// Create customer
CustomerService customerService = new CustomerService();
Customer customer = new Customer("John Doe", "john@example.com", "1234567890");
customerService.createCustomer(customer);

// Retrieve customer
Optional<Customer> found = customerService.getCustomerById(1);

// Update customer
customer.setPhoneNumber("555-0123");
customerService.updateCustomer(customer);
```

### Database Operations
```java
// Using DAO directly
CustomerDao customerDao = new CustomerDao();
List<Customer> allCustomers = customerDao.findAll();
Optional<Customer> customer = customerDao.findByEmail("john@example.com");
```

## 🚀 Building and Deployment

### Build
```bash
mvn clean package
```

### Run Tests
```bash
mvn verify
```

### Database Migrations
```bash
# Run migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info

# Clean database (development only)
mvn flyway:clean
```

## 📝 Contributing

1. Follow the established code style and patterns
2. Add unit tests for new features
3. Update documentation for significant changes
4. Use meaningful commit messages
5. Ensure all tests pass before submitting

## 🔗 Dependencies

- **MySQL Connector/J**: MySQL JDBC driver
- **HikariCP**: High-performance connection pooling
- **Flyway**: Database migration tool
- **SLF4J + Logback**: Logging framework
- **JUnit 5**: Testing framework
- **Mockito**: Mocking framework for unit tests

## 📄 License

This project is licensed under the MIT License - see the LICENSE file for details.

---

For questions or support, please open an issue in the project repository.