CREATE EXTENSION IF NOT EXISTS "pgcrypto";

CREATE TABLE carts (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id VARCHAR NOT NULL UNIQUE,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE cart_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  cart_id UUID REFERENCES carts(id) ON DELETE CASCADE,
  book_id UUID NOT NULL,
  book_title VARCHAR(500) NOT NULL,
  book_cover VARCHAR(500),
  book_type VARCHAR(20) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  quantity INT DEFAULT 1,
  added_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE addresses (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id VARCHAR NOT NULL,
  name VARCHAR(255) NOT NULL,
  street VARCHAR(500) NOT NULL,
  number VARCHAR(50),
  complement VARCHAR(255),
  district VARCHAR(255) NOT NULL,
  city VARCHAR(255) NOT NULL,
  province VARCHAR(255) NOT NULL,
  country VARCHAR(100) DEFAULT 'Moçambique',
  postal_code VARCHAR(20),
  is_default BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE orders (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_number VARCHAR(20) UNIQUE NOT NULL,
  user_id VARCHAR NOT NULL,
  address_id UUID REFERENCES addresses(id),
  status VARCHAR(30) DEFAULT 'PENDING' NOT NULL,
  subtotal DECIMAL(10,2) NOT NULL,
  delivery_fee DECIMAL(10,2) DEFAULT 0,
  total DECIMAL(10,2) NOT NULL,
  currency VARCHAR(10) DEFAULT 'MZN',
  notes TEXT,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE order_items (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
  book_id UUID NOT NULL,
  book_title VARCHAR(500) NOT NULL,
  book_type VARCHAR(20) NOT NULL,
  quantity INT DEFAULT 1,
  unit_price DECIMAL(10,2) NOT NULL,
  total_price DECIMAL(10,2) NOT NULL
);

CREATE TABLE payments (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  order_id UUID REFERENCES orders(id),
  subscription_id UUID,
  user_id VARCHAR NOT NULL,
  method VARCHAR(20) NOT NULL,
  status VARCHAR(20) DEFAULT 'PENDING',
  amount DECIMAL(10,2) NOT NULL,
  currency VARCHAR(10) DEFAULT 'MZN',
  external_id VARCHAR(255),
  mpesa_conversation_id VARCHAR(255),
  mpesa_third_party_ref VARCHAR(255),
  emola_reference VARCHAR(255),
  stripe_payment_intent_id VARCHAR(255),
  paypal_order_id VARCHAR(255),
  phone_number VARCHAR(30),
  failure_reason TEXT,
  paid_at TIMESTAMP,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

CREATE TABLE subscription_plans (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  name VARCHAR(100) NOT NULL,
  description TEXT,
  type VARCHAR(20) NOT NULL,
  price DECIMAL(10,2) NOT NULL,
  currency VARCHAR(10) DEFAULT 'MZN',
  is_active BOOLEAN DEFAULT true
);

CREATE TABLE subscriptions (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id VARCHAR UNIQUE NOT NULL,
  plan_id UUID REFERENCES subscription_plans(id),
  status VARCHAR(20) DEFAULT 'PENDING',
  start_date TIMESTAMP,
  end_date TIMESTAMP,
  auto_renew BOOLEAN DEFAULT true,
  cancelled_at TIMESTAMP,
  expiry_notified_7d BOOLEAN DEFAULT false,
  expiry_notified_3d BOOLEAN DEFAULT false,
  expiry_notified_1d BOOLEAN DEFAULT false,
  created_at TIMESTAMP DEFAULT NOW(),
  updated_at TIMESTAMP DEFAULT NOW()
);

INSERT INTO subscription_plans (id, name, type, price, currency, description) VALUES
  (gen_random_uuid(), 'Mensal', 'MONTHLY', 299.00, 'MZN', 'Acesso ilimitado por 1 mês'),
  (gen_random_uuid(), 'Anual', 'ANNUAL', 2499.00, 'MZN', 'Acesso ilimitado por 1 ano — poupa 58%');
