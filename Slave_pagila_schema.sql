SET FOREIGN_KEY_CHECKS = 0;

-- tablas in

CREATE TABLE actor (
    actor_id    SMALLINT NOT NULL AUTO_INCREMENT,
    first_name  VARCHAR(50) NOT NULL,
    last_name   VARCHAR(50) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id)
);

CREATE TABLE category (
    category_id SMALLINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(25) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (category_id)
);

CREATE TABLE language (
    language_id SMALLINT NOT NULL AUTO_INCREMENT,
    name        VARCHAR(20) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (language_id)
);

CREATE TABLE country (
    country_id  SMALLINT NOT NULL AUTO_INCREMENT,
    country     VARCHAR(50) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (country_id)
);

CREATE TABLE city (
    city_id     SMALLINT NOT NULL AUTO_INCREMENT,
    city        VARCHAR(50) NOT NULL,
    country_id  SMALLINT NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (city_id),
    FOREIGN KEY (country_id) REFERENCES country(country_id)
);

CREATE TABLE address (
    address_id  SMALLINT NOT NULL AUTO_INCREMENT,
    address     VARCHAR(50) NOT NULL,
    address2    VARCHAR(50) DEFAULT NULL,
    district    VARCHAR(20) NOT NULL,
    city_id     SMALLINT NOT NULL,
    postal_code VARCHAR(10) DEFAULT NULL,
    phone       VARCHAR(20) NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (address_id),
    FOREIGN KEY (city_id) REFERENCES city(city_id)
);

CREATE TABLE store (
    store_id         SMALLINT NOT NULL AUTO_INCREMENT,
    manager_staff_id SMALLINT NOT NULL,
    address_id       SMALLINT NOT NULL,
    last_update      TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (store_id),
    FOREIGN KEY (address_id) REFERENCES address(address_id)
);

CREATE TABLE staff (
    staff_id    SMALLINT NOT NULL AUTO_INCREMENT,
    first_name  VARCHAR(50) NOT NULL,
    last_name   VARCHAR(50) NOT NULL,
    address_id  SMALLINT NOT NULL,
    email       VARCHAR(50) DEFAULT NULL,
    store_id    SMALLINT NOT NULL,
    active      TINYINT(1) NOT NULL DEFAULT 1,
    username    VARCHAR(16) NOT NULL,
    password    VARCHAR(40) DEFAULT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    picture     MEDIUMBLOB DEFAULT NULL,
    PRIMARY KEY (staff_id),
    FOREIGN KEY (address_id) REFERENCES address(address_id),
    FOREIGN KEY (store_id)   REFERENCES store(store_id) ON UPDATE CASCADE
);

ALTER TABLE store
    ADD CONSTRAINT fk_store_manager
    FOREIGN KEY (manager_staff_id) REFERENCES staff(staff_id) ON UPDATE CASCADE;

CREATE TABLE film (
    film_id              SMALLINT NOT NULL AUTO_INCREMENT,
    title                VARCHAR(255) NOT NULL,
    description          TEXT DEFAULT NULL,
    release_year         YEAR DEFAULT NULL,
    language_id          SMALLINT NOT NULL,
    original_language_id SMALLINT DEFAULT NULL,
    rental_duration      TINYINT NOT NULL DEFAULT 3,
    rental_rate          DECIMAL(4,2) NOT NULL DEFAULT 4.99,
    length               SMALLINT DEFAULT NULL,
    replacement_cost     DECIMAL(5,2) NOT NULL DEFAULT 19.99,
    rating               ENUM('G','PG','PG-13','R','NC-17') DEFAULT 'G',
    last_update          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    special_features     SET('Trailers','Commentaries','Deleted Scenes','Behind the Scenes') DEFAULT NULL,
    PRIMARY KEY (film_id),
    FOREIGN KEY (language_id)          REFERENCES language(language_id),
    FOREIGN KEY (original_language_id) REFERENCES language(language_id)
);

CREATE TABLE film_actor (
    actor_id    SMALLINT NOT NULL,
    film_id     SMALLINT NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (actor_id, film_id),
    FOREIGN KEY (actor_id) REFERENCES actor(actor_id),
    FOREIGN KEY (film_id)  REFERENCES film(film_id)
);

CREATE TABLE film_category (
    category_id SMALLINT NOT NULL,
    film_id     SMALLINT NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (film_id, category_id),
    FOREIGN KEY (film_id)     REFERENCES film(film_id),
    FOREIGN KEY (category_id) REFERENCES category(category_id)
);

CREATE TABLE inventory (
    inventory_id INT NOT NULL AUTO_INCREMENT,
    film_id      SMALLINT NOT NULL,
    store_id     SMALLINT NOT NULL,
    last_update  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (inventory_id),
    FOREIGN KEY (film_id)  REFERENCES film(film_id),
    FOREIGN KEY (store_id) REFERENCES store(store_id)
);

-- tablas OUT

CREATE TABLE customer (
    customer_id INT NOT NULL AUTO_INCREMENT,
    store_id    SMALLINT NOT NULL,
    first_name  VARCHAR(50) NOT NULL,
    last_name   VARCHAR(50) NOT NULL,
    email       VARCHAR(50) NOT NULL,
    address_id  SMALLINT NOT NULL,
    activebool  TINYINT(1) NOT NULL DEFAULT 1,
    create_date DATE NOT NULL,
    last_update TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    active      INT DEFAULT NULL,
    PRIMARY KEY (customer_id),
    FOREIGN KEY (store_id)  REFERENCES store(store_id),
    FOREIGN KEY (address_id) REFERENCES address(address_id)
);

CREATE TABLE rental (
    rental_id    INT NOT NULL AUTO_INCREMENT,
    rental_date  DATETIME NOT NULL,
    inventory_id INT NOT NULL,
    customer_id  INT NOT NULL,
    return_date  DATETIME DEFAULT NULL,
    staff_id     SMALLINT NOT NULL,
    last_update  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (rental_id),
    FOREIGN KEY (inventory_id) REFERENCES inventory(inventory_id),
    FOREIGN KEY (customer_id)  REFERENCES customer(customer_id),
    FOREIGN KEY (staff_id)     REFERENCES staff(staff_id)
);

CREATE TABLE payment (
    payment_id   INT NOT NULL AUTO_INCREMENT,
    customer_id  INT NOT NULL,
    staff_id     SMALLINT NOT NULL,
    rental_id    INT DEFAULT NULL,
    amount       DECIMAL(5,2) NOT NULL,
    payment_date DATETIME NOT NULL,
    last_update  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (payment_id),
    FOREIGN KEY (customer_id) REFERENCES customer(customer_id),
    FOREIGN KEY (staff_id)    REFERENCES staff(staff_id),
    INDEX idx_rental_id (rental_id),
    FOREIGN KEY (rental_id) REFERENCES rental(rental_id) ON DELETE SET NULL
);

SET FOREIGN_KEY_CHECKS = 1;