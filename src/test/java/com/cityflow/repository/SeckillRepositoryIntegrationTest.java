package com.cityflow.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 秒杀核心 Repository 的集成测试（H2 内存数据库）。
 *
 * 设计说明：
 *   SeckillVoucher entity 把主键和 Voucher 关联耦合在同一列上
 *   （@Id voucherId + @JoinColumn voucher_id），用 entityManager.persist()
 *   会触发 Hibernate "从 voucher 派生 id" 的逻辑，因此这里改用 JdbcTemplate
 *   直接 INSERT，绕过 entity 映射怪圈，专注测真正关心的 SQL 行为。
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@ActiveProfiles("test")
class SeckillRepositoryIntegrationTest {

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SeckillVoucherRepository seckillVoucherRepository;

    @Autowired
    private VoucherOrderRepository voucherOrderRepository;

    @Test
    @DisplayName("decreaseStock：stock>0 时返回 1 并真扣 1")
    void decreaseStock_withStockAvailable_decrementsAndReturnsOne() {
        // 直接用 SQL 插入，跳过 SeckillVoucher entity 的 mapping 怪圈
        jdbc.update("INSERT INTO tb_seckill_voucher (voucher_id, stock) VALUES (?, ?)", 100L, 10);

        int affected = seckillVoucherRepository.decreaseStock(100L);

        assertThat(affected).isEqualTo(1);
        Integer remaining = jdbc.queryForObject(
                "SELECT stock FROM tb_seckill_voucher WHERE voucher_id = ?",
                Integer.class, 100L);
        assertThat(remaining).isEqualTo(9);
    }

    @Test
    @DisplayName("decreaseStock：stock=0 时返回 0 不扣（防超卖契约）")
    void decreaseStock_whenStockIsZero_returnsZeroAndNoDecrement() {
        jdbc.update("INSERT INTO tb_seckill_voucher (voucher_id, stock) VALUES (?, ?)", 101L, 0);

        int affected = seckillVoucherRepository.decreaseStock(101L);

        // 关键契约：UPDATE...WHERE stock>0 在 stock=0 时影响 0 行
        assertThat(affected).isEqualTo(0);
        Integer remaining = jdbc.queryForObject(
                "SELECT stock FROM tb_seckill_voucher WHERE voucher_id = ?",
                Integer.class, 101L);
        assertThat(remaining).isEqualTo(0);    // 仍然 0，没被扣到 -1
    }

    @Test
    @DisplayName("existsByUserIdAndVoucherId：有订单返回 true，没订单返回 false")
    void existsByUserIdAndVoucherId_returnsCorrectBoolean() {
        // 直接 SQL 插入，绕开任何关联约束
        jdbc.update(
                "INSERT INTO tb_voucher_order (id, user_id, voucher_id, status, pay_type) VALUES (?, ?, ?, ?, ?)",
                999_999_999L, 1010L, 200L, 1, 1);

        // 1010 买过 200 → true
        assertThat(voucherOrderRepository.existsByUserIdAndVoucherId(1010L, 200L)).isTrue();
        // 1010 没买过 999 → false
        assertThat(voucherOrderRepository.existsByUserIdAndVoucherId(1010L, 999L)).isFalse();
        // 别人没买过 200 → false
        assertThat(voucherOrderRepository.existsByUserIdAndVoucherId(2020L, 200L)).isFalse();
    }
}
