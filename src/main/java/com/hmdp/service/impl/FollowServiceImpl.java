package com.hmdp.service.impl;

import com.hmdp.repository.FollowRepository;
import com.hmdp.service.FollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Follow 服务实现类
 *
 * 只保留骨架，后续可实现业务逻辑
 */
@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;

    // 未来在这里实现业务逻辑
}