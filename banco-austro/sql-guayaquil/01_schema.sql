CREATE TABLE clientes (
    cedula VARCHAR(10) PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    ciudad VARCHAR(50) NOT NULL
);

CREATE TABLE cuentas (
    numero VARCHAR(20) PRIMARY KEY,
    saldo NUMERIC(12, 2) NOT NULL,
    oficina VARCHAR(50) NOT NULL,
    CONSTRAINT chk_oficina CHECK (oficina = 'GUAYAQUIL')
);