CREATE TABLE products (
    id         UUID           PRIMARY KEY,
    artist     VARCHAR(100)   NOT NULL,
    title      VARCHAR(100)   NOT NULL,
    price      DECIMAL(10,2)  NOT NULL,
    image_url  VARCHAR(500),
    created_at TIMESTAMP      NOT NULL DEFAULT now()
);