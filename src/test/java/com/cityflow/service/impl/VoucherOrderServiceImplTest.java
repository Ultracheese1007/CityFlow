package com.cityflow.service.impl;

import com.cityflow.dto.Result;
import com.cityflow.dto.UserDTO;
import com.cityflow.entity.SeckillVoucher;
import com.cityflow.entity.VoucherOrder;
import com.cityflow.repository.SeckillVoucherRepository;
import com.cityflow.repository.VoucherOrderRepository;
import com.cityflow.utils.RedisIdWorker;
import com.cityflow.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * VoucherOrderServiceImpl 单元测试
 *
 * 覆盖：
 *   seckillVoucher：4 个早期失败分支（券不存在/未开始/已结束/库存不足）
 *   createVoucherOrder：3 个分支（重复购买/扣库存失败/成功创建订单）
 *
 * 不覆盖：seckillVoucher 中 `new SimpleRedisLock` 与 `AopContext.currentProxy`
 * 之后的代码——这两处依赖运行时框架，留给集成测试。
 */
@ExtendWith(MockitoExtension.class)
class VoucherOrderServiceImplTest {

    @Mock
    private VoucherOrderRepository voucherOrderRepository;

    @Mock
    private SeckillVoucherRepository seckillVoucherRepository;

    @Mock
    private RedisIdWorker redisIdWorker;

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @InjectMocks
    private VoucherOrderServiceImpl voucherOrderService;

    @BeforeEach
    void setUp() {
        // createVoucherOrder 用 UserHolder 拿 userId，需要在 ThreadLocal 里塞一个用户
        UserDTO user = new UserDTO();
        user.setId(1010L);
        user.setNickName("user_9999");
        UserHolder.saveUser(user);
    }

    @AfterEach
    void tearDown() {
        // ThreadLocal 必须清——否则下一个测试可能拿到上一个测试的用户
        UserHolder.removeUser();
    }

    // ============ seckillVoucher 早期失败分支 ============

    @Test
    @DisplayName("seckillVoucher：券不存在时返回 fail")
    void seckillVoucher_voucherNotFound_returnsFail() {
        when(seckillVoucherRepository.findById(99L)).thenReturn(Optional.empty());

        Result result = voucherOrderService.seckillVoucher(99L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("秒杀券不存在");
        // 早期 return：库存绝不能被扣
        verify(seckillVoucherRepository, never()).decreaseStock(anyLong());
    }

    @Test
    @DisplayName("seckillVoucher：未到开始时间时返回 fail")
    void seckillVoucher_notStartedYet_returnsFail() {
        SeckillVoucher voucher = makeVoucher(
                LocalDateTime.now().plusHours(1),    // 1 小时后才开始
                LocalDateTime.now().plusHours(2),
                100
        );
        when(seckillVoucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        Result result = voucherOrderService.seckillVoucher(1L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("秒杀券尚未开始");
        verify(seckillVoucherRepository, never()).decreaseStock(anyLong());
    }

    @Test
    @DisplayName("seckillVoucher：已过结束时间时返回 fail")
    void seckillVoucher_alreadyEnded_returnsFail() {
        SeckillVoucher voucher = makeVoucher(
                LocalDateTime.now().minusHours(2),
                LocalDateTime.now().minusHours(1),    // 1 小时前已结束
                100
        );
        when(seckillVoucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        Result result = voucherOrderService.seckillVoucher(1L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("秒杀券已结束");
        verify(seckillVoucherRepository, never()).decreaseStock(anyLong());
    }

    @Test
    @DisplayName("seckillVoucher：库存为 0 时返回 fail")
    void seckillVoucher_outOfStock_returnsFail() {
        SeckillVoucher voucher = makeVoucher(
                LocalDateTime.now().minusHours(1),
                LocalDateTime.now().plusHours(1),
                0    // 已经卖光
        );
        when(seckillVoucherRepository.findById(1L)).thenReturn(Optional.of(voucher));

        Result result = voucherOrderService.seckillVoucher(1L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("库存不足！");
        verify(seckillVoucherRepository, never()).decreaseStock(anyLong());
    }

    // ============ createVoucherOrder 内部分支 ============

    @Test
    @DisplayName("createVoucherOrder：用户已购买过时返回 fail，不扣库存")
    void createVoucherOrder_alreadyBought_returnsFail() {
        when(voucherOrderRepository.existsByUserIdAndVoucherId(1010L, 1L)).thenReturn(true);

        Result result = voucherOrderService.createVoucherOrder(1L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("用户已经购买过一次！");
        // 重复购买检查通过前，绝不能扣库存或写订单
        verify(seckillVoucherRepository, never()).decreaseStock(anyLong());
        verify(voucherOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createVoucherOrder：原子扣库存失败（并发卖光）时返回 fail，不写订单")
    void createVoucherOrder_decreaseStockFails_returnsFail() {
        when(voucherOrderRepository.existsByUserIdAndVoucherId(1010L, 1L)).thenReturn(false);
        // decreaseStock 返回 0 表示 UPDATE...WHERE stock>0 没影响任何行
        when(seckillVoucherRepository.decreaseStock(1L)).thenReturn(0);

        Result result = voucherOrderService.createVoucherOrder(1L);

        assertThat(result.getSuccess()).isFalse();
        assertThat(result.getErrorMsg()).isEqualTo("库存不足！");
        // 库存扣失败：订单一定不能落库
        verify(voucherOrderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createVoucherOrder：happy path 返回订单 id 并落库")
    void createVoucherOrder_happyPath_savesOrderAndReturnsId() {
        when(voucherOrderRepository.existsByUserIdAndVoucherId(1010L, 1L)).thenReturn(false);
        when(seckillVoucherRepository.decreaseStock(1L)).thenReturn(1);
        when(redisIdWorker.nextId("order")).thenReturn(589237287871578113L);

        Result result = voucherOrderService.createVoucherOrder(1L);

        assertThat(result.getSuccess()).isTrue();
        assertThat(result.getData()).isEqualTo(589237287871578113L);
        // 验证 save 被调一次，且订单字段正确组装
        verify(voucherOrderRepository, times(1)).save(argThat(order -> {
            VoucherOrder o = (VoucherOrder) order;
            return o.getId().equals(589237287871578113L)
                    && o.getUserId().equals(1010L)
                    && o.getVoucherId().equals(1L)
                    && o.getStatus().equals(1)
                    && o.getPayType().equals(1);
        }));
    }

    // ============ 测试夹具 ============

    private SeckillVoucher makeVoucher(LocalDateTime begin, LocalDateTime end, Integer stock) {
        SeckillVoucher v = new SeckillVoucher();
        v.setBeginTime(begin);
        v.setEndTime(end);
        v.setStock(stock);
        return v;
    }
}
