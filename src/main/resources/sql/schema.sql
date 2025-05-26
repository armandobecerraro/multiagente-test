-- Schema for SMAGESCI Database
-- PostgreSQL Database Schema

-- Products table
CREATE TABLE IF NOT EXISTS products (
    product_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),
    base_price DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Customers table
CREATE TABLE IF NOT EXISTS customers (
    customer_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    address TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders table
CREATE TABLE IF NOT EXISTS orders (
    order_id VARCHAR(50) PRIMARY KEY,
    customer_id VARCHAR(50) NOT NULL,
    order_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    total_amount DECIMAL(10,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id)
);

-- Order Items table
CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    line_total DECIMAL(10,2) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Inventory table
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id SERIAL PRIMARY KEY,
    product_id VARCHAR(50) NOT NULL,
    location VARCHAR(100) NOT NULL DEFAULT 'MAIN_WAREHOUSE',
    quantity INTEGER NOT NULL DEFAULT 0,
    reserved_quantity INTEGER NOT NULL DEFAULT 0,
    min_stock_level INTEGER NOT NULL DEFAULT 0,
    max_stock_level INTEGER NOT NULL DEFAULT 1000,
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (product_id) REFERENCES products(product_id),
    UNIQUE(product_id, location)
);

-- Raw Materials table
CREATE TABLE IF NOT EXISTS raw_materials (
    material_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    supplier_id VARCHAR(50),
    unit_of_measure VARCHAR(20) NOT NULL,
    cost_per_unit DECIMAL(10,2) NOT NULL,
    current_stock INTEGER NOT NULL DEFAULT 0,
    min_stock_level INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Production Lines table
CREATE TABLE IF NOT EXISTS production_lines (
    line_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    capacity_per_hour INTEGER NOT NULL,
    efficiency_rate DECIMAL(5,2) NOT NULL DEFAULT 100.00,
    status VARCHAR(50) NOT NULL DEFAULT 'IDLE',
    current_product_id VARCHAR(50),
    setup_time_minutes INTEGER NOT NULL DEFAULT 30,
    last_maintenance TIMESTAMP,
    next_maintenance TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (current_product_id) REFERENCES products(product_id)
);

-- Production Jobs table
CREATE TABLE IF NOT EXISTS production_jobs (
    job_id VARCHAR(50) PRIMARY KEY,
    line_id VARCHAR(50) NOT NULL,
    product_id VARCHAR(50) NOT NULL,
    planned_quantity INTEGER NOT NULL,
    produced_quantity INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL DEFAULT 'SCHEDULED',
    priority INTEGER NOT NULL DEFAULT 5,
    scheduled_start TIMESTAMP,
    actual_start TIMESTAMP,
    scheduled_end TIMESTAMP,
    actual_end TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (line_id) REFERENCES production_lines(line_id),
    FOREIGN KEY (product_id) REFERENCES products(product_id)
);

-- Suppliers table
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address TEXT,
    reliability_score DECIMAL(3,2) DEFAULT 5.00,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Transportation table
CREATE TABLE IF NOT EXISTS shipments (
    shipment_id VARCHAR(50) PRIMARY KEY,
    order_id VARCHAR(50) NOT NULL,
    transporter_id VARCHAR(50),
    status VARCHAR(50) NOT NULL DEFAULT 'PREPARING',
    origin_address TEXT,
    destination_address TEXT,
    scheduled_pickup TIMESTAMP,
    actual_pickup TIMESTAMP,
    scheduled_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    tracking_number VARCHAR(100),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (order_id) REFERENCES orders(order_id)
);

-- System Events table (for auditing and monitoring)
CREATE TABLE IF NOT EXISTS system_events (
    event_id SERIAL PRIMARY KEY,
    event_type VARCHAR(100) NOT NULL,
    agent_name VARCHAR(100),
    description TEXT,
    data JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Insert some sample data
INSERT INTO products (product_id, name, description, category, base_price) VALUES
('PROD001', 'Product Alpha', 'High-quality product for industrial use', 'Industrial', 299.99),
('PROD002', 'Product Beta', 'Consumer electronics device', 'Electronics', 199.99),
('PROD003', 'Product Gamma', 'Automotive component', 'Automotive', 89.99)
ON CONFLICT (product_id) DO NOTHING;

INSERT INTO customers (customer_id, name, contact_email, address) VALUES
('CUST001', 'Acme Corporation', 'orders@acme.com', '123 Business St, City, State 12345'),
('CUST002', 'Global Industries', 'purchasing@global.com', '456 Industrial Ave, City, State 67890'),
('CUST003', 'TechStart LLC', 'buying@techstart.com', '789 Innovation Blvd, City, State 54321')
ON CONFLICT (customer_id) DO NOTHING;

INSERT INTO inventory (product_id, location, quantity, min_stock_level, max_stock_level) VALUES
('PROD001', 'MAIN_WAREHOUSE', 100, 20, 500),
('PROD002', 'MAIN_WAREHOUSE', 50, 10, 200),
('PROD003', 'MAIN_WAREHOUSE', 200, 50, 1000)
ON CONFLICT (product_id, location) DO NOTHING;

INSERT INTO production_lines (line_id, name, capacity_per_hour, efficiency_rate, status) VALUES
('LINE001', 'Production Line Alpha', 50, 95.5, 'IDLE'),
('LINE002', 'Production Line Beta', 70, 98.2, 'IDLE'),
('LINE003', 'Production Line Gamma', 40, 92.8, 'MAINTENANCE')
ON CONFLICT (line_id) DO NOTHING;
