-- 测试用极简 schema：只建秒杀相关的两张表，无 FK，无外部依赖
DROP TABLE IF EXISTS tb_seckill_voucher;
DROP TABLE IF EXISTS tb_voucher_order;

CREATE TABLE tb_seckill_voucher (
    voucher_id   BIGINT NOT NULL PRIMARY KEY,
    stock        INT NOT NULL,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    begin_time   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    end_time     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tb_voucher_order (
    id           BIGINT NOT NULL PRIMARY KEY,
    user_id      BIGINT NOT NULL,
    voucher_id   BIGINT NOT NULL,
    pay_type     TINYINT NOT NULL DEFAULT 1,
    status       TINYINT NOT NULL DEFAULT 1,
    create_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
