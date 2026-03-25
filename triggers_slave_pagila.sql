-- tabla log customer_log
create table if not exists customer_log (
    log_id       INT NOT NULL AUTO_INCREMENT,
    operation    ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    operated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    customer_id  INT,
    store_id     SMALLINT,
    first_name   VARCHAR(50),
    last_name    VARCHAR(50),
    email        VARCHAR(50),
    address_id   SMALLINT,
    activebool   TINYINT(1),
    create_date  DATE,
    last_update  TIMESTAMP NULL,
    active       INT,
    synced       TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (log_id)
);

-- trigger INSERT en customer
CREATE TRIGGER trg_customer_insert
AFTER INSERT ON customer
FOR EACH ROW
BEGIN
    INSERT INTO customer_log (
        operation, customer_id, store_id, first_name, last_name,
        email, address_id, activebool, create_date, last_update, active
    ) VALUES (
        'INSERT', NEW.customer_id, NEW.store_id, NEW.first_name, NEW.last_name,
        NEW.email, NEW.address_id, NEW.activebool, NEW.create_date, NEW.last_update, NEW.active
    );
END;

-- trigger UPDATE en customer
CREATE TRIGGER trg_customer_update
AFTER UPDATE ON customer
FOR EACH ROW
BEGIN
    INSERT INTO customer_log (
        operation, customer_id, store_id, first_name, last_name,
        email, address_id, activebool, create_date, last_update, active
    ) VALUES (
        'UPDATE', NEW.customer_id, NEW.store_id, NEW.first_name, NEW.last_name,
        NEW.email, NEW.address_id, NEW.activebool, NEW.create_date, NEW.last_update, NEW.active
    );
end;

-- trigger DELETE en customer
CREATE TRIGGER trg_customer_delete
AFTER DELETE ON customer
FOR EACH ROW
BEGIN
    INSERT INTO customer_log (
        operation, customer_id, store_id, first_name, last_name,
        email, address_id, activebool, create_date, last_update, active
    ) VALUES (
        'DELETE', OLD.customer_id, OLD.store_id, OLD.first_name, OLD.last_name,
        OLD.email, OLD.address_id, OLD.activebool, OLD.create_date, OLD.last_update, OLD.active
    );
end;

-- tabla log rental_log
CREATE TABLE rental_log (
    log_id       INT NOT NULL AUTO_INCREMENT,
    operation    ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    operated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    rental_id    INT,
    rental_date  DATETIME,
    inventory_id INT,
    customer_id  INT,
    return_date  DATETIME,
    staff_id     SMALLINT,
    last_update  TIMESTAMP NULL,
    synced       TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (log_id)
);

-- trigger INSERT en rental
CREATE TRIGGER trg_rental_insert
AFTER INSERT ON rental
FOR EACH ROW
BEGIN
    INSERT INTO rental_log (
        operation, rental_id, rental_date, inventory_id,
        customer_id, return_date, staff_id, last_update
    ) VALUES (
        'INSERT', NEW.rental_id, NEW.rental_date, NEW.inventory_id,
        NEW.customer_id, NEW.return_date, NEW.staff_id, NEW.last_update
    );
end;

-- trigger UPDATE en rental
CREATE TRIGGER trg_rental_update
AFTER UPDATE ON rental
FOR EACH ROW
BEGIN
    INSERT INTO rental_log (
        operation, rental_id, rental_date, inventory_id,
        customer_id, return_date, staff_id, last_update
    ) VALUES (
        'UPDATE', NEW.rental_id, NEW.rental_date, NEW.inventory_id,
        NEW.customer_id, NEW.return_date, NEW.staff_id, NEW.last_update
    );
end;

-- trigger DELETE en rental
CREATE TRIGGER trg_rental_delete
AFTER DELETE ON rental
FOR EACH ROW
BEGIN
    INSERT INTO rental_log (
        operation, rental_id, rental_date, inventory_id,
        customer_id, return_date, staff_id, last_update
    ) VALUES (
        'DELETE', OLD.rental_id, OLD.rental_date, OLD.inventory_id,
        OLD.customer_id, OLD.return_date, OLD.staff_id, OLD.last_update
    );
end;

-- tabla log payment_log
CREATE TABLE payment_log (
    log_id       INT NOT NULL AUTO_INCREMENT,
    operation    ENUM('INSERT','UPDATE','DELETE') NOT NULL,
    operated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payment_id   INT,
    customer_id  INT,
    staff_id     SMALLINT,
    rental_id    INT,
    amount       DECIMAL(5,2),
    payment_date DATETIME,
    last_update  TIMESTAMP NULL,
    synced       TINYINT(1) NOT NULL DEFAULT 0,
    PRIMARY KEY (log_id)
);

-- trigger INSERT en payment
CREATE TRIGGER trg_payment_insert
AFTER INSERT ON payment
FOR EACH ROW
BEGIN
    INSERT INTO payment_log (
        operation, payment_id, customer_id, staff_id,
        rental_id, amount, payment_date, last_update
    ) VALUES (
        'INSERT', NEW.payment_id, NEW.customer_id, NEW.staff_id,
        NEW.rental_id, NEW.amount, NEW.payment_date, NEW.last_update
    );
end;

-- trigger UPDATE en payment
CREATE TRIGGER trg_payment_update
AFTER UPDATE ON payment
FOR EACH ROW
BEGIN
    INSERT INTO payment_log (
        operation, payment_id, customer_id, staff_id,
        rental_id, amount, payment_date, last_update
    ) VALUES (
        'UPDATE', NEW.payment_id, NEW.customer_id, NEW.staff_id,
        NEW.rental_id, NEW.amount, NEW.payment_date, NEW.last_update
    );
end;

-- trigger DELETE  en payment
CREATE TRIGGER trg_payment_delete
AFTER DELETE ON payment
FOR EACH ROW
BEGIN
    INSERT INTO payment_log (
        operation, payment_id, customer_id, staff_id,
        rental_id, amount, payment_date, last_update
    ) VALUES (
        'DELETE', OLD.payment_id, OLD.customer_id, OLD.staff_id,
        OLD.rental_id, OLD.amount, OLD.payment_date, OLD.last_update
    );
end;
