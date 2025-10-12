-- Create Product table if it does not exist
CREATE TABLE IF NOT EXISTS Product
(
    product_id
    INT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    product_name
    VARCHAR
(
    255
) NOT NULL,
    price DECIMAL
(
    10,
    2
) NOT NULL
    );

-- Insert data into Product table
INSERT INTO Product (product_name, price)
VALUES ('Orange', 2.50),
       ('Lemon', 1.75),
       ('Apple', 3.00),
       ('Banana', 1.25),
       ('Grapes', 4.50),
       ('Pineapple', 5.00),
       ('Mango', 3.75),
       ('Peach', 2.25),
       ('Strawberry', 6.00),
       ('Blueberry', 7.25);

-- Create Customer table if it does not exist
CREATE TABLE IF NOT EXISTS Customer
(
    customer_id
    INT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    name
    VARCHAR
(
    255
) NOT NULL,
    email VARCHAR
(
    255
) NOT NULL UNIQUE,
    phone_number VARCHAR
(
    15
) NOT NULL
    );

-- Insert data into Customer table
INSERT INTO Customer (name, email, phone_number)
VALUES ('Alice Dupont', 'alice.dupont@gmail.com', '+33612345678'),
       ('Bernard Martin', 'bernard.martin@yahoo.com', '+33623456789'),
       ('Claire Lefevre', 'claire.lefevre@orange.fr', '+33634567890'),
       ('David Laurent', 'david.laurent@protonmail.com', '+33645678901'),
       ('Elise Renault', 'elise.renault@gmail.com', '+33656789012'),
       ('Francois Dubois', 'francois.dubois@gmail.com', '+33667890123'),
       ('Gabrielle Petit', 'gabrielle.petit@gmail.com', '+33678901234'),
       ('Hugo Blanchard', 'hugo.blanchard@outlook.com', '+33689012345'),
       ('Isabelle Fontaine', 'isabelle.fontaine@gmail.com', '+33690123456'),
       ('Jacques Durand', 'jacques.durand@yahoo.com', '+33601234567'),
       ('Karine Girard', 'karine.girard@orange.fr', '+33612345678'),
       ('Luc Moreau', 'luc.moreau@gmail.com', '+33623456789'),
       ('Marie Leroy', 'marie.leroy@protonmail.com', '+33634567890'),
       ('Nicolas Gauthier', 'nicolas.gauthier@gmail.com', '+33645678901'),
       ('Olivier Bernard', 'olivier.bernard@outlook.com', '+33656789012'),
       ('Pauline Robert', 'pauline.robert@gmail.com', '+33667890123'),
       ('Quentin Lambert', 'quentin.lambert@gmail.com', '+33678901234'),
       ('Sophie Michel', 'sophie.michel@orange.fr', '+33689012345'),
       ('Thomas Perrault', 'thomas.perrault@protonmail.com', '+33690123456'),
       ('Valerie Simon', 'valerie.simon@gmail.com', '+33601234567');

-- Create Order table if it does not exist
CREATE TABLE IF NOT EXISTS `Order`
(
    order_id
    INT
    PRIMARY
    KEY
    AUTO_INCREMENT,
    order_date
    DATE
    NOT
    NULL,
    customer_id
    INT
    NOT
    NULL,
    FOREIGN
    KEY
(
    customer_id
) REFERENCES Customer
(
    customer_id
) ON DELETE CASCADE
    );

-- Insert data into Order table (one order per customer)
INSERT INTO `Order` (order_date, customer_id)
VALUES ('2024-08-01', 1),
       ('2024-08-02', 2),
       ('2024-08-03', 3),
       ('2024-08-04', 4),
       ('2024-08-05', 5),
       ('2024-08-06', 6),
       ('2024-08-07', 7),
       ('2024-08-08', 8),
       ('2024-08-09', 9),
       ('2024-08-10', 10),
       ('2024-08-11', 11),
       ('2024-08-12', 12),
       ('2024-08-13', 13),
       ('2024-08-14', 14),
       ('2024-08-15', 15),
       ('2024-08-16', 16),
       ('2024-08-17', 17),
       ('2024-08-18', 18),
       ('2024-08-19', 19),
       ('2024-08-20', 20);

-- Create Order_Product table if it does not exist
CREATE TABLE IF NOT EXISTS Order_Product
(
    order_id
    INT
    NOT
    NULL,
    product_id
    INT
    NOT
    NULL,
    quantity
    INT
    NOT
    NULL,
    PRIMARY
    KEY
(
    order_id,
    product_id
),
    FOREIGN KEY
(
    order_id
) REFERENCES `Order`
(
    order_id
) ON DELETE CASCADE,
    FOREIGN KEY
(
    product_id
) REFERENCES Product
(
    product_id
)
  ON DELETE CASCADE
    );

-- Insert data into Order_Product table (mapping products to orders)
INSERT INTO Order_Product (order_id, product_id, quantity, unit_price)
VALUES (1, 1, 2, 2.50),    -- Orange
       (1, 3, 1, 3.00),    -- Apple
       (2, 2, 3, 1.75),    -- Lemon
       (2, 5, 1, 4.50),    -- Grapes
       (3, 4, 2, 1.25),    -- Banana
       (3, 6, 1, 5.00),    -- Pineapple
       (4, 8, 1, 2.25),    -- Peach
       (4, 7, 1, 3.75),    -- Mango
       (5, 9, 1, 6.00),    -- Strawberry
       (5, 10, 2, 7.25),   -- Blueberry
       (6, 1, 1, 2.50),    -- Orange
       (6, 4, 1, 1.25),    -- Banana
       (7, 3, 2, 3.00),    -- Apple
       (7, 5, 1, 4.50),    -- Grapes
       (8, 6, 2, 5.00),    -- Pineapple
       (8, 9, 1, 6.00),    -- Strawberry
       (9, 2, 1, 1.75),    -- Lemon
       (9, 8, 1, 2.25),    -- Peach
       (10, 3, 3, 3.00),   -- Apple
       (10, 4, 2, 1.25),   -- Banana
       (11, 5, 1, 4.50),   -- Grapes
       (11, 10, 1, 7.25),  -- Blueberry
       (12, 7, 1, 3.75),   -- Mango
       (12, 8, 1, 2.25),   -- Peach
       (13, 1, 2, 2.50),   -- Orange
       (13, 3, 2, 3.00),   -- Apple
       (14, 6, 2, 5.00),   -- Pineapple
       (14, 10, 1, 7.25),  -- Blueberry
       (15, 9, 1, 6.00),   -- Strawberry
       (15, 2, 1, 1.75),   -- Lemon
       (16, 1, 3, 2.50),   -- Orange
       (16, 4, 2, 1.25),   -- Banana
       (17, 5, 1, 4.50),   -- Grapes
       (17, 7, 2, 3.75),   -- Mango
       (18, 8, 1, 2.25),   -- Peach
       (18, 9, 1, 6.00),   -- Strawberry
       (19, 10, 2, 7.25),  -- Blueberry
       (19, 1, 1, 2.50),   -- Orange
       (20, 3, 1, 3.00),   -- Apple
       (20, 6, 1, 5.00);   -- Pineapple