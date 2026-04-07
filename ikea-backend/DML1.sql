-- =========================================================
-- IKEA 쇼핑몰 백엔드 (PostgreSQL) 기존 테이블 수정용 ALTER
-- =========================================================

-- 1. members.deleted 추가
ALTER TABLE members
ADD COLUMN IF NOT EXISTS deleted BOOLEAN NOT NULL DEFAULT FALSE;

-- 2. members.login_id UNIQUE 보강
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_members_login_id'
    ) THEN
        ALTER TABLE members
        ADD CONSTRAINT uk_members_login_id UNIQUE (login_id);
    END IF;
END $$;

-- 3. carts.member_id 1:1 보장용 UNIQUE 추가
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_carts_member_id'
    ) THEN
        ALTER TABLE carts
        ADD CONSTRAINT uk_carts_member_id UNIQUE (member_id);
    END IF;
END $$;

-- 4. 인덱스 추가
CREATE INDEX IF NOT EXISTS idx_products_category_id
    ON products(category_id);

CREATE INDEX IF NOT EXISTS idx_carts_member_id
    ON carts(member_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_cart_id
    ON cart_items(cart_id);

CREATE INDEX IF NOT EXISTS idx_cart_items_product_id
    ON cart_items(product_id);

CREATE INDEX IF NOT EXISTS idx_notice_attachments_notice_id
    ON notice_attachments(notice_id);

CREATE INDEX IF NOT EXISTS idx_qnas_member_id
    ON qnas(member_id);

CREATE INDEX IF NOT EXISTS idx_qnas_parent_id
    ON qnas(parent_id);

CREATE INDEX IF NOT EXISTS idx_orders_member_id
    ON orders(member_id);

CREATE INDEX IF NOT EXISTS idx_orders_order_status
    ON orders(order_status);

CREATE INDEX IF NOT EXISTS idx_order_items_order_id
    ON order_items(order_id);

CREATE INDEX IF NOT EXISTS idx_order_items_product_id
    ON order_items(product_id);

CREATE INDEX IF NOT EXISTS idx_payments_member_id
    ON payments(member_id);

CREATE INDEX IF NOT EXISTS idx_payments_transaction_id
    ON payments(transaction_id);

CREATE INDEX IF NOT EXISTS idx_reviews_member_id
    ON reviews(member_id);

CREATE INDEX IF NOT EXISTS idx_reviews_order_id
    ON reviews(order_id);

CREATE INDEX IF NOT EXISTS idx_reviews_product_id
    ON reviews(product_id);

    	-- 상수로 정의된 카테고리 데이터 삽입
INSERT INTO category (name) VALUES 
('침대/매트리스'), ('소파'), ('식탁/테이블/의자'), ('책상'), 
('주방가구'), ('주방용품'), ('화분/식물');