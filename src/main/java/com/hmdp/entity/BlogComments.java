package com.hmdp.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = false)
@Accessors(chain = true)
@Entity
@Table(name = "tb_blog_comments")
public class BlogComments implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 主键 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户id */
    @Column(name = "user_id")
    private Long userId;

    /** 探店id */
    @Column(name = "blog_id")
    private Long blogId;

    // BlogComments -> Blog
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blog_id", insertable = false, updatable = false)
    private Blog blog;

    // Blog -> User(作者)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", insertable = false, updatable = false)
    private User author;

    /** 关联的1级评论id，如果是一级评论，则值为0 */
    @Column(name = "parent_id")
    private Long parentId;

    /** 回复的评论id */
    @Column(name = "answer_id")
    private Long answerId;

    /** 回复的内容 */
    private String content;

    /** 点赞数 */
    private Integer liked;

    /** 状态，0：正常，1：被举报，2：禁止查看 */
    private Boolean status;

    /** 创建时间 */
    @CreationTimestamp
    @Column(name = "create_time")
    private LocalDateTime createTime;

    /** 更新时间 */
    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;
}