// src/main/java/org/ahmet/database/DatabaseSetup.java
package org.ahmet.database;

import org.ahmet.util.DatabaseUtil;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

public class DatabaseSetup {
    private static final Logger LOGGER = Logger.getLogger(DatabaseSetup.class.getName());

    public static void createDatabase(String dbName) {
        String createDatabaseQuery = "CREATE DATABASE IF NOT EXISTS " + dbName;
        try {
            DatabaseUtil.executeUpdate(createDatabaseQuery);
            LOGGER.info("Database " + dbName + " created successfully.");
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating database", e);
        }
    }

    public static void createTable(String dbName, String tableName, String createTableQuery) {
        String useDatabaseQuery = "USE " + dbName;
        String checkTableExistsQuery = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '" + dbName + "' AND table_name = '" + tableName + "'";
        try (Connection connection = DatabaseUtil.getConnection(dbName);
             Statement statement = connection.createStatement()) {
            statement.executeUpdate(useDatabaseQuery);
            ResultSet rs = statement.executeQuery(checkTableExistsQuery);
            rs.next();
            if (rs.getInt(1) == 0) {
                statement.executeUpdate(createTableQuery);
                LOGGER.info("Table " + tableName + " created successfully in database " + dbName);
            } else {
                LOGGER.info("Table " + tableName + " already exists in database " + dbName);
            }
        } catch (SQLException e) {
            LOGGER.log(Level.SEVERE, "Error creating table", e);
        }
    }

    public static void main(String[] args) {
        String dbName = DatabaseUtil.getDatabaseName();
        createDatabase(dbName);

        String createCustomerTableQuery = "CREATE TABLE Customer (" +
                "customer_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(100) NOT NULL UNIQUE, " +
                "phone_number VARCHAR(15) NOT NULL" +
                ")";
        createTable(dbName, "Customer", createCustomerTableQuery);

        String createProductTableQuery = "CREATE TABLE Product (" +
                "product_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "product_name VARCHAR(100) NOT NULL, " +
                "price DECIMAL(10, 2) NOT NULL" +
                ")";
        createTable(dbName, "Product", createProductTableQuery);

        String createOrderTableQuery = "CREATE TABLE `Order` (" +
                "order_id INT AUTO_INCREMENT PRIMARY KEY, " +
                "order_date DATE NOT NULL, " +
                "customer_id INT NOT NULL, " +
                "FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE CASCADE" +
                ")";
        createTable(dbName, "`Order`", createOrderTableQuery);

        String createOrderProductTableQuery = "CREATE TABLE Order_Product (" +
                "order_id INT NOT NULL, " +
                "product_id INT NOT NULL, " +
                "quantity INT NOT NULL, " +
                "PRIMARY KEY (order_id, product_id), " +
                "FOREIGN KEY (order_id) REFERENCES `Order`(order_id) ON DELETE CASCADE, " +
                "FOREIGN KEY (product_id) REFERENCES Product(product_id) ON DELETE CASCADE" +
                ")";
        createTable(dbName, "Order_Product", createOrderProductTableQuery);
    }
}