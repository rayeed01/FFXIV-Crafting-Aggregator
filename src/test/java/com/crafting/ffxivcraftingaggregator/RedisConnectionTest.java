package com.crafting.ffxivcraftingaggregator;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@SpringBootTest
class RedisConnectionTest {

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void canWriteAndReadBack() {
        redisTemplate.opsForValue().set("smoke:test", "hello", Duration.ofSeconds(30));

        assertThat(redisTemplate.opsForValue().get("smoke:test")).isEqualTo("hello");
    }
}
