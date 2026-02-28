package com.pedro.resumeapi.security;

import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.api.StatefulRedisConnection;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.ByteBuffer;

@Configuration
@ConditionalOnProperty(prefix = "app.security.rate-limit.share", name = "enabled", havingValue = "true", matchIfMissing = true)
public class ShareRateLimitConfig {

    @Value("${REDIS_URL}")
    private String host;

    @Value("${SPRING_DATA_REDIS_PORT}")
    private int port;

    @Value("${SPRING_DATA_REDIS_PASSWORD}")
    private String password;

    @Value("${spring.data.redis.database:0}")
    private int database;

    @Value("${spring.data.redis.ssl.enabled:true}")
    private boolean ssl;

    @Bean(destroyMethod = "shutdown")
    public RedisClient shareRateLimitRedisClient() {
        RedisURI.Builder builder = RedisURI.builder()
                .withHost(host)
                .withPort(port)
                .withDatabase(database);

        if (password != null && !password.isBlank()) {
            builder.withPassword(password.toCharArray());
        }

        if (ssl) {
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
