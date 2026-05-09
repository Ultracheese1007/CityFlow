package com.cityflow;

import org.junit.jupiter.api.Test;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 全栈烟雾测试——只验证 Spring 上下文能起来。
 *
 * 外部依赖处理：
 *   - properties 关掉 Redis + Kafka 自动配置（防止 Spring 试图连真服务）
 *   - @MockBean 替换业务代码注入的 StringRedisTemplate / RedissonClient / KafkaTemplate
 */
@SpringBootTest(properties = {
    "spring.autoconfigure.exclude=" +
        "org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisReactiveAutoConfiguration," +
        "org.springframework.boot.autoconfigure.data.redis.RedisRepositoriesAutoConfiguration," +
        "org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration"
})
@ActiveProfiles("test")
class CityFlowApplicationTests {

    @MockBean
    private StringRedisTemplate stringRedisTemplate;

    @MockBean
    private RedissonClient redissonClient;

    @MockBean
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Test
    void contextLoads() {
        // 进到这里说明 Spring 上下文启动成功
    }
}
