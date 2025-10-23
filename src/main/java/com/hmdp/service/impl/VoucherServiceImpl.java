package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.dto.VoucherView;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.repository.VoucherRepository;
import com.hmdp.repository.SeckillVoucherRepository;
import com.hmdp.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Voucher 服务实现类（JPA 风格）
 */
@Service
@RequiredArgsConstructor
public class VoucherServiceImpl implements VoucherService {

    private final VoucherRepository voucherRepository;
    private final SeckillVoucherRepository seckillVoucherRepository;

    @Override
    public Result queryVoucherOfShop(Long shopId) {
        List<VoucherView> vouchers = voucherRepository.queryVoucherOfShop(shopId);
        return Result.ok(vouchers);
    }

    @Override
    @Transactional
    /**
     * 不要写：sv.setVoucherId(saved.getId())
     * Hibernate 期望从 voucher 对象获取主键（因为用了 @MapsId）
     * 从关联对象传递，根据 @MapsId 自动同步 voucher.id 到 voucher_id 列
     * */
    public void addSeckillVoucher(Voucher voucherInput) {
        // 1) 先保存主表，拿到托管实体（含自增 id）
        Voucher voucher = voucherRepository.save(voucherInput);

        // 2) 再保存从表：必须设置对象关联，由 @MapsId 传递主键
        SeckillVoucher sv = new SeckillVoucher();
        sv.setVoucher(voucher);           // ★ 核心：不要 setVoucherId
        sv.setStock(voucherInput.getStock());
        sv.setBeginTime(voucherInput.getBeginTime());
        sv.setEndTime(voucherInput.getEndTime());

        seckillVoucherRepository.save(sv);
    }


    @Override
    @Transactional
    public Long addVoucher(Voucher voucher) {
        Voucher saved = voucherRepository.save(voucher);
        return saved.getId();
    }

}
