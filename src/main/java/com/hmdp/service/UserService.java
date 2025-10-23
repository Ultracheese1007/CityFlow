package com.hmdp.service;

import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;

import javax.servlet.http.HttpSession;

/**
 * User 服务接口
 *
 * 只保留骨架，后续可添加业务方法
 */
public interface UserService {

    Result sendCode(String phone, HttpSession session);

    Result login(LoginFormDTO loginForm, HttpSession session);
}