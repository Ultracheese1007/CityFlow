package com.hmdp.entity;

import javax.persistence.*;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "tb_user_info")
public class UserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键，对应用户id */
    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    /** 城市名称 */
    private String city;

    /** 个人介绍，不超过128个字符 */
    @Column(length = 128)
    private String introduce;

    /** 粉丝数量 */
    private Integer fans;

    /** 关注的人数量 */
    private Integer followee;

    /** 性别，0：男，1：女 */
    private Boolean gender;

    /** 生日 */
    private LocalDate birthday;

    /** 积分 */
    private Integer credits;

    /** 会员级别，0~9级 */
    private Integer level;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}