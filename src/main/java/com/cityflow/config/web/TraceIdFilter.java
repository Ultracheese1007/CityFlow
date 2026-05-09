package com.cityflow.config.web;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

/**
 * 给每个 HTTP 请求生成一个 traceId，写进 SLF4J MDC，
 * 这样这个请求线程上的所有日志都会自动带上 [traceId]。
 *
 * 也透传到响应头 X-Trace-Id，前端 / 客户端可以记下来——
 * 用户报"我下单失败了"时把响应里的 traceId 给客服，运维一秒定位日志。
 *
 * 优先级 HIGHEST_PRECEDENCE 确保它跑在所有其他 filter（包括 Security）之前，
 * 这样任何拦截器抛出的异常也能被 traceId 标记。
 */
@Slf4j
@Component
public class TraceIdFilter extends OncePerRequestFilter implements Ordered {

    private static final String MDC_KEY = "traceId";
    private static final String HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest req,
                                    HttpServletResponse resp,
                                    FilterChain chain)
            throws ServletException, IOException {

        // 优先用客户端传的 trace（分布式 tracing 串联）；没有则自己生成
        String traceId = req.getHeader(HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().substring(0, 8);
        }

        MDC.put(MDC_KEY, traceId);
        resp.setHeader(HEADER, traceId);    // 响应头也带上

        try {
            chain.doFilter(req, resp);
        } finally {
            // 关键：thread 跑完一定要清，否则线程池复用时上一个请求的 traceId 会污染
            MDC.remove(MDC_KEY);
        }
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
