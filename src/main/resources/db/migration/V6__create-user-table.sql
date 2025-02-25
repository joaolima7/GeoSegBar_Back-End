CREATE TABLE users(
    id SERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    phone VARCHAR(255) NOT NULL,
    sex_id INT NOT NULL,
    group_id INT NOT NULL, 
    FOREIGN KEY (sex_id) REFERENCES sex(id) ON DELETE CASCADE,
    FOREIGN KEY (group_id) REFERENCES group_profile(id) ON DELETE CASCADE
);