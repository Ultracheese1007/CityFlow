-- =====================================================================
-- V1__init_schema.sql
-- 创建 CityFlow 全部业务表（仅表结构 + 基础索引；唯一约束放到 V2）。
-- 字符集统一 utf8mb4 / utf8mb4_0900_ai_ci。
-- =====================================================================

CREATE TABLE `tb_blog` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shop_id`     BIGINT UNSIGNED NOT NULL COMMENT '商户 id',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
    `title`       VARCHAR(255)    NOT NULL COMMENT '标题',
    `images`      VARCHAR(2048)   NOT NULL COMMENT '探店照片，多张以逗号分隔',
    `content`     VARCHAR(2048)   NOT NULL COMMENT '探店文字描述',
    `liked`       INT UNSIGNED    DEFAULT 0    COMMENT '点赞数量',
    `comments`    INT UNSIGNED    DEFAULT NULL COMMENT '评论数量',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_blog_comments` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
    `blog_id`     BIGINT UNSIGNED NOT NULL COMMENT '探店 id',
    `parent_id`   BIGINT UNSIGNED NOT NULL COMMENT '关联的 1 级评论 id；0 表示一级评论',
    `answer_id`   BIGINT UNSIGNED NOT NULL COMMENT '回复的评论 id',
    `content`     VARCHAR(255)    NOT NULL COMMENT '回复内容',
    `liked`       INT UNSIGNED    DEFAULT NULL,
    `status`      TINYINT UNSIGNED DEFAULT NULL COMMENT '0 正常 / 1 被举报 / 2 禁止查看',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_follow` (
    `id`             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`        BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
    `follow_user_id` BIGINT UNSIGNED NOT NULL COMMENT '被关注的用户 id',
    `create_time`    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_seckill_voucher` (
    `voucher_id`  BIGINT UNSIGNED NOT NULL COMMENT '关联的优惠券 id',
    `stock`       INT UNSIGNED    NOT NULL COMMENT '库存',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `begin_time`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '生效时间',
    `end_time`    TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '失效时间',
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`voucher_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci
  COMMENT = '秒杀优惠券表，与优惠券是一对一关系';

CREATE TABLE `tb_shop` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(128)    NOT NULL COMMENT '商铺名称',
    `type_id`     BIGINT UNSIGNED NOT NULL COMMENT '商铺类型的 id',
    `images`      VARCHAR(1024)   NOT NULL COMMENT '商铺图片，多张以逗号分隔',
    `area`        VARCHAR(128)    DEFAULT NULL COMMENT '商圈',
    `address`     VARCHAR(255)    NOT NULL,
    `x`           DOUBLE          NOT NULL COMMENT '经度',
    `y`           DOUBLE          NOT NULL COMMENT '纬度',
    `avg_price`   BIGINT UNSIGNED DEFAULT NULL COMMENT '均价（整数）',
    `sold`        INT UNSIGNED    NOT NULL COMMENT '销量',
    `comments`    INT UNSIGNED    NOT NULL COMMENT '评论数量',
    `score`       INT UNSIGNED    NOT NULL COMMENT '评分 1~5 分（×10 保存）',
    `open_hours`  VARCHAR(32)     DEFAULT NULL COMMENT '营业时间',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `foreign_key_type` (`type_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_shop_type` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `name`        VARCHAR(32)     DEFAULT NULL,
    `icon`        VARCHAR(255)    DEFAULT NULL,
    `sort`        INT UNSIGNED    DEFAULT NULL,
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_sign` (
    `id`        BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `user_id`   BIGINT UNSIGNED NOT NULL COMMENT '用户 id',
    `year`      YEAR            NOT NULL,
    `month`     TINYINT UNSIGNED NOT NULL,
    `date`      DATE            NOT NULL,
    `is_backup` TINYINT UNSIGNED DEFAULT NULL COMMENT '是否补签',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_user` (
    `id`          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `phone`       VARCHAR(11)     NOT NULL COMMENT '手机号',
    `password`    VARCHAR(128)    DEFAULT '' COMMENT '密码（加密存储）',
    `nick_name`   VARCHAR(32)     DEFAULT '' COMMENT '昵称',
    `icon`        VARCHAR(255)    DEFAULT '' COMMENT '头像',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`)
    -- phone 的唯一约束放在 V2，集中管理
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_user_info` (
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '主键，用户 id',
    `city`        VARCHAR(64)     DEFAULT '',
    `introduce`   VARCHAR(128)    DEFAULT NULL,
    `fans`        INT UNSIGNED    DEFAULT 0,
    `followee`    INT UNSIGNED    DEFAULT 0,
    `gender`      TINYINT UNSIGNED DEFAULT 0 COMMENT '0 男 / 1 女',
    `birthday`    DATE            DEFAULT NULL,
    `credits`     INT UNSIGNED    DEFAULT 0,
    `level`       TINYINT UNSIGNED DEFAULT 0 COMMENT '0~9 级；0 表示未开通会员',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_voucher` (
    `id`           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    `shop_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '商铺 id',
    `title`        VARCHAR(255)    NOT NULL COMMENT '代金券标题',
    `sub_title`    VARCHAR(255)    DEFAULT NULL,
    `rules`        VARCHAR(1024)   DEFAULT NULL,
    `pay_value`    BIGINT UNSIGNED NOT NULL COMMENT '支付金额，单位分',
    `actual_value` BIGINT UNSIGNED NOT NULL COMMENT '抵扣金额，单位分',
    `type`         TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT '0 普通券；1 秒杀券',
    `status`       TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 上架；2 下架；3 过期',
    `create_time`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `update_time`  TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_shop` (`shop_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE `tb_voucher_order` (
    `id`          BIGINT UNSIGNED NOT NULL COMMENT '主键（雪花/发号器生成，不自增）',
    `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '下单用户 id',
    `voucher_id`  BIGINT UNSIGNED NOT NULL COMMENT '代金券 id',
    `pay_type`    TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 余额；2 支付宝；3 微信',
    `status`      TINYINT UNSIGNED NOT NULL DEFAULT 1 COMMENT '1 未支付；2 已支付；3 已核销；4 已取消；5 退款中；6 已退款',
    `create_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '下单时间',
    `pay_time`    TIMESTAMP       NULL DEFAULT NULL COMMENT '支付时间',
    `use_time`    TIMESTAMP       NULL DEFAULT NULL COMMENT '核销时间',
    `refund_time` TIMESTAMP       NULL DEFAULT NULL COMMENT '退款时间',
    `update_time` TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    KEY `idx_user`    (`user_id`),
    KEY `idx_voucher` (`voucher_id`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
