-- SMAGESCI Database Schema
-- PostgreSQL Version

-- Create database (run separately as superuser)
-- CREATE DATABASE smagesci;
-- CREATE USER smagesci_user WITH PASSWORD 'smagesci_password';
-- GRANT ALL PRIVILEGES ON DATABASE smagesci TO smagesci_user;

-- Raw Materials Table
CREATE TABLE IF NOT EXISTS raw_materials (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    type VARCHAR(50) NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    stock_level INTEGER DEFAULT 0,
    min_stock_level INTEGER DEFAULT 0,
    supplier_id VARCHAR(50),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Products Table
CREATE TABLE IF NOT EXISTS products (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    category VARCHAR(50),
    price DECIMAL(10,2) NOT NULL,
    stock_level INTEGER DEFAULT 0,
    min_stock_level INTEGER DEFAULT 0,
    production_time INTEGER DEFAULT 0, -- in minutes
    quality_status VARCHAR(20) DEFAULT 'PENDING',
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Inventory Table
CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    item_id INTEGER NOT NULL,
    item_name VARCHAR(100) NOT NULL,
    type VARCHAR(20) NOT NULL, -- RAW_MATERIAL, FINISHED_PRODUCT, WORK_IN_PROGRESS
    current_stock INTEGER DEFAULT 0,
    min_stock_level INTEGER DEFAULT 0,
    max_stock_level INTEGER DEFAULT 1000,
    reserved_stock INTEGER DEFAULT 0,
    location VARCHAR(100),
    warehouse_id VARCHAR(50),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_stock_check TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Customers Table
CREATE TABLE IF NOT EXISTS customers (
    id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    address TEXT,
    city VARCHAR(50),
    country VARCHAR(50),
    customer_type VARCHAR(20) DEFAULT 'RETAIL',
    credit_limit DECIMAL(12,2) DEFAULT 0,
    current_credit DECIMAL(12,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Orders Table
CREATE TABLE IF NOT EXISTS orders (
    id SERIAL PRIMARY KEY,
    customer_id VARCHAR(50) REFERENCES customers(id),
    status VARCHAR(20) DEFAULT 'RECEIVED',
    priority VARCHAR(10) DEFAULT 'NORMAL',
    total_amount DECIMAL(12,2) DEFAULT 0,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    requested_delivery_date TIMESTAMP,
    actual_delivery_date TIMESTAMP,
    shipping_address TEXT,
    transporter_id VARCHAR(50)
);

-- Order Items Table
CREATE TABLE IF NOT EXISTS order_items (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id) ON DELETE CASCADE,
    product_id INTEGER REFERENCES products(id),
    product_name VARCHAR(100),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL
);

-- Production Orders Table
CREATE TABLE IF NOT EXISTS production_orders (
    id SERIAL PRIMARY KEY,
    product_id INTEGER REFERENCES products(id),
    quantity INTEGER NOT NULL,
    status VARCHAR(20) DEFAULT 'PLANNED',
    priority VARCHAR(10) DEFAULT 'NORMAL',
    production_line_id VARCHAR(50),
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Purchase Orders Table
CREATE TABLE IF NOT EXISTS purchase_orders (
    id SERIAL PRIMARY KEY,
    supplier_id VARCHAR(50) NOT NULL,
    material_id INTEGER REFERENCES raw_materials(id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(12,2) NOT NULL,
    status VARCHAR(20) DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    delivery_date TIMESTAMP,
    received_date TIMESTAMP
);

-- Shipments Table
CREATE TABLE IF NOT EXISTS shipments (
    id SERIAL PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    transporter_id VARCHAR(50) NOT NULL,
    status VARCHAR(20) DEFAULT 'PREPARING',
    pickup_address TEXT,
    delivery_address TEXT,
    pickup_time TIMESTAMP,
    delivery_time TIMESTAMP,
    estimated_delivery TIMESTAMP,
    tracking_number VARCHAR(100),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- System Metrics Table
CREATE TABLE IF NOT EXISTS system_metrics (
    id SERIAL PRIMARY KEY,
    metric_name VARCHAR(50) NOT NULL,
    metric_value DECIMAL(15,4),
    metric_unit VARCHAR(20),
    agent_name VARCHAR(100),
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Agent Performance Table
CREATE TABLE IF NOT EXISTS agent_performance (
    id SERIAL PRIMARY KEY,
    agent_name VARCHAR(100) NOT NULL,
    response_time DECIMAL(10,2),
    throughput DECIMAL(10,4),
    error_rate DECIMAL(5,4),
    tasks_completed INTEGER DEFAULT 0,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Risk Events Table
CREATE TABLE IF NOT EXISTS risk_events (
    id SERIAL PRIMARY KEY,
    risk_type VARCHAR(50) NOT NULL,
    description TEXT,
    severity DECIMAL(3,2) NOT NULL, -- 0.00 to 1.00
    status VARCHAR(20) DEFAULT 'ACTIVE',
    detection_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolution_time TIMESTAMP,
    mitigation_strategy TEXT
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_inventory_item_type ON inventory(item_id, type);
CREATE INDEX IF NOT EXISTS idx_orders_customer ON orders(customer_id);
CREATE INDEX IF NOT EXISTS idx_orders_status ON orders(status);
CREATE INDEX IF NOT EXISTS idx_order_items_order ON order_items(order_id);
CREATE INDEX IF NOT EXISTS idx_production_orders_status ON production_orders(status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier ON purchase_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_shipments_order ON shipments(order_id);
CREATE INDEX IF NOT EXISTS idx_system_metrics_timestamp ON system_metrics(timestamp);
CREATE INDEX IF NOT EXISTS idx_agent_performance_agent ON agent_performance(agent_name);
CREATE INDEX IF NOT EXISTS idx_risk_events_type ON risk_events(risk_type);

-- Views for commonly used queries
CREATE OR REPLACE VIEW low_stock_items AS
SELECT i.*, 
       CASE 
           WHEN i.current_stock <= i.min_stock_level THEN 'CRITICAL'
           WHEN i.current_stock <= i.min_stock_level * 1.2 THEN 'LOW'
           ELSE 'NORMAL'
       END as stock_status
FROM inventory i
WHERE i.current_stock <= i.min_stock_level * 1.2;

CREATE OR REPLACE VIEW order_summary AS
SELECT o.id, o.customer_id, c.name as customer_name, o.status, o.total_amount,
       o.order_date, o.requested_delivery_date,
       COUNT(oi.id) as item_count
FROM orders o
JOIN customers c ON o.customer_id = c.id
LEFT JOIN order_items oi ON o.id = oi.order_id
GROUP BY o.id, o.customer_id, c.name, o.status, o.total_amount, 
         o.order_date, o.requested_delivery_date;

CREATE OR REPLACE VIEW production_status AS
SELECT po.id, po.product_id, p.name as product_name, po.quantity,
       po.status, po.production_line_id, po.start_time, po.end_time,
       CASE 
           WHEN po.end_time IS NOT NULL THEN 100
           WHEN po.start_time IS NOT NULL THEN 50
           ELSE 0
       END as completion_percentage
FROM production_orders po
JOIN products p ON po.product_id = p.id;
