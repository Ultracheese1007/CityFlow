package com.hmdp.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "tb_voucher_order")
public class VoucherOrder implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键（如果是手动生成ID，不要加 @GeneratedValue） */
    @Id
    private Long id;

    /** 下单的用户id */
    @Column(name = "user_id")
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User user;

    /** 购买的代金券id */
    @Column(name = "voucher_id")
    private Long voucherId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "voucher_id", insertable = false, updatable = false)
    private Voucher voucher;

    /** 支付方式 1：余额支付；2：支付宝；3：微信 */
    @Column(name = "pay_type")
    private Integer payType;

    /** 订单状态 */
    private Integer status;

    /** 下单时间 */
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 支付时间 */
    @CreationTimestamp
    @Column(name = "pay_time")
    private LocalDateTime payTime;

    /** 核销时间 */
    @Column(name = "use_time")
    private LocalDateTime useTime;

    /** 退款时间 */
    @Column(name = "refund_time")
    private LocalDateTime refundTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        if (createTime == null) createTime = LocalDateTime.now();
        if (updateTime == null) updateTime = LocalDateTime.now();
    }
}