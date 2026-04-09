2) 상품 데이터가 실제로 비어 있는지 확인
SELECT COUNT(*) AS product_count
FROM products;

이게 0이면 /api/product가 빈 배열인 원인이 거의 확정입니다.

상품 샘플까지 보고 싶으면:

SELECT product_id, name, price, category_id, created_at
FROM products
ORDER BY product_id;
3) 카테고리는 들어가 있는지 확인

압축파일의 DML1.sql에는 카테고리 insert만 있습니다.
그래서 카테고리만 있고 상품은 없을 가능성이 높습니다.

SELECT id, name
FROM category
ORDER BY id;

여기서 카테고리가 정상인데 products만 비어 있으면 지금 문제와 딱 맞습니다.

4) 재고 테이블도 같이 확인

상품이 있더라도 product_stocks가 없으면 재고 API가 계속 문제를 일으킬 수 있습니다.

SELECT COUNT(*) AS stock_count
FROM product_stocks;

그리고 매칭 확인:

SELECT
    p.product_id,
    p.name,
    ps.stock_id,
    ps.quantity
FROM products p
LEFT JOIN product_stocks ps
    ON ps.product_id = p.product_id
ORDER BY p.product_id;

여기서 stock_id가 NULL인 상품이 있으면 재고 row가 없는 상품입니다.

5) 지금 가장 안전한 최소 seed SQL

P2 제외, 현재 연동만 살리는 목적이면
카테고리 → 상품 → 재고 이 3개만 맞추면 됩니다.

아래는 바로 넣을 수 있는 형태입니다.

5-1. 카테고리 확인 후 없으면 추가
INSERT INTO category (name)
SELECT '소파'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '소파'
);

INSERT INTO category (name)
SELECT '침대/매트리스'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '침대/매트리스'
);

INSERT INTO category (name)
SELECT '식탁/테이블/의자'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '식탁/테이블/의자'
);

INSERT INTO category (name)
SELECT '책상'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '책상'
);

INSERT INTO category (name)
SELECT '주방가구'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '주방가구'
);

INSERT INTO category (name)
SELECT '주방용품'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '주방용품'
);

INSERT INTO category (name)
SELECT '화분/식물'
WHERE NOT EXISTS (
    SELECT 1 FROM category WHERE name = '화분/식물'
);
5-2. product_code 컬럼 아직 안 넣었으면 먼저 추가

앞 단계 수정 기준입니다.

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
5-3. 최소 상품 seed 예시

아래는 예시라서 이미지 경로, 이름, 가격은 팀 상황에 맞게 바꾸면 됩니다.

INSERT INTO products (
    product_code,
    name,
    price,
    original_price,
    img_path,
    brand,
    badge,
    label,
    type_slug,
    attributes,
    detail_content,
    gallery_images,
    dimension_image_path,
    created_at,
    category_id
)
SELECT
    '10489009',
    'GLOSTAD 2인용 소파',
    129000,
    149000,
    '/uploads/products/glostad.jpg',
    'IKEA',
    'BEST',
    '신상품',
    'sofa',
    '[]',
    'GLOSTAD 상품 상세 설명',
    '[]',
    '/uploads/products/glostad-dimension.jpg',
    NOW(),
    c.id
FROM category c
WHERE c.name = '소파'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_code = '10489009'
  );

INSERT INTO products (
    product_code,
    name,
    price,
    original_price,
    img_path,
    brand,
    badge,
    label,
    type_slug,
    attributes,
    detail_content,
    gallery_images,
    dimension_image_path,
    created_at,
    category_id
)
SELECT
    's29509026',
    'MALM 침대 프레임',
    299000,
    329000,
    '/uploads/products/malm-bed.jpg',
    'IKEA',
    'HOT',
    '추천',
    'bed',
    '[]',
    'MALM 상품 상세 설명',
    '[]',
    '/uploads/products/malm-dimension.jpg',
    NOW(),
    c.id
FROM category c
WHERE c.name = '침대/매트리스'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_code = 's29509026'
  );

INSERT INTO products (
    product_code,
    name,
    price,
    original_price,
    img_path,
    brand,
    badge,
    label,
    type_slug,
    attributes,
    detail_content,
    gallery_images,
    dimension_image_path,
    created_at,
    category_id
)
SELECT
    '30522041',
    'LISABO 테이블',
    199000,
    219000,
    '/uploads/products/lisabo-table.jpg',
    'IKEA',
    'NEW',
    '인기',
    'table',
    '[]',
    'LISABO 상품 상세 설명',
    '[]',
    '/uploads/products/lisabo-dimension.jpg',
    NOW(),
    c.id
FROM category c
WHERE c.name = '식탁/테이블/의자'
  AND NOT EXISTS (
      SELECT 1 FROM products WHERE product_code = '30522041'
  );
5-4. 상품별 재고 row 생성
INSERT INTO product_stocks (product_id, quantity, updated_at)
SELECT p.product_id, 10, NOW()
FROM products p
LEFT JOIN product_stocks ps
    ON ps.product_id = p.product_id
WHERE ps.product_id IS NULL;

이 쿼리는 재고 row 없는 상품만 채웁니다.

6) 적용 후 최종 확인 SQL
상품 목록 확인
SELECT product_id, product_code, name, price
FROM products
ORDER BY product_id;
재고 확인
SELECT p.product_id, p.product_code, p.name, ps.quantity
FROM products p
LEFT JOIN product_stocks ps
    ON ps.product_id = p.product_id
ORDER BY p.product_id;
API 기준 기대 상태
/api/product → 빈 배열이 아니어야 함
각 상품에 productId, productCode 둘 다 있어야 함
/api/product_stocks/{productId} → tracked=false 또는 정상 재고 응답
장바구니/리뷰/주문에서 productCode 또는 productId 기준 조회 가능
7) 지금 바로 팀에 전달할 핵심 요약

그대로 전달해도 되는 문장으로 정리하면 이겁니다.

현재 /api/product 가 [] 로 나오는 원인은 코드보다 DB 데이터 상태 문제일 가능성이 높다.
백엔드는 localhost:5440/ikea DB를 보고 있고, ProductService는 findAll()만 수행한다.
제공된 SQL에는 category insert는 있지만 product insert는 없다.
따라서 products 테이블 row와 product_stocks row를 먼저 채워야 프론트 실제 연동이 가능하다.
추가로 상품 식별자 통일을 위해 products.product_code 컬럼을 함께 운영해야 한다.
8) 여기서 남은 실무 체크 2개
프론트 fallback 데이터의 상품 코드와
DB에 넣는 product_code를 정확히 일치시킬 것
이미지 경로 img_path는
실제 프론트가 표시 가능한 경로인지 확인할 것
지금 코드상 업로드 경로는 C:/uploads/products/ 기준입니다