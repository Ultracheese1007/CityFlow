package com.hmdp.service.impl;

import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.repository.BlogRepository;
import com.hmdp.repository.UserRepository;
import com.hmdp.service.BlogService;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BlogServiceImpl implements BlogService {

    private final BlogRepository blogRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public Long saveBlog(Blog blog) {
        // 获取当前登录用户并设置作者
        UserDTO user = UserHolder.getUser();
        blog.setUserId(user.getId());
        Blog saved = blogRepository.save(blog);
        return saved.getId();
    }

    @Override
    @Transactional
    public boolean likeBlog(Long id) {
        // 点赞 +1（原子更新）
        int rows = blogRepository.incrementLikedById(id);
        return rows > 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> queryMyBlog(Integer current) {
        UserDTO user = UserHolder.getUser();
        int pageIndex = Math.max(0, (current == null ? 1 : current) - 1);
        Pageable pageable = PageRequest.of(
                pageIndex,
                SystemConstants.MAX_PAGE_SIZE,
                Sort.by(Sort.Direction.DESC, "createTime")
        );
        Page<Blog> page = blogRepository.findByUserIdOrderByCreateTimeDesc(user.getId(), pageable);
        return page.getContent();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Blog> queryHotBlog(Integer current) {
        int pageIndex = Math.max(0, (current == null ? 1 : current) - 1);
        Pageable pageable = PageRequest.of(pageIndex, SystemConstants.MAX_PAGE_SIZE);
        Page<Blog> page = blogRepository.findHot(pageable);
        List<Blog> records = page.getContent();

        // 回填用户展示信息（沿用你原来的 transient 字段逻辑）
        records.forEach(blog -> {
            Long userId = blog.getUserId();
            userRepository.findById(userId).ifPresent((User u) -> {
                blog.setName(u.getNickName());
                blog.setIcon(u.getIcon());
            });
        });
        return records;
    }
}