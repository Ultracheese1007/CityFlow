-- 保障：本脚本可反复执行
SET FOREIGN_KEY_CHECKS = 0;

-- 1) 建 stage 表（字段类型尽量贴近正式表，也可先用宽松的 VARCHAR）
CREATE TABLE IF NOT EXISTS stage_user (
                                          id BIGINT UNSIGNED,
                                          phone VARCHAR(11),
    password VARCHAR(128),
    nick_name VARCHAR(32),
    icon VARCHAR(255),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
    );

CREATE TABLE IF NOT EXISTS stage_shop (
                                          id BIGINT UNSIGNED,
                                          name VARCHAR(128),
    type_id BIGINT UNSIGNED,
    images VARCHAR(1024),
    area VARCHAR(128),
    address VARCHAR(255),
    x DOUBLE UNSIGNED,
    y DOUBLE UNSIGNED,
    avg_price BIGINT UNSIGNED,
    sold INT UNSIGNED,
    comments INT UNSIGNED,
    score INT UNSIGNED,
    open_hours VARCHAR(32),
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
    );

CREATE TABLE IF NOT EXISTS stage_voucher (
                                             id BIGINT UNSIGNED,
                                             shop_id BIGINT UNSIGNED,
                                             title VARCHAR(255),
    sub_title VARCHAR(255),
    rules VARCHAR(1024),
    pay_value BIGINT UNSIGNED,
    actual_value BIGINT UNSIGNED,
    type TINYINT UNSIGNED,
    status TINYINT UNSIGNED,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
    );

CREATE TABLE IF NOT EXISTS stage_blog (
                                          id BIGINT UNSIGNED,
                                          shop_id BIGINT UNSIGNED,
                                          user_id BIGINT UNSIGNED,
                                          title VARCHAR(255),
    images VARCHAR(2048),
    content VARCHAR(2048),
    liked INT UNSIGNED,
    comments INT UNSIGNED,
    create_time TIMESTAMP NULL,
    update_time TIMESTAMP NULL
    );

-- 2) 每次装载前清空 stage
TRUNCATE stage_user;
TRUNCATE stage_shop;
TRUNCATE stage_voucher;
TRUNCATE stage_blog;

-- 3) 从 TSV 装载（使用 LOCAL，避免 secure_file_priv 限制）
-- 注意：Flyway 执行环境需允许 LOCAL INFILE。
LOAD DATA LOCAL INFILE '${dataDir}/tb_user.tsv'
INTO TABLE stage_user
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES
(@id,@phone,@password,@nick_name,@icon,@create_time,@update_time)
SET
  id = NULLIF(@id,''),
  phone = NULLIF(@phone,''),
  password = IFNULL(@password,''),
  nick_name = IFNULL(@nick_name,''),
  icon = IFNULL(@icon,''),
  create_time = NULLIF(@create_time,''),
  update_time = NULLIF(@update_time,'')
;

LOAD DATA LOCAL INFILE '${dataDir}/tb_shop.tsv'
INTO TABLE stage_shop
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE '${dataDir}/tb_voucher.tsv'
INTO TABLE stage_voucher
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

LOAD DATA LOCAL INFILE '${dataDir}/tb_blog.tsv'
INTO TABLE stage_blog
CHARACTER SET utf8mb4
FIELDS TERMINATED BY '\t'
LINES TERMINATED BY '\n'
IGNORE 1 LINES;

SET FOREIGN_KEY_CHECKS = 1;