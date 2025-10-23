package com.hmdp.service;

import com.hmdp.entity.UserInfo;

public interface UserInfoService {
    UserInfo getInfoForDisplay(Long userId);
}