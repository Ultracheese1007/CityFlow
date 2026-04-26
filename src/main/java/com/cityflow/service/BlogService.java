package com.cityflow.service;

import com.cityflow.entity.Blog;

import java.util.List;

public interface BlogService {
    Long saveBlog(Blog blog);
    boolean likeBlog(Long id);
    List<Blog> queryMyBlog(Integer current);
    List<Blog> queryHotBlog(Integer current);
}