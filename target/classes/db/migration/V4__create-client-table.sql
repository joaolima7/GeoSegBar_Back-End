CREATE TABLE client(
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    acronym CHAR(3) NOT NULL,
    logo VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    name_contact VARCHAR(255) NOT NULL,
    phone_contact VARCHAR(255) NOT NULL,
    address_id INT NOT NULL,
    dam_id INT NOT NULL,
    FOREIGN KEY (address_id) REFERENCES address(id) ON DELETE CASCADE,
    FOREIGN KEY (dam_id) REFERENCES dam(id) ON DELETE CASCADE
);