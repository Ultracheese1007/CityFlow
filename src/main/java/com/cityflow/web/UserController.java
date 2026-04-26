package com.cityflow.web;


import com.cityflow.dto.LoginFormDTO;
import com.cityflow.dto.Result;
import com.cityflow.entity.UserInfo;
import com.cityflow.repository.UserInfoRepository;
import com.cityflow.repository.UserRepository;
import com.cityflow.service.UserInfoService;
import com.cityflow.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;


@Slf4j
@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final UserInfoService userInfoService;

    /**
     * 发送手机验证码
     */
    @PostMapping("code")
    public Result sendCode(@RequestParam("phone") String phone, HttpSession session) {
        return userService.sendCode(phone, session);
    }

    /**
     * 登录功能
     * @param loginForm 登录参数，包含手机号、验证码；或者手机号、密码
     */
    @PostMapping("/login")
    public Result login(@RequestBody LoginFormDTO loginForm, HttpSession session){
        return userService.login(loginForm, session);
    }

    /**
     * 登出功能
     * @return 无
     */
    @PostMapping("/logout")
    public Result logout(){
        // TODO 实现登出功能
        return Result.fail("功能未完成");
    }

    @GetMapping("/me")
    public Result me(){
        // TODO 获取当前登录的用户并返回
        return Result.fail("功能未完成");
    }

    @GetMapping("/info/{id}")
    public Result info(@PathVariable("id") Long userId){
        UserInfo info = userInfoService.getInfoForDisplay(userId);
        if (info == null) {
            // 没有详情，应该是第一次查看详情
            return Result.ok();
        }
        return Result.ok(info);
    }
}