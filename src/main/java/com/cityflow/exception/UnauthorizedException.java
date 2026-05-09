package com.cityflow.exception;

import com.cityflow.dto.ErrorCode;

/**
 * 未授权专用异常——区别于 Spring Security 的 AuthenticationException，
 * 这个是业务层主动判断（比如已登录但权限不足）。
 */
public class UnauthorizedException extends BizException {

    public UnauthorizedException() {
        super(ErrorCode.UNAUTHORIZED);
    }

    public UnauthorizedException(String customMessage) {
        super(ErrorCode.UNAUTHORIZED, customMessage);
    }
}
