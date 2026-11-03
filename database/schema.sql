-- MySQL 8.x. Run once on a NEW empty laptop_store database.
-- Does not DROP tables or erase existing data.
CREATE DATABASE IF NOT EXISTS laptop_store CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE laptop_store;
CREATE TABLE accounts (
 id BIGINT AUTO_INCREMENT PRIMARY KEY, email VARCHAR(160) NOT NULL UNIQUE,
 password_hash VARCHAR(255) NOT NULL, full_name VARCHAR(100) NOT NULL,
 phone VARCHAR(255), address VARCHAR(300), role VARCHAR(255) NOT NULL
) ENGINE=InnoDB;
CREATE TABLE categories(id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(100) NOT NULL UNIQUE) ENGINE=InnoDB;
CREATE TABLE products (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,name VARCHAR(160) NOT NULL,brand VARCHAR(60) NOT NULL,
 category_id BIGINT NOT NULL,purpose VARCHAR(30) NOT NULL,cpu VARCHAR(255),ram VARCHAR(255),storage VARCHAR(255),gpu VARCHAR(255),screen VARCHAR(255),description VARCHAR(3000),price BIGINT NOT NULL,stock INT NOT NULL,image_url VARCHAR(255),active BOOLEAN NOT NULL,version BIGINT,
 CONSTRAINT fk_product_category FOREIGN KEY(category_id) REFERENCES categories(id),
 CONSTRAINT chk_product_price CHECK(price>=0),CONSTRAINT chk_product_stock CHECK(stock>=0),
 INDEX idx_product_filter(active,brand,purpose,price)
) ENGINE=InnoDB;
CREATE TABLE cart_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT NOT NULL,product_id BIGINT NOT NULL,quantity INT NOT NULL,
 UNIQUE KEY uq_cart(account_id,product_id),
 FOREIGN KEY(account_id) REFERENCES accounts(id),FOREIGN KEY(product_id) REFERENCES products(id),CHECK(quantity BETWEEN 1 AND 20)
) ENGINE=InnoDB;
CREATE TABLE shop_orders (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT NOT NULL,checkout_token VARCHAR(64) NOT NULL UNIQUE,
 status VARCHAR(40) NOT NULL,payment_method VARCHAR(30) NOT NULL,payment_status VARCHAR(40) NOT NULL,
 subtotal BIGINT NOT NULL,discount BIGINT NOT NULL,total BIGINT NOT NULL,coupon VARCHAR(255),
 full_name VARCHAR(100) NOT NULL,phone VARCHAR(20) NOT NULL,address VARCHAR(300) NOT NULL,
 created_at DATETIME(6) NOT NULL,paid_at DATETIME(6),delivered_at DATETIME(6),expires_at DATETIME(6),
 payment_ref VARCHAR(64) UNIQUE,payment_url VARCHAR(2000),provider_transaction VARCHAR(255),version BIGINT,
 FOREIGN KEY(account_id) REFERENCES accounts(id),CHECK(total>=0),
 INDEX idx_order_customer(account_id,created_at),INDEX idx_order_report(status,payment_status,delivered_at)
) ENGINE=InnoDB;
CREATE TABLE order_items (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,order_id BIGINT NOT NULL,product_id BIGINT NOT NULL,
 product_name VARCHAR(255) NOT NULL,unit_price BIGINT NOT NULL,quantity INT NOT NULL,
 FOREIGN KEY(order_id) REFERENCES shop_orders(id),FOREIGN KEY(product_id) REFERENCES products(id),CHECK(quantity>0)
) ENGINE=InnoDB;
CREATE TABLE reviews (
 id BIGINT AUTO_INCREMENT PRIMARY KEY,account_id BIGINT NOT NULL,product_id BIGINT NOT NULL,
 rating INT NOT NULL,content VARCHAR(1500) NOT NULL,author VARCHAR(255) NOT NULL,created_at DATETIME(6),
 UNIQUE KEY uq_review(account_id,product_id),FOREIGN KEY(account_id) REFERENCES accounts(id),FOREIGN KEY(product_id) REFERENCES products(id),CHECK(rating BETWEEN 1 AND 5)
) ENGINE=InnoDB;
-- SeedData inserts sample products and hashes the configured admin password on first startup.