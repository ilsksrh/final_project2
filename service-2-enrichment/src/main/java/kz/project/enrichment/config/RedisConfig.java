package kz.project.enrichment.config;

import kz.project.enrichment.dto.EnrichmentResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
public class RedisConfig {

    @Bean
    public ReactiveRedisTemplate<String, EnrichmentResponse> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory factory) {

        StringRedisSerializer keySerializer = new StringRedisSerializer();

        JacksonJsonRedisSerializer<EnrichmentResponse> valueSerializer =
                new JacksonJsonRedisSerializer<>(EnrichmentResponse.class);

        RedisSerializationContext<String, EnrichmentResponse> serializationContext =
                RedisSerializationContext.<String, EnrichmentResponse>newSerializationContext(keySerializer)
                        .value(valueSerializer)
                        .hashKey(keySerializer)
                        .hashValue(valueSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(factory, serializationContext);
    }
}