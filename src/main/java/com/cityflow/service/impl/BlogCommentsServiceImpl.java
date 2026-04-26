package com.cityflow.service.impl;

import com.cityflow.repository.BlogCommentsRepository;
import com.cityflow.service.BlogCommentsService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * BlogComments 服务实现类
 *
 * 只保留骨架，后续可实现业务逻辑
 */
@Service
@RequiredArgsConstructor
public class BlogCommentsServiceImpl implements BlogCommentsService {

    private final BlogCommentsRepository blogCommentsRepository;

    // 未来在这里实现业务逻辑
}