USE wechat_backend;

-- 正确的 IF NOT EXISTS 语法：ADD COLUMN IF NOT EXISTS 列名 类型...
ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, SOLD';

ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS `inactive_date` DATE NULL COMMENT 'Date when the card becomes inactive';

ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS `sale_date` DATE NULL COMMENT 'Date when the card is sold';

ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS `sale_price` DECIMAL(10, 2) NULL COMMENT 'Sale price when status is SOLD';

ALTER TABLE cards
    ADD COLUMN IF NOT EXISTS `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Card creation time';

-- 初始化状态字段（避免空值）
UPDATE cards
SET `status` = 'ACTIVE'
WHERE `status` IS NULL OR `status` = '';

-- 确保状态字段非空（可选，因为ADD时已设置NOT NULL）
ALTER TABLE cards
    MODIFY COLUMN `status` VARCHAR(16) NOT NULL DEFAULT 'ACTIVE';