-- Migration: Create users table
-- Description: Creates the users table with proper constraints, indexes, and PostgreSQL-specific features
-- Author: Marcel Pulido
-- Date: 2025

-- Create users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'MEMBER',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraints
    CONSTRAINT users_email_check CHECK (email ~* '^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$'),
    CONSTRAINT users_role_check CHECK (role IN ('MEMBER', 'ADMIN')),
    CONSTRAINT users_first_name_check CHECK (LENGTH(first_name) > 0),
    CONSTRAINT users_last_name_check CHECK (LENGTH(last_name) > 0)
);

-- Create indexes for performance
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_name ON users(last_name, first_name);
CREATE INDEX idx_users_created_at ON users(created_at);

-- Create updated_at trigger (PostgreSQL-specific)
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_users_updated_at 
    BEFORE UPDATE ON users 
    FOR EACH ROW 
    EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE users IS 'User accounts for library management system';
COMMENT ON COLUMN users.id IS 'Primary key - auto-generated user ID';
COMMENT ON COLUMN users.email IS 'Unique email address for authentication';
COMMENT ON COLUMN users.password IS 'Hashed password (never store plaintext)';
COMMENT ON COLUMN users.first_name IS 'User first name (required)';
COMMENT ON COLUMN users.last_name IS 'User last name (required)';
COMMENT ON COLUMN users.phone IS 'Optional phone number';
COMMENT ON COLUMN users.role IS 'User role: MEMBER or ADMIN';
COMMENT ON COLUMN users.created_at IS 'Timestamp when user was created';
COMMENT ON COLUMN users.updated_at IS 'Timestamp when user was last updated';