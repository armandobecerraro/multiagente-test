-- Additional tables required by the agents
-- This file extends the main schema.sql

-- Suppliers Table
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id VARCHAR(50) PRIMARY KEY,
    supplier_name VARCHAR(100) NOT NULL,
    contact_info TEXT,
    performance_rating DECIMAL(3,2) DEFAULT 5.0,
    default_lead_time_days INTEGER DEFAULT 7,
    completed_orders INTEGER DEFAULT 0,
    category VARCHAR(50),
    quality_rating DECIMAL(3,2) DEFAULT 8.0,
    is_active BOOLEAN DEFAULT TRUE,
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Material Batches Table
CREATE TABLE IF NOT EXISTS material_batches (
    batch_id VARCHAR(50) PRIMARY KEY,
    material_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    supplier_id VARCHAR(50) REFERENCES suppliers(supplier_id),
    received_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expiry_date TIMESTAMP,
    quality_score DECIMAL(3,2) DEFAULT 8.0,
    location_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'AVAILABLE'
);

-- Inventory Transactions Table
CREATE TABLE IF NOT EXISTS inventory_transactions (
    transaction_id VARCHAR(50) PRIMARY KEY,
    transaction_type VARCHAR(20) NOT NULL, -- IN, OUT, TRANSFER, ADJUSTMENT
    material_id VARCHAR(50) NOT NULL,
    quantity INTEGER NOT NULL,
    previous_stock INTEGER,
    new_stock INTEGER,
    reference_id VARCHAR(50), -- Order ID, Production Run ID, etc.
    notes TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    agent_id VARCHAR(50)
);

-- Storage Locations Table
CREATE TABLE IF NOT EXISTS storage_locations (
    location_id VARCHAR(50) PRIMARY KEY,
    location_name VARCHAR(100) NOT NULL,
    capacity INTEGER DEFAULT 1000,
    current_utilization DECIMAL(5,2) DEFAULT 0.0,
    temperature DECIMAL(5,2),
    humidity DECIMAL(5,2),
    location_type VARCHAR(30) DEFAULT 'GENERAL'
);

-- Production Plans Table
CREATE TABLE IF NOT EXISTS production_plans (
    plan_id VARCHAR(50) PRIMARY KEY,
    product_id INTEGER REFERENCES products(id),
    target_quantity INTEGER NOT NULL,
    start_date TIMESTAMP,
    end_date TIMESTAMP,
    priority VARCHAR(20) DEFAULT 'NORMAL',
    status VARCHAR(20) DEFAULT 'PLANNED',
    created_by VARCHAR(50),
    created_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Production Lines Table
CREATE TABLE IF NOT EXISTS production_lines (
    line_id VARCHAR(50) PRIMARY KEY,
    line_name VARCHAR(100) NOT NULL,
    capacity INTEGER DEFAULT 100,
    current_status VARCHAR(20) DEFAULT 'IDLE',
    efficiency_rating DECIMAL(5,2) DEFAULT 85.0,
    is_active BOOLEAN DEFAULT TRUE
);

-- Production Equipment Table
CREATE TABLE IF NOT EXISTS production_equipment (
    equipment_id VARCHAR(50) PRIMARY KEY,
    equipment_name VARCHAR(100) NOT NULL,
    line_id VARCHAR(50) REFERENCES production_lines(line_id),
    equipment_type VARCHAR(50),
    efficiency_rating DECIMAL(5,2) DEFAULT 90.0,
    status VARCHAR(20) DEFAULT 'OPERATIONAL',
    last_maintenance TIMESTAMP,
    next_maintenance TIMESTAMP
);

-- Production Runs Table
CREATE TABLE IF NOT EXISTS production_runs (
    run_id VARCHAR(50) PRIMARY KEY,
    plan_id VARCHAR(50) REFERENCES production_plans(plan_id),
    line_id VARCHAR(50) REFERENCES production_lines(line_id),
    product_id INTEGER REFERENCES products(id),
    target_quantity INTEGER NOT NULL,
    produced_quantity INTEGER DEFAULT 0,
    start_time TIMESTAMP,
    estimated_end_time TIMESTAMP,
    actual_end_time TIMESTAMP,
    status VARCHAR(20) DEFAULT 'PLANNED',
    efficiency DECIMAL(5,2),
    quality_score DECIMAL(3,2)
);

-- Production Metrics Table
CREATE TABLE IF NOT EXISTS production_metrics (
    id SERIAL PRIMARY KEY,
    line_id VARCHAR(50) REFERENCES production_lines(line_id),
    run_id VARCHAR(50) REFERENCES production_runs(run_id),
    target_quantity INTEGER,
    produced_quantity INTEGER,
    efficiency DECIMAL(5,2),
    quality_score DECIMAL(3,2),
    downtime_minutes INTEGER DEFAULT 0,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Deliveries Table
CREATE TABLE IF NOT EXISTS deliveries (
    delivery_id VARCHAR(50) PRIMARY KEY,
    order_id INTEGER REFERENCES orders(id),
    transporter_id VARCHAR(50),
    status VARCHAR(20) DEFAULT 'SCHEDULED',
    pickup_address TEXT,
    delivery_address TEXT,
    scheduled_pickup TIMESTAMP,
    actual_pickup TIMESTAMP,
    estimated_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    last_update TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Supplier Performance Table
CREATE TABLE IF NOT EXISTS supplier_performance (
    id SERIAL PRIMARY KEY,
    supplier_id VARCHAR(50) REFERENCES suppliers(supplier_id),
    order_id VARCHAR(50),
    delivery_time INTEGER, -- in days
    quality_score DECIMAL(3,2),
    price_competitiveness DECIMAL(3,2),
    evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Update raw_materials table to include additional fields used by agents
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS material_id VARCHAR(50) UNIQUE;
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS reorder_point INTEGER DEFAULT 10;
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS max_stock INTEGER DEFAULT 1000;
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS preferred_supplier VARCHAR(50);
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS shelf_life_days INTEGER;
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS storage_temp_min DECIMAL(5,2);
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS storage_temp_max DECIMAL(5,2);

-- Update inventory table to include additional fields
ALTER TABLE inventory ADD COLUMN IF NOT EXISTS material_id VARCHAR(50);
ALTER TABLE inventory ADD COLUMN IF NOT EXISTS location_id VARCHAR(50);

-- Update purchase_orders table structure to match agent usage
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS order_id VARCHAR(50) UNIQUE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS po_id VARCHAR(50) UNIQUE;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS material_id_str VARCHAR(50);
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS expected_delivery_date TIMESTAMP;
ALTER TABLE purchase_orders ADD COLUMN IF NOT EXISTS actual_delivery_date TIMESTAMP;

-- Create indexes for the new tables
CREATE INDEX IF NOT EXISTS idx_material_batches_material ON material_batches(material_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_material ON inventory_transactions(material_id);
CREATE INDEX IF NOT EXISTS idx_production_runs_line ON production_runs(line_id);
CREATE INDEX IF NOT EXISTS idx_production_runs_status ON production_runs(status);
CREATE INDEX IF NOT EXISTS idx_deliveries_order ON deliveries(order_id);
CREATE INDEX IF NOT EXISTS idx_supplier_performance_supplier ON supplier_performance(supplier_id);

-- Insert some sample data for testing
INSERT INTO suppliers (supplier_id, supplier_name, contact_info, performance_rating, default_lead_time_days, category) VALUES
('SUPPLIER_001', 'Raw Materials Inc.', 'contact@rawmaterials.com', 8.5, 5, 'CHEMICALS'),
('SUPPLIER_002', 'Quality Components Ltd.', 'info@qualitycomp.com', 9.0, 7, 'ELECTRONICS'),
('SUPPLIER_003', 'Fast Delivery Co.', 'orders@fastdelivery.com', 7.5, 3, 'PACKAGING')
ON CONFLICT (supplier_id) DO NOTHING;

INSERT INTO production_lines (line_id, line_name, capacity, current_status, efficiency_rating) VALUES
('LINE_001', 'Assembly Line 1', 200, 'IDLE', 92.5),
('LINE_002', 'Packaging Line 1', 150, 'IDLE', 88.0),
('LINE_003', 'Quality Control Line', 100, 'IDLE', 95.0)
ON CONFLICT (line_id) DO NOTHING;

INSERT INTO storage_locations (location_id, location_name, capacity, current_utilization, location_type) VALUES
('LOC_001', 'Main Warehouse A1', 1000, 45.0, 'GENERAL'),
('LOC_002', 'Cold Storage B1', 500, 30.0, 'COLD'),
('LOC_003', 'Hazmat Storage C1', 200, 15.0, 'HAZMAT')
ON CONFLICT (location_id) DO NOTHING;
