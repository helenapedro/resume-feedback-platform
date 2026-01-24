package com.pedro.resumeapi.security;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;

@Configuration
public class ShareRateLimitConfig {

    @Bean(destroyMethod = "shutdown")
    public RedisClient shareRateLimitRedisClient(RedisProperties properties) {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(properties.getHost())
                .withPort(properties.getPort());

        if (properties.getPassword() != null) {
            builder.withPassword(properties.getPassword().toCharArray());
        }

        builder.withDatabase(properties.getDatabase());

        if (properties.getSsl().isEnabled()) {
            builder.withSsl(true);
        }

        return RedisClient.create(builder.build());
    }

    @Bean(destroyMethod = "close")
    public StatefulRedisConnection<String, byte[]> shareRateLimitRedisConnection(RedisClient redisClient) {
        RedisCodec<String, byte[]> codec = new RedisCodec<>() {
            @Override
            public String decodeKey(ByteBuffer bytes) {
                return StringCodec.UTF8.decodeKey(bytes);
            }

            @Override
            public byte[] decodeValue(ByteBuffer bytes) {
                return ByteArrayCodec.INSTANCE.decodeValue(bytes);
            }

            @Override
            public ByteBuffer encodeKey(String key) {
                return StringCodec.UTF8.encodeKey(key);
            }

            @Override
            public ByteBuffer encodeValue(byte[] value) {
                return ByteArrayCodec.INSTANCE.encodeValue(value);
            }
        };

        return redisClient.connect(codec);
    }

    @Bean
    public ProxyManager<String> shareRateLimitProxyManager(StatefulRedisConnection<String, byte[]> connection) {
        return LettuceBasedProxyManager.builderFor(connection).build();
    }
}
