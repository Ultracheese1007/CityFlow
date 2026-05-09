package com.cityflow.service.impl;

import com.cityflow.config.security.jwt.JwtProps;
import com.cityflow.dto.ErrorCode;
import com.cityflow.dto.LoginFormDTO;
import com.cityflow.dto.Result;
import com.cityflow.exception.BizException;
import com.cityflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * UserServiceImpl 单元测试
 *
 * 覆盖范围：
 *   - sendCode：手机号校验、验证码写入 Redis
 *   - login：手机号校验、验证码校验失败的两种分支
 *
 * 不覆盖：登录成功路径（涉及 RequestContextHolder 静态调用，移到集成测试里更省事）
 *
 * Phase 3 改动：失败分支从"返回 fail Result"改为"抛 BizException"，
 * 测试相应改用 assertThatThrownBy + 检查 errorCode。
 */
@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private JwtProps jwtProps;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserServiceImpl userService;

    @BeforeEach
    void setUp() {
        // 大多数 redis 调用走 opsForValue()，统一在这里 stub 掉
        // lenient 让没用到这个 stub 的测试不会因为 "unnecessary stubbing" 报错
        lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    @DisplayName("sendCode：手机号格式错误时抛 BizException(INVALID_PHONE)，不写 Redis")
    void sendCode_invalidPhone_throwsBizException() {
        assertThatThrownBy(() -> userService.sendCode("not-a-phone", null))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PHONE);

        // 关键：验证 redis 没被写入
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any());
    }

    @Test
    @DisplayName("sendCode：手机号合法时把验证码写进 Redis 并返回 ok")
    void sendCode_validPhone_writesCodeToRedis() {
        Result result = userService.sendCode("13900001111", null);

        assertThat(result.getSuccess()).isTrue();
        // 验证 set 被调用一次，key 是 login:code: 前缀 + 手机号
        verify(valueOperations, times(1))
                .set(eq("login:code:13900001111"), anyString(), anyLong(), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("login：手机号格式错误时抛 BizException(INVALID_PHONE)，不查 Redis")
    void login_invalidPhone_throwsBizException() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("bad-phone");
        form.setCode("123456");

        assertThatThrownBy(() -> userService.login(form, null))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_PHONE);

        verify(valueOperations, never()).get(anyString());
    }

    @Test
    @DisplayName("login：Redis 里没存验证码时抛 BizException(INVALID_CODE)")
    void login_codeNotInRedis_throwsBizException() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("13900001111");
        form.setCode("123456");
        when(valueOperations.get("login:code:13900001111")).thenReturn(null);

        assertThatThrownBy(() -> userService.login(form, null))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CODE);

        // 验证没去查数据库
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("login：用户传的验证码与 Redis 里的不匹配时抛 BizException(INVALID_CODE)")
    void login_codeMismatch_throwsBizException() {
        LoginFormDTO form = new LoginFormDTO();
        form.setPhone("13900001111");
        form.setCode("999999");
        when(valueOperations.get("login:code:13900001111")).thenReturn("123456");

        assertThatThrownBy(() -> userService.login(form, null))
                .isInstanceOf(BizException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.INVALID_CODE);

        verifyNoInteractions(userRepository);
    }
}
