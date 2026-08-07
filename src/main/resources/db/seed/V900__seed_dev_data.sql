-- Datos de desarrollo: catalogo, recetas, stock y usuarios suficientes para ejecutar una
-- compra completa (crear orden -> agregar items -> pagar -> descuento de inventario) contra
-- una base recien migrada.
--
-- Vive en db/seed/ y NO en db/migration/ a proposito: solo el perfil dev incluye esta location
-- (ver spring.flyway.locations en application-dev.yml), asi que estos datos no pueden viajar a
-- produccion aunque la rama se mergee. La version arranca en 900 para no chocar nunca con la
-- numeracion real del esquema.
--
-- Los UUID son fijos y legibles para poder usarlos directo en curl/Postman sin consultarlos antes.
-- Todas las contrasenas son 'password' (hash BCrypt generado con el mismo BCryptPasswordEncoder
-- que valida el login). Complementa a DevDataSeeder, que crea el usuario ADMIN y reutiliza la
-- primera sucursal existente: la que crea este seed.

-- ---------------------------------------------------------------------------
-- Sucursales
-- ---------------------------------------------------------------------------
INSERT INTO branches (id, name, address, city, state) VALUES
    ('11111111-1111-1111-1111-111111111111', 'Sucursal Centro', 'Av. Principal 100', 'Ciudad de Mexico', 'CDMX'),
    ('11111111-1111-1111-1111-222222222222', 'Sucursal Norte',  'Blvd. Norte 450',   'Monterrey',        'NL');

-- ---------------------------------------------------------------------------
-- Catalogo: categorias, ingredientes y productos
-- ---------------------------------------------------------------------------
-- categories.id es SERIAL: se insertan sin id para que la secuencia avance y las altas
-- posteriores por API no choquen con un id ya ocupado.
INSERT INTO categories (name) VALUES
    ('Bebidas Calientes'),
    ('Bebidas Frias'),
    ('Alimentos');

INSERT INTO ingredients (id, name, measure_unit) VALUES
    ('22222222-0000-0000-0000-000000000001', 'Cafe molido',          'g'),
    ('22222222-0000-0000-0000-000000000002', 'Leche entera',         'ml'),
    ('22222222-0000-0000-0000-000000000003', 'Agua purificada',      'ml'),
    ('22222222-0000-0000-0000-000000000004', 'Azucar',               'g'),
    ('22222222-0000-0000-0000-000000000005', 'Hielo',                'g'),
    ('22222222-0000-0000-0000-000000000006', 'Jarabe de vainilla',   'ml'),
    ('22222222-0000-0000-0000-000000000007', 'Chocolate en polvo',   'g'),
    ('22222222-0000-0000-0000-000000000008', 'Vaso 12 oz',           'pza');

INSERT INTO products (id, category_id, name, base_price) VALUES
    ('33333333-0000-0000-0000-000000000001', (SELECT id FROM categories WHERE name = 'Bebidas Calientes'), 'Espresso',            35.00),
    ('33333333-0000-0000-0000-000000000002', (SELECT id FROM categories WHERE name = 'Bebidas Calientes'), 'Americano',           40.00),
    ('33333333-0000-0000-0000-000000000003', (SELECT id FROM categories WHERE name = 'Bebidas Calientes'), 'Latte',               55.00),
    ('33333333-0000-0000-0000-000000000004', (SELECT id FROM categories WHERE name = 'Bebidas Calientes'), 'Capuchino',           52.00),
    ('33333333-0000-0000-0000-000000000005', (SELECT id FROM categories WHERE name = 'Bebidas Calientes'), 'Mocha',               62.00),
    ('33333333-0000-0000-0000-000000000006', (SELECT id FROM categories WHERE name = 'Bebidas Frias'),     'Frappe de Vainilla',  70.00),
    ('33333333-0000-0000-0000-000000000007', (SELECT id FROM categories WHERE name = 'Alimentos'),         'Croissant',           38.00);

-- ---------------------------------------------------------------------------
-- Recetas (consumo por unidad vendida)
-- ---------------------------------------------------------------------------
-- El Croissant se deja a proposito SIN receta: es el caso valido de un producto que se vende
-- pero no descuenta inventario (discountForSale corta si el producto no tiene lineas de receta).
INSERT INTO recipes (product_id, ingredient_id, required_quantity) VALUES
    -- Espresso: cafe + vaso
    ('33333333-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000001',  18.000),
    ('33333333-0000-0000-0000-000000000001', '22222222-0000-0000-0000-000000000008',   1.000),
    -- Americano: cafe + agua + vaso
    ('33333333-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000001',  18.000),
    ('33333333-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000003', 200.000),
    ('33333333-0000-0000-0000-000000000002', '22222222-0000-0000-0000-000000000008',   1.000),
    -- Latte: cafe + leche + vaso
    ('33333333-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000001',  18.000),
    ('33333333-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000002', 200.000),
    ('33333333-0000-0000-0000-000000000003', '22222222-0000-0000-0000-000000000008',   1.000),
    -- Capuchino: cafe + leche + vaso
    ('33333333-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000001',  18.000),
    ('33333333-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000002', 150.000),
    ('33333333-0000-0000-0000-000000000004', '22222222-0000-0000-0000-000000000008',   1.000),
    -- Mocha: cafe + leche + chocolate + vaso
    ('33333333-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000001',  18.000),
    ('33333333-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000002', 180.000),
    ('33333333-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000007',  20.000),
    ('33333333-0000-0000-0000-000000000005', '22222222-0000-0000-0000-000000000008',   1.000),
    -- Frappe de Vainilla: cafe + leche + hielo + jarabe + vaso
    ('33333333-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000001',  18.000),
    ('33333333-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000002', 150.000),
    ('33333333-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000005', 150.000),
    ('33333333-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000006',  30.000),
    ('33333333-0000-0000-0000-000000000006', '22222222-0000-0000-0000-000000000008',   1.000);

-- ---------------------------------------------------------------------------
-- Usuarios de acceso
-- ---------------------------------------------------------------------------
-- Contrasena de todos: 'password'.
INSERT INTO users (id, email, password_hash, first_name, last_name, role) VALUES
    ('44444444-0000-0000-0000-000000000001', 'gerente@coffeeshop.dev', '$2a$10$wceUP00uyzjByQg63UXEm.CCcCi/p8JuaSs8V7Fgbxhq/wozBWp8W', 'Lucia',  'Mendez',   'MANAGER'),
    ('44444444-0000-0000-0000-000000000002', 'cajero@coffeeshop.dev',  '$2a$10$OIlYjoflzM9yAaj2Qf8Nye05iHUM6VGNPfe./mIEfCfHy4lAj9AMm', 'Carlos', 'Ruiz',     'CASHIER'),
    ('44444444-0000-0000-0000-000000000003', 'barista@coffeeshop.dev', '$2a$10$YU2y2N1I5bAYal4NK4lV0eHCR4BNPxdwJu4XmkH7RegasNCkN5DDm', 'Diego',  'Fuentes',  'BARISTA'),
    ('44444444-0000-0000-0000-000000000004', 'cliente@coffeeshop.dev', '$2a$10$Bgh9k69zVrdJp3EXXLem9.WilX67O6oWGuwZ2ew3Cx2zbZ3zISmcu', 'Ana',    'Lopez',    'CUSTOMER');

INSERT INTO employees (user_id, branch_id, position, role, hire_date) VALUES
    ('44444444-0000-0000-0000-000000000001', '11111111-1111-1111-1111-111111111111', 'Gerente de sucursal', 'MANAGER', DATE '2025-03-01'),
    ('44444444-0000-0000-0000-000000000002', '11111111-1111-1111-1111-111111111111', 'Cajero',              'CASHIER', DATE '2025-06-15'),
    ('44444444-0000-0000-0000-000000000003', '11111111-1111-1111-1111-111111111111', 'Barista',             'BARISTA', DATE '2025-09-01');

INSERT INTO customers (user_id, loyalty_points, birth_date) VALUES
    ('44444444-0000-0000-0000-000000000004', 120, DATE '1998-05-12');

-- ---------------------------------------------------------------------------
-- Inventario por sucursal
-- ---------------------------------------------------------------------------
-- Toda receta exige que exista la fila de branch_inventory del ingrediente en esa sucursal:
-- discountForSale falla con 400 si no la encuentra. Por eso ambas sucursales llevan las ocho.
--
-- Sucursal Centro queda con stock holgado para operar. Sucursal Norte lleva a proposito solo
-- 10 g de chocolate (un Mocha consume 20 g) para poder probar el rechazo por stock insuficiente
-- y el rollback completo del pago sin tener que vaciar nada a mano.
INSERT INTO branch_inventory (branch_id, ingredient_id, current_quantity, minimum_stock) VALUES
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000001',  5000.000, 1000.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000002', 20000.000, 4000.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000003', 30000.000, 5000.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000004',  3000.000,  500.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000005', 10000.000, 2000.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000006',  2000.000,  400.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000007',  1500.000,  300.000),
    ('11111111-1111-1111-1111-111111111111', '22222222-0000-0000-0000-000000000008',   500.000,  100.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000001',  2500.000, 1000.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000002',  8000.000, 4000.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000003', 12000.000, 5000.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000004',  1200.000,  500.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000005',  4000.000, 2000.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000006',   800.000,  400.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000007',    10.000,  300.000),
    ('11111111-1111-1111-1111-222222222222', '22222222-0000-0000-0000-000000000008',   200.000,  100.000);

-- Movimiento INCOMING que justifica el stock inicial, para que el historial de movimientos de
-- cada ingrediente no arranque vacio. Se deriva de branch_inventory en vez de repetir cifras.
INSERT INTO inventory_movements (inventory_id, user_id, type, quantity, reason)
SELECT bi.id, '44444444-0000-0000-0000-000000000001', 'INCOMING', bi.current_quantity, 'Carga inicial de inventario'
FROM branch_inventory bi
WHERE bi.current_quantity > 0;

-- ---------------------------------------------------------------------------
-- Orden historica ya pagada
-- ---------------------------------------------------------------------------
-- Deja GET /api/v1/orders con contenido desde el primer arranque. Reproduce lo que habria
-- hecho el servicio: 2 Latte (55.00) + 1 Croissant (38.00) = 148.00 de subtotal, 16% de
-- impuesto = 23.68, total 171.68, cubierto por un unico pago en efectivo.
INSERT INTO orders (id, branch_id, customer_id, employee_id, status, subtotal, taxes, total, created_at) VALUES
    ('55555555-0000-0000-0000-000000000001',
     '11111111-1111-1111-1111-111111111111',
     '44444444-0000-0000-0000-000000000004',
     '44444444-0000-0000-0000-000000000002',
     'PAID', 148.00, 23.68, 171.68, CURRENT_TIMESTAMP - INTERVAL '2 days');

INSERT INTO order_items (order_id, product_id, quantity, unit_price, notes) VALUES
    ('55555555-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000003', 2, 55.00, 'sin azucar'),
    ('55555555-0000-0000-0000-000000000001', '33333333-0000-0000-0000-000000000007', 1, 38.00, NULL);

INSERT INTO payments (order_id, method, amount, payment_date) VALUES
    ('55555555-0000-0000-0000-000000000001', 'CASH', 171.68, CURRENT_TIMESTAMP - INTERVAL '2 days');

-- El consumo de esa orden se calcula desde las recetas en vez de escribirlo a mano, para que
-- inventario y ventas no puedan quedar descuadrados. El Croissant no aporta: no tiene receta.
WITH consumo AS (
    SELECT r.ingredient_id, SUM(r.required_quantity * oi.quantity) AS cantidad
    FROM order_items oi
    JOIN recipes r ON r.product_id = oi.product_id
    WHERE oi.order_id = '55555555-0000-0000-0000-000000000001'
    GROUP BY r.ingredient_id
)
UPDATE branch_inventory bi
SET current_quantity = bi.current_quantity - c.cantidad,
    last_updated     = CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM consumo c
WHERE bi.ingredient_id = c.ingredient_id
  AND bi.branch_id = '11111111-1111-1111-1111-111111111111';

WITH consumo AS (
    SELECT r.ingredient_id, SUM(r.required_quantity * oi.quantity) AS cantidad
    FROM order_items oi
    JOIN recipes r ON r.product_id = oi.product_id
    WHERE oi.order_id = '55555555-0000-0000-0000-000000000001'
    GROUP BY r.ingredient_id
)
INSERT INTO inventory_movements (inventory_id, user_id, type, quantity, reason, created_at)
SELECT bi.id,
       '44444444-0000-0000-0000-000000000002',
       'SALE',
       c.cantidad,
       'Order 55555555-0000-0000-0000-000000000001',
       CURRENT_TIMESTAMP - INTERVAL '2 days'
FROM consumo c
JOIN branch_inventory bi
  ON bi.ingredient_id = c.ingredient_id
 AND bi.branch_id = '11111111-1111-1111-1111-111111111111';
