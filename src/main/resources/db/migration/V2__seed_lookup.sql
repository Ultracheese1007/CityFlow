-- V2: lookup + constraints (minimal)
-- 仅处理字典表 tb_shop_type；演示数据（tb_shop 等）不要放在 V2

-- 1) 唯一性约束：类型名不允许重复
ALTER TABLE tb_shop_type
    MODIFY name varchar(32) NOT NULL,
    ADD UNIQUE KEY uk_shoptype_name (name);

ALTER TABLE tb_shop
    ADD UNIQUE KEY uk_shop_name_addr (name, address);

ALTER TABLE tb_voucher
    MODIFY title varchar(255) NOT NULL,
    ADD UNIQUE KEY uk_voucher_shop_title (shop_id, title);

ALTER TABLE tb_blog
    MODIFY title varchar(255) NOT NULL,
    ADD UNIQUE KEY uk_blog_title_user (title, user_id);

ALTER TABLE tb_follow
    ADD UNIQUE KEY uk_follow_pair (user_id, follow_user_id);

ALTER TABLE tb_sign
    ADD UNIQUE KEY uk_sign_user_day (user_id, `date`);

-- 2) 基础字典数据
INSERT INTO tb_shop_type (name, icon, sort, create_time, update_time)
VALUES
    ('美食',       '/types/ms.png',    1,  '2021-12-22 20:17:47', '2021-12-23 11:24:31'),
    ('KTV',        '/types/KTV.png',   2,  '2021-12-22 20:18:27', '2021-12-23 11:24:31'),
    ('丽人·美发',  '/types/lrmf.png',  3,  '2021-12-22 20:18:48', '2021-12-23 11:24:31'),
    ('健身运动',   '/types/jsyd.png', 10,  '2021-12-22 20:19:04', '2021-12-23 11:24:31'),
    ('按摩·足疗',  '/types/amzl.png',  5,  '2021-12-22 20:19:27', '2021-12-23 11:24:31'),
    ('美容SPA',    '/types/spa.png',   6,  '2021-12-22 20:19:35', '2021-12-23 11:24:31'),
    ('亲子游乐',   '/types/qzyl.png',  7,  '2021-12-22 20:19:53', '2021-12-23 11:24:31'),
    ('酒吧',       '/types/jiuba.png', 8,  '2021-12-22 20:20:02', '2021-12-23 11:24:31'),
    ('轰趴馆',     '/types/hpg.png',   9,  '2021-12-22 20:20:08', '2021-12-23 11:24:31'),
    ('美睫·美甲',  '/types/mjmj.png',  4,  '2021-12-22 20:21:46', '2021-12-23 11:24:31')
    ON DUPLICATE KEY UPDATE
                         icon        = VALUES(icon),
                         sort        = VALUES(sort),
                         create_time = VALUES(create_time),   -- 如已存在也回写为原始时间戳
                         update_time = VALUES(update_time);

-- 备注：
-- 后续导入 tb_shop 请不要依赖硬编码的 type_id=1/2，
-- 而是在合并阶段通过  JOIN tb_shop_type t ON t.name = stage.type_name  拿到 t.id