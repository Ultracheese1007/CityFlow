package com.hmdp.web;

import com.hmdp.dto.Result;
import com.hmdp.entity.Blog;
import com.hmdp.service.BlogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/blog")
@RequiredArgsConstructor
public class BlogController {

    private final BlogService blogService;

    @PostMapping
    public Result saveBlog(@RequestBody Blog blog) {
        Long id = blogService.saveBlog(blog);
        return Result.ok(id);
    }

    @PutMapping("/like/{id}")
    public Result likeBlog(@PathVariable("id") Long id) {
        return blogService.likeBlog(id) ? Result.ok() : Result.fail("Blog not found");
    }

    @GetMapping("/of/me")
    public Result queryMyBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryMyBlog(current));
    }

    @GetMapping("/hot")
    public Result queryHotBlog(@RequestParam(value = "current", defaultValue = "1") Integer current) {
        return Result.ok(blogService.queryHotBlog(current));
    }
}