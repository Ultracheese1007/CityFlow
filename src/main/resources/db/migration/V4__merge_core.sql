-- =====================================================================
-- V4__merge_core.sql
-- 合并核心表（无外键 / 被依赖方）：tb_user、tb_shop。
--
-- 策略：INSERT ... ON DUPLICATE KEY UPDATE，命中 V2 加的自然键时执行更新；
--      用 COALESCE 给关键 NOT NULL 字段兜底；
--      合并完毕重置 AUTO_INCREMENT，避免后续业务插入主键冲突。
-- =====================================================================

START TRANSACTION;

-- ---------- 1) tb_user ：以 phone 为自然键合并 ----------

INSERT INTO tb_user (id, phone, password, nick_name, icon, create_time, update_time)
SELECT
    id,
    phone,
    IFNULL(password, ''),
    IFNULL(nick_name, ''),
    IFNULL(icon, ''),
    COALESCE(create_time, NOW()),
    COALESCE(update_time, NOW())
FROM stage_user
WHERE phone IS NOT NULL
ON DUPLICATE KEY UPDATE
    password    = VALUES(password),
    nick_name   = VALUES(nick_name),
    icon        = VALUES(icon),
    update_time = VALUES(update_time);

-- ---------- 2) tb_shop ：以 (name, address) 为自然键合并 ----------

INSERT INTO tb_shop
    (id, name, type_id, images, area, address, x, y,
     avg_price, sold, comments, score, open_hours, create_time, update_time)
SELECT
    id, name, type_id,
    IFNULL(images, ''),
    area,
    IFNULL(address, ''),
    x, y,
    avg_price,
    IFNULL(sold, 0),
    IFNULL(comments, 0),
    IFNULL(score, 0),
    open_hours,
    COALESCE(create_time, NOW()),
    COALESCE(update_time, NOW())
FROM stage_shop
WHERE name IS NOT NULL AND address IS NOT NULL AND x IS NOT NULL AND y IS NOT NULL
ON DUPLICATE KEY UPDATE
    type_id     = VALUES(type_id),
    images      = VALUES(images),
    area        = VALUES(area),
    x           = VALUES(x),
    y           = VALUES(y),
    avg_price   = VALUES(avg_price),
    sold        = VALUES(sold),
    comments    = VALUES(comments),
    score       = VALUES(score),
    open_hours  = VALUES(open_hours),
    update_time = VALUES(update_time);

COMMIT;

-- ---------- 3) 重置 AUTO_INCREMENT 到 max(id)+1 ----------
-- ALTER TABLE 不能直接接子查询，用 PREPARE/EXECUTE 兜一下。

SELECT IFNULL(MAX(id), 0) + 1 INTO @next_user_id FROM tb_user;
SET @sql_user := CONCAT('ALTER TABLE tb_user AUTO_INCREMENT = ', @next_user_id);
PREPARE stmt FROM @sql_user;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT IFNULL(MAX(id), 0) + 1 INTO @next_shop_id FROM tb_shop;
SET @sql_shop := CONCAT('ALTER TABLE tb_shop AUTO_INCREMENT = ', @next_shop_id);
PREPARE stmt FROM @sql_shop;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
