package kz.project.enrichment.controller;

import kz.project.enrichment.dto.EnrichmentResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class EnrichmentController {

    private final ReactiveRedisTemplate<String, EnrichmentResponse> redisTemplate;


    private static final String KEY_PREFIX = "extended:client:";

    @GetMapping("/extended-info/{clientId}")
    public Mono<EnrichmentResponse> getExtendedInfo(@PathVariable Long clientId) {
        String key = KEY_PREFIX + clientId;

        return redisTemplate.opsForValue()
                .get(key)
                .doOnNext(resp ->
                        log.debug("Cache hit for clientId={}: {}", clientId, resp)
                )
                .switchIfEmpty(Mono.defer(() -> {
                    log.debug("Cache miss for clientId={}, returning default", clientId);
                    return Mono.just(new EnrichmentResponse());
                }))
                .onErrorResume(e -> {
                    log.error("Redis error for clientId={}: {}", clientId, e.getMessage());
                    return Mono.just(new EnrichmentResponse());
                });
    }

}