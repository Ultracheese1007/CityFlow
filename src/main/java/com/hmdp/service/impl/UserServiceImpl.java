package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.hmdp.config.security.jwt.CookieUtil;
import com.hmdp.config.security.jwt.JwtProps;
import com.hmdp.config.security.jwt.JwtUtil;
import com.hmdp.dto.LoginFormDTO;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.User;
import com.hmdp.repository.UserRepository;
import com.hmdp.service.UserService;
import com.hmdp.utils.RegexUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;


import static com.hmdp.utils.RedisConstants.LOGIN_CODE_KEY;
import static com.hmdp.utils.RedisConstants.LOGIN_CODE_TTL;
import static com.hmdp.utils.SystemConstants.USER_NICK_NAME_PREFIX;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProps jwtProps;
    private final UserRepository userRepository;

    @Override
    public Result sendCode(String phone, HttpSession session) {
        if (RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机号格式错误！");
        String code = String.valueOf((int)((Math.random()*9 + 1) * 100000));
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);
        log.debug("发送短信验证码成功，验证码：{}", code);
        return Result.ok();
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {
        String phone = loginForm.getPhone();
        if (RegexUtils.isPhoneInvalid(phone)) return Result.fail("手机号格式错误！");

        String cacheCode = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);
        if (cacheCode == null || !cacheCode.equals(loginForm.getCode())) return Result.fail("验证码错误");

        // JPA 查询/创建
        User user = userRepository.findByPhone(phone).orElseGet(() -> createUserWithPhone(phone));

        // 签 Access
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        Map<String,Object> claims = new HashMap<>();
        claims.put("nickname", userDTO.getNickName());
        String access = JwtUtil.genAccess(
                jwtProps.getSecret(),
                jwtProps.getAccessTtlMinutes(),
                String.valueOf(userDTO.getId()),
                claims
        );

        // 签 Refresh 并写入 HttpOnly Cookie —— 通过 RequestContextHolder 拿 response
        HttpServletResponse response = ((ServletRequestAttributes)
                RequestContextHolder.currentRequestAttributes()).getResponse();
        if (response != null) {
            String refresh = JwtUtil.genRefresh(
                    jwtProps.getSecret(),
                    jwtProps.getRefreshTtlDays(),
                    String.valueOf(userDTO.getId())
            );
            CookieUtil.addHttpOnlyCookie(
                    response,
                    jwtProps.getRefreshCookieName(),
                    refresh,
                    (int) (jwtProps.getRefreshTtlDays() * 24 * 60 * 60),
                    jwtProps.getCookieDomain(),
                    jwtProps.isCookieSecure(),
                    jwtProps.getCookieSameSite()
            );
        } else {
            log.warn("HttpServletResponse is null when setting refresh cookie.");
        }

        // 返回体保持原样：data = access token 字符串
        return Result.ok(access);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + phone.substring(phone.length() - 4));
        return userRepository.save(user);
    }
}