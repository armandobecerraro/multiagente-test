-- Crear la base de datos y esquemas iniciales
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Tabla de materiales prima
CREATE TABLE IF NOT EXISTS raw_materials (
    material_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    unit VARCHAR(20) NOT NULL,
    reorder_point INTEGER DEFAULT 100,
    max_stock INTEGER DEFAULT 1000,
    preferred_supplier VARCHAR(100),
    shelf_life_days INTEGER,
    storage_temp_min DECIMAL(5,2),
    storage_temp_max DECIMAL(5,2),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de ubicaciones de almacenamiento
CREATE TABLE IF NOT EXISTS storage_locations (
    location_id VARCHAR(50) PRIMARY KEY,
    location_name VARCHAR(255) NOT NULL,
    capacity INTEGER NOT NULL,
    current_utilization DECIMAL(5,2) DEFAULT 0.0,
    temperature DECIMAL(5,2),
    humidity DECIMAL(5,2),
    location_type VARCHAR(50) NOT NULL,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de inventario
CREATE TABLE IF NOT EXISTS inventory (
    id SERIAL PRIMARY KEY,
    material_id VARCHAR(50) REFERENCES raw_materials(material_id),
    current_stock INTEGER NOT NULL DEFAULT 0,
    reserved_stock INTEGER DEFAULT 0,
    location_id VARCHAR(50) REFERENCES storage_locations(location_id),
    last_updated TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(material_id, location_id)
);

-- Tabla de proveedores
CREATE TABLE IF NOT EXISTS suppliers (
    supplier_id VARCHAR(50) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    contact_email VARCHAR(255),
    contact_phone VARCHAR(50),
    address TEXT,
    rating DECIMAL(3,2) DEFAULT 5.0,
    is_active BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de órdenes de compra
CREATE TABLE IF NOT EXISTS purchase_orders (
    order_id VARCHAR(50) PRIMARY KEY,
    supplier_id VARCHAR(50) REFERENCES suppliers(supplier_id),
    material_id VARCHAR(50) REFERENCES raw_materials(material_id),
    quantity INTEGER NOT NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    total_amount DECIMAL(10,2) GENERATED ALWAYS AS (quantity * unit_price) STORED,
    status VARCHAR(50) DEFAULT 'PENDING',
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expected_delivery TIMESTAMP,
    actual_delivery TIMESTAMP,
    created_by VARCHAR(100)
);

-- Tabla de rendimiento de proveedores
CREATE TABLE IF NOT EXISTS supplier_performance (
    id SERIAL PRIMARY KEY,
    supplier_id VARCHAR(50) REFERENCES suppliers(supplier_id),
    order_id VARCHAR(50) REFERENCES purchase_orders(order_id),
    delivery_time INTEGER, -- días
    quality_score DECIMAL(3,2), -- 1-10
    price_competitiveness DECIMAL(3,2), -- 1-10
    evaluation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Tabla de transacciones de inventario
CREATE TABLE IF NOT EXISTS inventory_transactions (
    transaction_id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    material_id VARCHAR(50) REFERENCES raw_materials(material_id),
    location_id VARCHAR(50) REFERENCES storage_locations(location_id),
    transaction_type VARCHAR(20) NOT NULL, -- 'IN', 'OUT', 'TRANSFER'
    quantity INTEGER NOT NULL,
    reference_id VARCHAR(100), -- ID de orden, producción, etc.
    notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(100)
);

-- Tabla de lotes de materiales
CREATE TABLE IF NOT EXISTS material_batches (
    batch_id VARCHAR(50) PRIMARY KEY,
    material_id VARCHAR(50) REFERENCES raw_materials(material_id),
    location_id VARCHAR(50) REFERENCES storage_locations(location_id),
    quantity INTEGER NOT NULL,
    manufacture_date DATE,
    expiry_date DATE,
    supplier_id VARCHAR(50) REFERENCES suppliers(supplier_id),
    quality_status VARCHAR(20) DEFAULT 'APPROVED',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices para mejorar rendimiento
CREATE INDEX IF NOT EXISTS idx_inventory_material_id ON inventory(material_id);
CREATE INDEX IF NOT EXISTS idx_inventory_location_id ON inventory(location_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_supplier_id ON purchase_orders(supplier_id);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_status ON purchase_orders(status);
CREATE INDEX IF NOT EXISTS idx_purchase_orders_order_date ON purchase_orders(order_date);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_material_id ON inventory_transactions(material_id);
CREATE INDEX IF NOT EXISTS idx_inventory_transactions_created_at ON inventory_transactions(created_at);
CREATE INDEX IF NOT EXISTS idx_material_batches_material_id ON material_batches(material_id);
CREATE INDEX IF NOT EXISTS idx_material_batches_expiry_date ON material_batches(expiry_date);

-- Insertar datos de ejemplo
INSERT INTO storage_locations (location_id, location_name, capacity, location_type, temperature, humidity) VALUES
('LOC001', 'Almacén Principal A', 1000, 'WAREHOUSE', 20.0, 45.0),
('LOC002', 'Almacén Refrigerado B', 500, 'COLD_STORAGE', 4.0, 80.0),
('LOC003', 'Área de Cuarentena', 200, 'QUARANTINE', 15.0, 50.0),
('LOC004', 'Almacén de Químicos', 300, 'CHEMICAL_STORAGE', 18.0, 40.0)
ON CONFLICT (location_id) DO NOTHING;

INSERT INTO suppliers (supplier_id, name, contact_email, contact_phone, rating) VALUES
('SUP001', 'Materiales Industriales SA', 'ventas@matind.com', '+1-555-0101', 8.5),
('SUP002', 'Químicos y Reactivos Ltda', 'pedidos@quimreact.com', '+1-555-0102', 9.0),
('SUP003', 'Distribuidora Nacional', 'comercial@distnac.com', '+1-555-0103', 7.8),
('SUP004', 'Proveedores Especializados', 'info@provesp.com', '+1-555-0104', 8.2)
ON CONFLICT (supplier_id) DO NOTHING;

INSERT INTO raw_materials (material_id, name, unit, reorder_point, max_stock, preferred_supplier, shelf_life_days, storage_temp_min, storage_temp_max) VALUES
('MAT001', 'Acero Inoxidable 304', 'KG', 100, 1000, 'SUP001', 3650, 15.0, 25.0),
('MAT002', 'Polietileno de Alta Densidad', 'KG', 200, 2000, 'SUP003', 1825, 10.0, 30.0),
('MAT003', 'Ácido Sulfúrico 98%', 'L', 50, 500, 'SUP002', 365, 15.0, 25.0),
('MAT004', 'Catalizador Platino', 'G', 10, 100, 'SUP004', 1095, 18.0, 22.0),
('MAT005', 'Resina Epoxi', 'KG', 75, 750, 'SUP001', 730, 10.0, 25.0)
ON CONFLICT (material_id) DO NOTHING;

-- Insertar inventario inicial
INSERT INTO inventory (material_id, current_stock, location_id) VALUES
('MAT001', 500, 'LOC001'),
('MAT002', 800, 'LOC001'),
('MAT003', 150, 'LOC004'),
('MAT004', 25, 'LOC003'),
('MAT005', 300, 'LOC001')
ON CONFLICT (material_id, location_id) DO NOTHING;
