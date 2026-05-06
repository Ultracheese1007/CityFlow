-- =====================================================================
-- V3__stage_load.sql
-- 把四个 TSV（user / shop / voucher / blog）装进 stage_* 缓冲表。
--
-- 工程化要点：
--   1) stage 用宽松类型（VARCHAR / NULL 允许），先求"装得进"。
--   2) 每次执行先 TRUNCATE，保证可重入。
--   3) 用"显式列 + 用户变量 + NULLIF"模式，把空字符串落成 NULL，
--      避免列错位 / 把空串误塞进数值列。
--   4) ${dataDir} 是 Flyway 占位符，由
--        spring.flyway.placeholders.dataDir
--      （或环境变量 SPRING_FLYWAY_PLACEHOLDERS_DATADIR）注入；
--      容器里挂载到 /app/data。
--   5) LOAD DATA LOCAL INFILE 是客户端读取，
--      JDBC URL 必须含 allowLoadLocalInfile=true，
--      MySQL 服务端必须 local_infile=ON。
-- =====================================================================

SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 1) stage 表 ----------

CREATE TABLE IF NOT EXISTS stage_user (
    id          BIGINT UNSIGNED,
    phone       VARCHAR(11),
    password    VARCHAR(128),
    nick_name   VARCHAR(32),
    icon        VARCHAR(255),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS stage_shop (
    id          BIGINT UNSIGNED,
    name        VARCHAR(128),
    type_id     BIGINT UNSIGNED,
    images      VARCHAR(2048),
    area        VARCHAR(128),
    address     VARCHAR(255),
    x           DOUBLE,
    y           DOUBLE,
    avg_price   BIGINT UNSIGNED,
    sold        INT UNSIGNED,
    comments    INT UNSIGNED,
    score       INT UNSIGNED,
    open_hours  VARCHAR(32),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS stage_voucher (
    id           BIGINT UNSIGNED,
    shop_id      BIGINT UNSIGNED,
    title        VARCHAR(255),
    sub_title    VARCHAR(255),
    rules        VARCHAR(1024),
    pay_value    BIGINT UNSIGNED,
    actual_value BIGINT UNSIGNED,
    type         TINYINT UNSIGNED,
    status       TINYINT UNSIGNED,
    create_time  TIMESTAMP NULL,
    update_time  TIMESTAMP NULL
);

CREATE TABLE IF NOT EXISTS stage_blog (
    id          BIGINT UNSIGNED,
    shop_id     BIGINT UNSIGNED,
    user_id     BIGINT UNSIGNED,
    title       VARCHAR(255),
    images      VARCHAR(2048),
    content     VARCHAR(2048),
    liked       INT UNSIGNED,
    comments    INT UNSIGNED,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
);

-- ---------- 2) 每次装载前清空 stage（保证重入幂等） ----------

TRUNCATE stage_user;
TRUNCATE stage_shop;
TRUNCATE stage_voucher;
TRUNCATE stage_blog;

-- ---------- 3) LOAD DATA LOCAL INFILE ----------
-- 模式：(@col1, @col2, ...) → SET col = NULLIF(@colN, '')
-- 这样 TSV 里的空字符串都会变成 NULL；如果列错位，落到数值列也会变成 NULL
-- 而不是抛 "Incorrect integer value" 终止。

LOAD DATA LOCAL INFILE '${dataDir}/tb_user.tsv'
INTO TABLE stage_user
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, @phone, @password, @nick_name, @icon, @create_time, @update_time)
SET
    id          = NULLIF(@id, ''),
    phone       = NULLIF(@phone, ''),
    password    = IFNULL(@password, ''),
    nick_name   = IFNULL(@nick_name, ''),
    icon        = IFNULL(@icon, ''),
    create_time = NULLIF(@create_time, ''),
    update_time = NULLIF(@update_time, '');

LOAD DATA LOCAL INFILE '${dataDir}/tb_shop.tsv'
INTO TABLE stage_shop
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, @name, @type_id, @images, @area, @address,
 @x, @y, @avg_price, @sold, @comments, @score, @open_hours,
 @create_time, @update_time)
SET
    id          = NULLIF(@id, ''),
    name        = NULLIF(@name, ''),
    type_id     = NULLIF(@type_id, ''),
    images      = IFNULL(@images, ''),
    area        = NULLIF(@area, ''),
    address     = IFNULL(@address, ''),
    x           = NULLIF(@x, ''),
    y           = NULLIF(@y, ''),
    avg_price   = NULLIF(@avg_price, ''),
    sold        = NULLIF(@sold, ''),
    comments    = NULLIF(@comments, ''),
    score       = NULLIF(@score, ''),
    open_hours  = NULLIF(@open_hours, ''),
    create_time = NULLIF(@create_time, ''),
    update_time = NULLIF(@update_time, '');

LOAD DATA LOCAL INFILE '${dataDir}/tb_voucher.tsv'
INTO TABLE stage_voucher
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, @shop_id, @title, @sub_title, @rules,
 @pay_value, @actual_value, @type, @status,
 @create_time, @update_time)
SET
    id           = NULLIF(@id, ''),
    shop_id      = NULLIF(@shop_id, ''),
    title        = NULLIF(@title, ''),
    sub_title    = NULLIF(@sub_title, ''),
    rules        = NULLIF(@rules, ''),
    pay_value    = NULLIF(@pay_value, ''),
    actual_value = NULLIF(@actual_value, ''),
    type         = NULLIF(@type, ''),
    status       = NULLIF(@status, ''),
    create_time  = NULLIF(@create_time, ''),
    update_time  = NULLIF(@update_time, '');

LOAD DATA LOCAL INFILE '${dataDir}/tb_blog.tsv'
INTO TABLE stage_blog
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id, @shop_id, @user_id, @title, @images, @content,
 @liked, @comments, @create_time, @update_time)
SET
    id          = NULLIF(@id, ''),
    shop_id     = NULLIF(@shop_id, ''),
    user_id     = NULLIF(@user_id, ''),
    title       = NULLIF(@title, ''),
    images      = IFNULL(@images, ''),
    content     = IFNULL(@content, ''),
    liked       = NULLIF(@liked, ''),
    comments    = NULLIF(@comments, ''),
    create_time = NULLIF(@create_time, ''),
    update_time = NULLIF(@update_time, '');

SET FOREIGN_KEY_CHECKS = 1;
