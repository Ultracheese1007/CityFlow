-- 视图：商铺 + 类型
CREATE OR REPLACE VIEW v_shop_with_type AS
SELECT s.id, s.`name`, s.type_id, t.`name` AS type_name,
       s.images, s.area, s.address, s.x, s.y,
       s.avg_price, s.sold, s.comments, s.score, s.open_hours,
       s.create_time, s.update_time
FROM tb_shop s
         LEFT JOIN tb_shop_type t ON t.id = s.type_id;

-- 视图：优惠券 + 秒杀信息
CREATE OR REPLACE VIEW v_voucher_with_seckill AS
SELECT v.id AS voucher_id, v.shop_id, v.`title`, v.`sub_title`, v.`rules`,
       sv.stock, sv.begin_time, sv.end_time,
       v.create_time, GREATEST(v.update_time, sv.update_time) AS update_time
FROM tb_voucher v
         LEFT JOIN tb_seckill_voucher sv ON sv.voucher_id = v.id;

-- 视图：博客 + 作者
CREATE OR REPLACE VIEW v_blog_with_author AS
SELECT b.id AS blog_id, b.shop_id, b.user_id,
       u.nick_name AS author_name, u.icon AS author_icon,
       b.`title`, b.`images`, b.`content`, b.liked, b.comments,
       b.create_time, b.update_time
FROM tb_blog b
         LEFT JOIN tb_user u ON u.id = b.user_id;

-- 视图：用户档案汇总
CREATE OR REPLACE VIEW v_user_profile AS
SELECT u.id AS user_id, u.phone, u.nick_name, u.icon,
       ui.city, ui.introduce, ui.fans, ui.followee, ui.gender, ui.birthday,
       ui.credits, ui.`level`,
       LEAST(u.create_time, ui.create_time) AS create_time,
       GREATEST(u.update_time, ui.update_time) AS update_time
FROM tb_user u
         LEFT JOIN tb_user_info ui ON ui.user_id = u.id;
