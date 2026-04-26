package com.cityflow.service.impl;

import com.cityflow.entity.UserInfo;
import com.cityflow.repository.UserInfoRepository;
import com.cityflow.service.UserInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserInfoServiceImpl implements UserInfoService {

    private final UserInfoRepository userInfoRepository;

    @Override
    @Transactional(readOnly = true)
    public UserInfo getInfoForDisplay(Long userId) {
        UserInfo info = userInfoRepository.findById(userId).orElse(null);
        if (info == null) return null;
        // 保持原控制器逻辑：对外不暴露时间字段
        info.setCreateTime(null);
        info.setUpdateTime(null);
        return info;
    }
}