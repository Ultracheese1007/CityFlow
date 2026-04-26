package com.cityflow.service;

import com.cityflow.entity.UserInfo;

public interface UserInfoService {
    UserInfo getInfoForDisplay(Long userId);
}