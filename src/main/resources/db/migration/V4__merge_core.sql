START TRANSACTION;

-- user：按唯一键 phone 幂等合并
INSERT INTO tb_user (id, phone, password, nick_name, icon, create_time, update_time)
SELECT id, phone, password, nick_name, icon, create_time, update_time
FROM stage_user su
-- 如果目标表已有这部份数据，依赖 phone 唯一约束做覆盖式更新
    ON DUPLICATE KEY UPDATE
                         password = VALUES(password),
                         nick_name = VALUES(nick_name),
                         icon = VALUES(icon),
                         update_time = VALUES(update_time);

-- shop：按 (name,address) 幂等合并
INSERT INTO tb_shop
(id,name,type_id,images,area,address,x,y,avg_price,sold,comments,score,open_hours,create_time,update_time)
SELECT
    id,name,type_id,images,area,address,x,y,avg_price,sold,comments,score,open_hours,create_time,update_time
FROM stage_shop ss
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

-- 重置自增，避免后续插入撞号
SET @mx := (SELECT IFNULL(MAX(id),0) + 1 FROM tb_user);
SET @sql := CONCAT('ALTER TABLE tb_user AUTO_INCREMENT = ', @mx);
PREPARE s1 FROM @sql; EXECUTE s1; DEALLOCATE PREPARE s1;

SET @mx := (SELECT IFNULL(MAX(id),0) + 1 FROM tb_shop);
SET @sql := CONCAT('ALTER TABLE tb_shop AUTO_INCREMENT = ', @mx);
PREPARE s2 FROM @sql; EXECUTE s2; DEALLOCATE PREPARE s2;

COMMIT;