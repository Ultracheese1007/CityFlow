package com.hmdp.entity;

import javax.persistence.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Table(
        name = "tb_user",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_user_phone", columnNames = "phone")
        }
)
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 手机号码（唯一） */
    @Column(nullable = false, length = 20)
    private String phone;

    /** 密码（加密存储，不对外序列化） */
    @JsonIgnore
    @Column(nullable = false, length = 255)
    private String password;

    /** 昵称（默认随机字符） */
    @Column(name = "nick_name")
    private String nickName;

    /** 用户头像 */
    @Column(length = 512)
    private String icon = "";

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}