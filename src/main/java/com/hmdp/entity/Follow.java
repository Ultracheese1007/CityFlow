package com.hmdp.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "tb_follow")
public class Follow implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // MySQL 自增
    private Long id;

    /** 用户id */
    @Column(name = "user_id")
    private Long userId;

    /** 关联的用户id */
    @Column(name = "follow_user_id")
    private Long followUserId;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;
}