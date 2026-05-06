-- =====================================================================
-- V5__merge_refs.sql
-- 合并依赖表：tb_voucher（依赖 shop）、tb_blog（依赖 user + shop）。
-- 必须在 V4 之后执行。
-- =====================================================================

START TRANSACTION;

-- ---------- 1) tb_voucher ：以 (shop_id, title) 为自然键合并 ----------

INSERT INTO tb_voucher
    (id, shop_id, title, sub_title, rules,
     pay_value, actual_value, type, status, create_time, update_time)
SELECT
    id, shop_id, title, sub_title, rules,
    IFNULL(pay_value, 0),
    IFNULL(actual_value, 0),
    IFNULL(type, 0),
    IFNULL(status, 1),
    COALESCE(create_time, NOW()),
    COALESCE(update_time, NOW())
FROM stage_voucher
WHERE shop_id IS NOT NULL AND title IS NOT NULL
ON DUPLICATE KEY UPDATE
    sub_title    = VALUES(sub_title),
    rules        = VALUES(rules),
    pay_value    = VALUES(pay_value),
    actual_value = VALUES(actual_value),
    type         = VALUES(type),
    status       = VALUES(status),
    update_time  = VALUES(update_time);

-- ---------- 2) tb_blog ：以 (title, user_id, shop_id) 为自然键合并 ----------
-- 注意：原始 TSV 中 id=6 与 id=7 自然键完全相同，会被合并为一条
-- （这是数据本身的重复，工程化保留唯一约束、容忍这一条丢失）。

INSERT INTO tb_blog
    (id, shop_id, user_id, title, images, content,
     liked, comments, create_time, update_time)
SELECT
    id, shop_id, user_id, title,
    IFNULL(images, ''),
    IFNULL(content, ''),
    IFNULL(liked, 0),
    comments,
    COALESCE(create_time, NOW()),
    COALESCE(update_time, NOW())
FROM stage_blog
WHERE title IS NOT NULL AND user_id IS NOT NULL AND shop_id IS NOT NULL
ON DUPLICATE KEY UPDATE
    images      = VALUES(images),
    content     = VALUES(content),
    liked       = VALUES(liked),
    comments    = VALUES(comments),
    update_time = VALUES(update_time);

COMMIT;

-- ---------- 3) 重置 AUTO_INCREMENT ----------

SELECT IFNULL(MAX(id), 0) + 1 INTO @next_voucher_id FROM tb_voucher;
SET @sql_voucher := CONCAT('ALTER TABLE tb_voucher AUTO_INCREMENT = ', @next_voucher_id);
PREPARE stmt FROM @sql_voucher;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IFNULL(MAX(id), 0) + 1 INTO @next_blog_id FROM tb_blog;
SET @sql_blog := CONCAT('ALTER TABLE tb_blog AUTO_INCREMENT = ', @next_blog_id);
PREPARE stmt FROM @sql_blog;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
