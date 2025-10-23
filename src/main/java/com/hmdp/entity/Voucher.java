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
@Table(name = "tb_voucher")
public class Voucher implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 商铺id */
    @Column(name = "shop_id")
    private Long shopId;

    /** 代金券标题 */
    private String title;

    /** 副标题 */
    @Column(name = "sub_title")
    private String subTitle;

    /** 使用规则 */
    private String rules;

    /** 支付金额 */
    @Column(name = "pay_value")
    private Long payValue;

    /** 抵扣金额 */
    @Column(name = "actual_value")
    private Long actualValue;

    /** 优惠券类型 */
    private Integer type;

    /** 状态 */
    private Integer status;

    /** 库存 */
    @Transient
    private Integer stock;

    /** 生效时间 */
    @Transient
    private LocalDateTime beginTime;

    /** 失效时间 */
    @Transient
    private LocalDateTime endTime;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}