package com.hmdp.repository;

import com.hmdp.entity.BlogComments;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BlogCommentsRepository extends JpaRepository<BlogComments, Long> {
}