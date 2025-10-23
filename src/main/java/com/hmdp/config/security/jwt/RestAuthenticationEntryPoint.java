// File: src/main/java/com/hmdp/config/security/jwt/RestAuthenticationEntryPoint.java
package com.hmdp.config.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hmdp.dto.Result;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/** 未登录统一返回 —— 保持与项目 Result 结构一致 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final ObjectMapper om = new ObjectMapper();

    @Override
    public void commence(HttpServletRequest req, HttpServletResponse resp, AuthenticationException ex) throws IOException {
        resp.setStatus(HttpServletResponse.SC_OK); // 与原风格一致：200 + 业务码
        resp.setContentType(MediaType.APPLICATION_JSON_VALUE);
        resp.setCharacterEncoding("UTF-8");
        resp.getWriter().write(om.writeValueAsString(Result.fail("未登录")));
    }
}
