ALTER TABLE products
ADD COLUMN product_code VARCHAR(100);

ALTER TABLE products
ADD CONSTRAINT uk_products_product_code UNIQUE (product_code);

-- -- 그 다음 실제 프론트에서 쓰는 화면용 ID를 넣어야 합니다.

-- 예시:

-- UPDATE products SET product_code = '10489009' WHERE product_id = 1;
-- UPDATE products SET product_code = 's29509026' WHERE product_id = 2;
-- UPDATE products SET product_code = '305.220.41' WHERE product_id = 3;

-- 아직 어떤 상품에 어떤 코드가 들어가야 하는지 정리가 안 되어 있으면, 임시로라도 넣어두는 게 좋습니다.

-- UPDATE products
-- SET product_code = CONCAT('P', product_id)
-- -- WHERE product_code IS NULL;

ALTER TABLE products
ADD COLUMN IF NOT EXISTS product_code VARCHAR(100);

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_products_product_code'
    ) THEN
        ALTER TABLE products
        ADD CONSTRAINT uk_products_product_code UNIQUE (product_code);
    END IF;
END $$;

INSERT INTO product_stocks (product_id, quantity, updated_at)
SELECT p.product_id, 10, NOW()
FROM products p
LEFT JOIN product_stocks ps
    ON ps.product_id = p.product_id
WHERE ps.product_id IS NULL;