START TRANSACTION;

-- voucher：按 (shop_id,title) 做幂等不够通用。
-- 稳妥做法：将 stage_voucher 的 shop_id 通过 (name,address) 映射到正式库的 shop.id（前提：TSV 中有 shop 的自然键信息）。
-- 如果 TSV 没有 shop 的 name/address，只能先假设 id 一致：那就按 (shop_id,title) 合并即可。
-- 这里演示“id 一致版”，与你当前导出的数据一致：
INSERT INTO tb_voucher
(id, shop_id, title, sub_title, rules, pay_value, actual_value, type, status, create_time, update_time)
SELECT id, shop_id, title, sub_title, rules, pay_value, actual_value, type, status, create_time, update_time
FROM stage_voucher sv
    ON DUPLICATE KEY UPDATE
                         sub_title   = VALUES(sub_title),
                         rules       = VALUES(rules),
                         pay_value   = VALUES(pay_value),
                         actual_value= VALUES(actual_value),
                         type        = VALUES(type),
                         status      = VALUES(status),
                         update_time = VALUES(update_time);

-- blog：同理，现阶段按 (title,user_id) 合并（你已在 V2 加过唯一约束）
INSERT INTO tb_blog
(id, shop_id, user_id, title, images, content, liked, comments, create_time, update_time)
SELECT id, shop_id, user_id, title, images, content, liked, comments, create_time, update_time
FROM stage_blog sb
    ON DUPLICATE KEY UPDATE
                         images      = VALUES(images),
                         content     = VALUES(content),
                         liked       = VALUES(liked),
                         comments    = VALUES(comments),
                         update_time = VALUES(update_time);

-- 重置自增
SET @mx := (SELECT IFNULL(MAX(id),0) + 1 FROM tb_voucher);
SET @sql := CONCAT('ALTER TABLE tb_voucher AUTO_INCREMENT = ', @mx);
PREPARE s3 FROM @sql; EXECUTE s3; DEALLOCATE PREPARE s3;

SET @mx := (SELECT IFNULL(MAX(id),0) + 1 FROM tb_blog);
SET @sql := CONCAT('ALTER TABLE tb_blog AUTO_INCREMENT = ', @mx);
PREPARE s4 FROM @sql; EXECUTE s4; DEALLOCATE PREPARE s4;

COMMIT;