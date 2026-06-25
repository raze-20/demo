-- 1. Definition of Strict Types (Enums)
CREATE TYPE user_role AS ENUM ('ADMIN', 'MANAGER', 'CASHIER', 'BARISTA', 'CUSTOMER');
CREATE TYPE order_status AS ENUM ('PENDING', 'PAID', 'PREPARING', 'DELIVERED', 'CANCELLED');
CREATE TYPE payment_method AS ENUM ('CASH', 'CARD', 'LOYALTY_POINTS', 'TRANSFER');
CREATE TYPE movement_type AS ENUM ('INCOMING', 'SALE', 'WASTE', 'ADJUSTMENT');

-- 2. Base Tables and Users
CREATE TABLE branches (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    address TEXT NOT NULL,
    city VARCHAR(50) NOT NULL,
    state VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    role user_role NOT NULL,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE customers (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    loyalty_points INTEGER DEFAULT 0 CHECK (loyalty_points >= 0),
    birth_date DATE
);

CREATE TABLE employees (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    branch_id UUID REFERENCES branches(id) ON DELETE RESTRICT,
    position VARCHAR(100) NOT NULL,
    role VARCHAR(100) NOT NULL,
    hire_date DATE NOT NULL
);

-- 3. Catalog and Recipes (Bill of Materials)
CREATE TABLE categories (
    id SERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE products (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    category_id INTEGER REFERENCES categories(id) ON DELETE RESTRICT,
    name VARCHAR(100) NOT NULL,
    base_price NUMERIC(10, 2) NOT NULL CHECK (base_price >= 0),
    active BOOLEAN DEFAULT TRUE
);

CREATE TABLE ingredients (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL UNIQUE,
    measure_unit VARCHAR(20) NOT NULL
);

CREATE TABLE recipes (
    product_id UUID REFERENCES products(id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE RESTRICT,
    required_quantity NUMERIC(10, 3) NOT NULL CHECK (required_quantity > 0),
    PRIMARY KEY (product_id, ingredient_id)
);

-- 4. Physical Inventory
CREATE TABLE branch_inventory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID REFERENCES branches(id) ON DELETE CASCADE,
    ingredient_id UUID REFERENCES ingredients(id) ON DELETE RESTRICT,
    current_quantity NUMERIC(10, 3) DEFAULT 0 CHECK (current_quantity >= 0),
    minimum_stock NUMERIC(10, 3) DEFAULT 0 CHECK (minimum_stock >= 0),
    last_updated TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (branch_id, ingredient_id)
);

CREATE TABLE inventory_movements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    inventory_id UUID REFERENCES branch_inventory(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE RESTRICT,
    type movement_type NOT NULL,
    quantity NUMERIC(10, 3) NOT NULL CHECK (quantity > 0),
    reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- 5. Transactional: Orders and Payments
CREATE TABLE orders (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    branch_id UUID REFERENCES branches(id) ON DELETE RESTRICT,
    customer_id UUID REFERENCES customers(user_id) ON DELETE SET NULL,
    employee_id UUID REFERENCES employees(user_id) ON DELETE RESTRICT,
    status order_status DEFAULT 'PENDING',
    subtotal NUMERIC(10, 2) NOT NULL DEFAULT 0,
    taxes NUMERIC(10, 2) NOT NULL DEFAULT 0,
    total NUMERIC(10, 2) NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    product_id UUID REFERENCES products(id) ON DELETE RESTRICT,
    quantity INTEGER NOT NULL CHECK (quantity > 0),
    unit_price NUMERIC(10, 2) NOT NULL CHECK (unit_price >= 0),
    notes TEXT
);

CREATE TABLE payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
    method payment_method NOT NULL,
    amount NUMERIC(10, 2) NOT NULL CHECK (amount > 0),
    payment_date TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);