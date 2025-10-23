// File: src/main/java/com/hmdp/config/security/jwt/JwtAuthenticationFilter.java
package com.hmdp.config.security.jwt;

import com.hmdp.dto.UserDTO;
import com.hmdp.utils.UserHolder;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * 极简认证 + 被动刷新：
 * 1) 先用请求头 authorization 的 Access 验签。
 * 2) 如果 Access 缺失/过期，再从 HttpOnly 刷新 Cookie 验签：
 *    - 刷新 Cookie 可用：本次请求直接通过，并下发一个新的 Access（放响应头 X-ACCESS-TOKEN，前端不读也没关系）。
 *    - 刷新 Cookie 不可用：不设置认证，交给 EntryPoint 返回“未登录”。
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtProps props;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse resp, FilterChain chain)
            throws ServletException, IOException {
        boolean authenticated = false;
        String access = req.getHeader("authorization"); // 前端不改：无 Bearer 前缀

        try {
            if (StringUtils.hasText(access)) {
                // 先尝试用 Access
                Jws<Claims> jws = JwtUtil.parse(props.getSecret(), access);
                accept(jws, access);
                authenticated = true;
            }
        } catch (ExpiredJwtException eje) {
            // Access 过期，尝试 Refresh
            String refresh = readCookie(req, props.getRefreshCookieName());
            if (StringUtils.hasText(refresh)) {
                try {
                    Jws<Claims> rjws = JwtUtil.parse(props.getSecret(), refresh);
                    String userId = rjws.getBody().getSubject();
                    // 颁发一个新的 Access，并本次请求放行
                    String newAccess = JwtUtil.genAccess(props.getSecret(), props.getAccessTtlMinutes(), userId, null);
                    resp.setHeader("X-ACCESS-TOKEN", newAccess); // 可选：提供给你调试查看
                    // 将新 Access 作为“凭证”注入上下文（不强制前端更新）
                    accept(JwtUtil.parse(props.getSecret(), newAccess), newAccess);
                    authenticated = true;
                } catch (Exception ignore) {
                    // refresh 无效 -> 走未登录
                }
            }
        } catch (Exception ignore) {
            // 其他解析异常，继续走未登录
        }

        try {
            chain.doFilter(req, resp);
        } finally {
            if (authenticated) {
                UserHolder.removeUser();
                SecurityContextHolder.clearContext();
            }
        }
    }

    private void accept(Jws<Claims> jws, String credentialToken) {
        Claims c = jws.getBody();
        String userId = c.getSubject();
        // 构造认证
        AbstractAuthenticationToken auth =
                new AbstractAuthenticationToken(AuthorityUtils.NO_AUTHORITIES) {
                    @Override public Object getCredentials() { return credentialToken; }
                    @Override public Object getPrincipal() { return userId; }
                };
        auth.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(auth);
        // 兼容旧业务：填充 UserHolder（nickName 非必需，可忽略）
        UserDTO dto = new UserDTO();
        dto.setId(Long.valueOf(userId));
        UserHolder.saveUser(dto);
    }

    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) if (name.equals(c.getName())) return c.getValue();
        return null;
    }
}