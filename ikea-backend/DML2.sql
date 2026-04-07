ALTER TABLE products
ADD COLUMN original_price INTEGER,
ADD COLUMN brand VARCHAR(100),
ADD COLUMN badge VARCHAR(100),
ADD COLUMN label VARCHAR(100),
ADD COLUMN type_slug VARCHAR(100),
ADD COLUMN attributes TEXT,
ADD COLUMN detail_content TEXT,
ADD COLUMN gallery_images TEXT,
ADD COLUMN dimension_image_path VARCHAR(500);