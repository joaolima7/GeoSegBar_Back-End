CREATE TABLE users(
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    sex_id INT NOT NULL,
    group_id INT NOT NULL, 
);