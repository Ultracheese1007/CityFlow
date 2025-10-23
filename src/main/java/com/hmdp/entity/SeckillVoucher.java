package com.hmdp.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 秒杀优惠券表，与优惠券是一对一关系
 */
@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "tb_seckill_voucher")
public class SeckillVoucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 关联的优惠券id（主键，同时也是外键） */
    @Id
    @Column(name = "voucher_id")
    private Long voucherId;

    // SeckillVoucher
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "voucher_id")
    private Voucher voucher;

    /** 库存 */
    private Integer stock;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 生效时间 */
    @Column(name = "begin_time")
    private LocalDateTime beginTime;

    /** 失效时间 */
    @Column(name = "end_time")
    private LocalDateTime endTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

}