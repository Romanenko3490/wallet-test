package ru.practicum.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import ru.practicum.redis.WalletCacheDto;

@Configuration
public class RedisConfig {

    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory factory) {
        RedisTemplate<String, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(factory);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        return redisTemplate;
    }

    @Bean
    public ReactiveRedisTemplate<String, WalletCacheDto> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory redisConnectionFactory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();
        Jackson2JsonRedisSerializer<WalletCacheDto> valueSerializer =
                new Jackson2JsonRedisSerializer<>(WalletCacheDto.class);

        RedisSerializationContext.RedisSerializationContextBuilder<String, WalletCacheDto> builder =
                RedisSerializationContext.newSerializationContext(keySerializer);

        RedisSerializationContext<String, WalletCacheDto> context =
                builder.value(valueSerializer).build();

        return new ReactiveRedisTemplate<>(redisConnectionFactory, context);
    }
}
