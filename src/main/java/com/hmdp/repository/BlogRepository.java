package com.hmdp.repository;

import com.hmdp.entity.Blog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BlogRepository extends JpaRepository<Blog, Long> {

    Page<Blog> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    @Query("select b from Blog b order by b.liked desc")
    Page<Blog> findHot(Pageable pageable);

    @Modifying
    @Query("update Blog b set b.liked = b.liked + 1 where b.id = :id")
    int incrementLikedById(Long id);
}
