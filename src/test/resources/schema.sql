DROP TABLE IF EXISTS Order_Product;
DROP TABLE IF EXISTS `Order`;
DROP TABLE IF EXISTS Product;
DROP TABLE IF EXISTS Customer;

CREATE TABLE Customer (
                          customer_id INT AUTO_INCREMENT PRIMARY KEY,
                          name VARCHAR(100) NOT NULL,
                          email VARCHAR(100) NOT NULL UNIQUE,
                          phone_number VARCHAR(15) NOT NULL
);

CREATE TABLE Product (
                         product_id INT AUTO_INCREMENT PRIMARY KEY,
                         product_name VARCHAR(100) NOT NULL,
                         price DECIMAL(10, 2) NOT NULL CHECK (price >= 0)
);

CREATE TABLE `Order` (
                         order_id INT AUTO_INCREMENT PRIMARY KEY,
                         order_date DATE NOT NULL,
                         customer_id INT NOT NULL,
                         FOREIGN KEY (customer_id) REFERENCES Customer(customer_id) ON DELETE CASCADE
);

CREATE TABLE Order_Product (
                               order_id INT NOT NULL,
                               product_id INT NOT NULL,
                               quantity INT NOT NULL,
                               PRIMARY KEY (order_id, product_id),
                               FOREIGN KEY (order_id) REFERENCES `Order`(order_id) ON DELETE CASCADE,
                               FOREIGN KEY (product_id) REFERENCES Product(product_id) ON DELETE CASCADE
);