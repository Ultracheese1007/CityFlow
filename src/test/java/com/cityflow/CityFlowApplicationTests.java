package com.cityflow;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 全栈烟雾测试：用 H2 加载完整 Spring 上下文，证明应用能成功启动。
 *
 * Redis 在测试里的处理：
 *   - 用 properties 关掉 Spring Boot 三套 Redis 自动配置（默认连真 Redis 会失败）
 *   - 用 @MockBean 给 prod 代码注入的 StringRedisTemplate / RedissonClient 提供假实例
 *
 * 真 Repository SQL 行为由 SeckillRepositoryIntegrationTest 验证。
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration"
})
@ActiveProfiles("test")
class CityFlowApplicationTests {

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @Test
    void contextLoads() {
        // 不需要断言：进到这里说明 ApplicationContext 启动成功
    }
}
