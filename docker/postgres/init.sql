CREATE TABLE users (
    id SERIAL PRIMARY KEY,
    username VARCHAR(100) NOT NULL,
    login_name VARCHAR(100) NOT NULL UNIQUE,
    email_address VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    CONSTRAINT chk_users_username_not_blank CHECK (btrim(username) <> ''),
    CONSTRAINT chk_users_login_name_not_blank CHECK (btrim(login_name) <> ''),
    CONSTRAINT chk_users_email_address_not_blank CHECK (btrim(email_address) <> ''),
    CONSTRAINT chk_users_password_hash_not_blank CHECK (btrim(password_hash) <> '')
);

CREATE UNIQUE INDEX idx_users_login_name ON users (login_name);
CREATE UNIQUE INDEX idx_users_email_address ON users (email_address);
