package com.hmdp.service;

import com.hmdp.entity.Blog;

import java.util.List;

public interface BlogService {
    Long saveBlog(Blog blog);
    boolean likeBlog(Long id);
    List<Blog> queryMyBlog(Integer current);
    List<Blog> queryHotBlog(Integer current);
}